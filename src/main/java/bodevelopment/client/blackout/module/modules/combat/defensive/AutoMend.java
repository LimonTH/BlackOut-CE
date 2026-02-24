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
import bodevelopment.client.blackout.module.modules.combat.misc.Suicide;
import bodevelopment.client.blackout.module.modules.combat.offensive.AutoCrystal;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class AutoMend extends Module {

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgPause = this.addGroup("Pause");
    private final SettingGroup sgRender = this.addGroup("Render");

    private final Setting<Boolean> antiCharity = this.sgGeneral.booleanSetting("Anti Charity", true,
            "Avoids throwing experience if an enemy is standing in the same block, to prevent mending their gear.");
    private final Setting<Double> throwSpeed = this.sgGeneral.doubleSetting("Throw Speed", 20.0, 0.0, 20.0, 0.2,
            "Throwing frequency. Respects pauses and Anti-Charity checks to ensure efficiency in combat.");
    private final Setting<Integer> bottles = this.sgGeneral.intSetting("Bottles", 1, 1, 10, 1,
            "Amount of bottles to throw in a single action.");
    private final Setting<SwitchMode> switchMode = this.sgGeneral.enumSetting("Switch Mode", SwitchMode.Silent,
            "The method used to switch to experience bottles in your hotbar.");
    private final Setting<Integer> minDur = this.sgGeneral.intSetting("Min Durability", 75, 0, 100, 1,
            "The threshold (%) to start mending armor pieces.");
    private final Setting<Integer> antiWaste = this.sgGeneral.intSetting("Anti Waste", 90, 0, 100, 1,
            "Stops throwing experience if any armor piece reaches this durability percentage.");
    private final Setting<Integer> forceMend = this.sgGeneral.intSetting("Force Mend", 30, 0, 100, 1,
            "Overrides Anti Waste and Charity if armor durability drops below this critical level.");
    private final Setting<Boolean> instantRotate = this.sgGeneral.booleanSetting("Instant Rotate", true,
            "Forces the player to look down instantly, ignoring rotation speed limits.");

    private final Setting<Integer> autoCrystalPause = this.sgPause.intSetting("Auto Crystal Pause", 0, 0, 100, 1,
            "Stops mending for a set amount of ticks after AutoCrystal places a crystal.");
    private final Setting<Integer> surroundPause = this.sgPause.intSetting("Surround Pause", 0, 0, 100, 1,
            "Stops mending for a set amount of ticks after Surround places a block.");
    private final Setting<Integer> movePause = this.sgPause.intSetting("Move Pause", 0, 0, 100, 1,
            "Pauses mending for a set amount of ticks after you move to a different block.");
    private final Setting<Integer> airPause = this.sgPause.intSetting("Air Pause", 0, 0, 100, 1,
            "Pauses mending while you are not on the ground.");

    private final Setting<Boolean> renderSwing = this.sgRender.booleanSetting("Render Swing", true,
            "Shows the arm swing animation when throwing bottles.");
    private final Setting<SwingHand> swingHand = this.sgRender.enumSetting("Swing Hand", SwingHand.RealHand,
            "Determines which hand is used for the swing animation.");

    private double throwsLeft = 0.0;
    private BlockPos lastPos = null;
    private boolean throwing = false;
    private int acTimer = 0;
    private int surroundTimer = 0;
    private int selfTrapTimer = 0;
    private int moveTimer = 0;
    private int offGroundTimer = 0;

    public AutoMend() {
        super("Auto Mend", "Smart armor repair for combat. Pauses during actions (like AutoCrystal) and features Anti-Charity to avoid mending enemies.", SubCategory.DEFENSIVE, true);
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
        if (BlackOut.mc.player != null && BlackOut.mc.world != null && !Suicide.getInstance().enabled) {
            this.throwsLeft = this.throwsLeft + this.throwSpeed.get() / 20.0;
            this.updateTimers();
            this.update();
            this.throwsLeft = Math.min(1.0, this.throwsLeft);
        }
    }

    private void update() {
        Hand hand = OLEPOSSUtils.getHand(Items.EXPERIENCE_BOTTLE);
        FindResult result = null;
        boolean switched = false;
        int bottlesLeft = 0;
        if (hand == null) {
            result = this.switchMode.get().find(Items.EXPERIENCE_BOTTLE);
            if (result.wasFound()) {
                bottlesLeft = Math.min((int) Math.floor(this.throwsLeft), result.amount());
            }
        } else {
            int b = hand == Hand.MAIN_HAND ? Managers.PACKET.getStack().getCount() : BlackOut.mc.player.getOffHandStack().getCount();
            bottlesLeft = Math.min((int) Math.floor(this.throwsLeft), b);
        }

        bottlesLeft = Math.min(bottlesLeft, this.bottles.get());
        if (this.shouldThrow() && bottlesLeft >= 1) {
            this.throwing = true;
            if (this.rotate(Managers.ROTATION.nextYaw, 90.0F, RotationType.Other.withInstant(this.instantRotate.get()), "throwing")) {
                if (hand == null) {
                    switched = this.switchMode.get().swap(result.slot());
                }

                if (hand != null || switched) {
                    while (bottlesLeft > 0) {
                        this.throwBottle(hand);
                        bottlesLeft--;
                        this.throwsLeft--;
                    }

                    if (switched) {
                        this.switchMode.get().swapBack();
                    }
                }
            }
        } else {
            this.throwing = false;
            this.end("throwing");
        }
    }

    private boolean shouldThrow() {
        return this.shouldMend() && this.acTimer <= 0 && this.surroundTimer <= 0 && this.selfTrapTimer <= 0 && this.moveTimer <= 0 && this.offGroundTimer <= 0;
    }

    private void updateTimers() {
        this.acTimer--;
        this.surroundTimer--;
        this.selfTrapTimer--;
        this.moveTimer--;
        this.offGroundTimer--;
    }

    private boolean shouldMend() {
        List<ItemStack> armors = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            armors.add(BlackOut.mc.player.getInventory().getArmorStack(i));
        }

        float max = -1.0F;
        float lowest = 500.0F;

        for (ItemStack stack : armors) {
            float dur = (float) (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage() * 100.0F;
            if (dur > max) {
                max = dur;
            }

            if (dur < lowest) {
                lowest = dur;
            }
        }

        if (lowest <= this.forceMend.get()) {
            return true;
        } else if (this.antiCharity.get() && this.playerAtPos()) {
            return false;
        } else {
            return !(max >= this.antiWaste.get()) && (lowest <= this.minDur.get() || this.throwing);
        }
    }

    private boolean playerAtPos() {
        for (AbstractClientPlayerEntity player : BlackOut.mc.world.getPlayers()) {
            if (player != BlackOut.mc.player && !Managers.FRIENDS.isFriend(player) && player.getBlockPos().equals(BlackOut.mc.player.getBlockPos())) {
                return true;
            }
        }

        return false;
    }

    private void throwBottle(Hand hand) {
        this.useItem(hand);
        if (this.renderSwing.get()) {
            this.clientSwing(this.swingHand.get(), hand);
        }
    }
}
