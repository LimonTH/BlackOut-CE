package bodevelopment.client.blackout.module.modules.combat.defensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.offensive.AutoCrystal;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.util.DamageUtils;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ThrowablePotionItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

public class AutoPot extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgHealth = this.addGroup("Health");
    private final SettingGroup sgPause = this.addGroup("Pause");
    private final SettingGroup sgRender = this.addGroup("Render");

    private final Setting<Double> throwSpeed = this.sgGeneral.doubleSetting("Throw Speed", 20.0, 0.0, 20.0, 0.2, "How many timer to throw every second. 20 is recommended.");
    private final Setting<Integer> bottles = this.sgGeneral.intSetting("Bottles", 1, 1, 10, 1, "Amount of bottles to throw every time.");
    private final Setting<SwitchMode> switchMode = this.sgGeneral.enumSetting("Switch Mode", SwitchMode.Silent, "Method of switching. Silent is the most reliable.");
    private final Setting<Integer> throwTicks = this.sgGeneral
            .intSetting("Throw Ticks", 1, 1, 20, 1, "Doesn't use experience if any armor piece is above this durability.");
    private final Setting<Boolean> instantRotate = this.sgGeneral.booleanSetting("Instant Rotate", true, "Ignores rotation speed limit.");
    private final Setting<Integer> health = this.sgHealth.intSetting("Health", 75, 0, 100, 1, "Mends if any armor piece is under this durability.");
    private final Setting<Boolean> safe = this.sgHealth.booleanSetting("Safe", true, "Doesn't use experience if any armor piece is above this durability.");
    private final Setting<Double> safeHealth = this.sgHealth.doubleSetting("Safe Health", 5.0, 0.0, 36.0, 0.1, ".");
    private final Setting<Integer> maxExisted = this.sgHealth.intSetting("Max Existed", 10, 0, 100, 1, ".");
    private final Setting<Integer> autoCrystalPause = this.sgPause.intSetting("Auto Crystal Pause", 0, 0, 100, 1, "Pauses for x ticks if auto crystal places.");
    private final Setting<Integer> surroundPause = this.sgPause.intSetting("Surround Pause", 0, 0, 100, 1, "Pauses for x ticks if surround places.");
    private final Setting<Integer> movePause = this.sgPause.intSetting("Move Pause", 0, 0, 100, 1, "Pauses for x ticks if moved.");
    private final Setting<Integer> airPause = this.sgPause.intSetting("Air Pause", 0, 0, 100, 1, "Pauses for x ticks if off ground.");
    private final Setting<Boolean> renderSwing = this.sgRender.booleanSetting("Render Swing", true, "Renders swing animation when throwing an exp bottle.");
    private final Setting<SwingHand> swingHand = this.sgRender.enumSetting("Swing Hand", SwingHand.RealHand, "Which hand should be swung.");
    private final Predicate<ItemStack> healthPred = stack -> {
        if (!(stack.getItem() instanceof ThrowablePotionItem)) {
            return false;
        }
        PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
        if (contents != null) {
            for (StatusEffectInstance instance : contents.getEffects()) {
                if (instance.getEffectType().equals(StatusEffects.INSTANT_HEALTH)) {
                    return true;
                }
            }
        }

        return false;
    };
    private double throwsLeft = 0.0;
    private BlockPos lastPos = null;
    private int acTimer = 0;
    private int surroundTimer = 0;
    private int moveTimer = 0;
    private int offGroundTimer = 0;
    private int throwTimer = 0;
    private boolean switched = false;
    private FindResult result = null;

    public AutoPot() {
        super("Auto Pot", "Automatically throws potions.", SubCategory.DEFENSIVE, true);
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (AutoCrystal.getInstance().placing) {
                this.acTimer = this.autoCrystalPause.get();
            }

            if (Surround.getInstance().placing) {
                this.surroundTimer = this.surroundPause.get();
            }

            if (!BlackOut.mc.player.getBlockPos().equals(this.lastPos)) {
                this.lastPos = BlackOut.mc.player.getBlockPos();
                this.moveTimer = this.movePause.get();
            }

            if (!BlackOut.mc.player.isOnGround()) {
                this.offGroundTimer = this.airPause.get();
            }
        }
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.throwsLeft = this.throwsLeft + this.throwSpeed.get() / 20.0;
            this.updateTimers();
            this.update();
            this.throwsLeft = Math.min(1.0, this.throwsLeft);
        }
    }

    private void update() {
        Hand hand = OLEPOSSUtils.getHand(this.healthPred);
        this.result = null;
        int bottlesLeft = 0;
        if (hand == null) {
            if ((this.result = this.switchMode.get().find(this.healthPred)).wasFound()) {
                bottlesLeft = Math.min((int) Math.floor(this.throwsLeft), this.result.amount());
            }
        } else {
            int b = hand == Hand.MAIN_HAND ? Managers.PACKET.getStack().getCount() : BlackOut.mc.player.getOffHandStack().getCount();
            bottlesLeft = Math.min((int) Math.floor(this.throwsLeft), b);
        }

        if (this.shouldThrow() && (bottlesLeft = Math.min(bottlesLeft, this.bottles.get())) > 0) {
            this.throwTimer = this.throwTicks.get();
        }

        if (this.throwTimer <= 0) {
            this.end("throwing");
        } else {
            this.switched = false;
            if (this.rotatePitch(90.0F, RotationType.Other.withInstant(this.instantRotate.get()), "throwing")) {
                while (bottlesLeft > 0) {
                    this.throwBottle(hand);
                    bottlesLeft--;
                    this.throwsLeft--;
                }

                if (this.switched) {
                    this.switchMode.get().swapBack();
                }
            }
        }
    }

    private boolean shouldThrow() {
        return this.shouldHeal() && this.acTimer <= 0 && this.surroundTimer <= 0 && this.moveTimer <= 0 && this.offGroundTimer <= 0;
    }

    private void updateTimers() {
        this.acTimer--;
        this.surroundTimer--;
        this.moveTimer--;
        this.offGroundTimer--;
        this.throwTimer--;
    }

    private boolean shouldHeal() {
        return BlackOut.mc.player.getHealth() <= this.health.get() || this.safe.get() && this.inDanger();
    }

    private boolean inDanger() {
        for (Entity entity : BlackOut.mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity && entity.age <= this.maxExisted.get()) {
                double damage = DamageUtils.crystalDamage(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox(), entity.getPos());
                if (BlackOut.mc.player.getHealth() - damage <= this.safeHealth.get()) {
                    return true;
                }
            }
        }

        return false;
    }

    private void throwBottle(Hand hand) {
        if (hand != null || (this.switched = this.switchMode.get().swap(this.result.slot()))) {
            this.useItem(hand);
            if (this.renderSwing.get()) {
                this.clientSwing(this.swingHand.get(), hand);
            }
        }
    }
}
