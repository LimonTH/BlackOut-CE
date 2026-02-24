package bodevelopment.client.blackout.module.modules.combat.defensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class ExpThrower extends Module {
    private static ExpThrower INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgRender = this.addGroup("Render");

    private final Setting<Double> throwSpeed = this.sgGeneral.doubleSetting("Throw Speed", 20.0, 0.0, 20.0, 0.2,
            "Maximum throwing frequency. Ignores all combat and movement pauses for instant repairs.");
    private final Setting<Integer> bottles = this.sgGeneral.intSetting("Bottles", 1, 1, 10, 1,
            "Number of bottles to throw in a single tick.");
    private final Setting<SwitchMode> switchMode = this.sgGeneral.enumSetting("Switch Mode", SwitchMode.Silent,
            "The method for switching to Experience Bottles. Silent is best for keeping your current item held.");
    private final Setting<Integer> antiWaste = this.sgGeneral.intSetting("Anti Waste", 90, 0, 100, 1,
            "Stops throwing experience if all armor pieces are above this durability percentage.");
    private final Setting<Integer> forceMend = this.sgGeneral.intSetting("Force Mend", 30, 0, 100, 1,
            "Continues throwing even if Anti-Waste is triggered, as long as any piece is below this critical threshold.");
    private final Setting<Boolean> rotate = this.sgGeneral.booleanSetting("Rotate", true,
            "Automatically looks down at your feet to ensure the experience orbs are collected immediately.");
    private final Setting<Boolean> instantRotate = this.sgGeneral.booleanSetting("Instant Rotate", true,
            "Forces the rotation to happen instantly, ignoring rotation speed smoothing.", this.rotate::get);

    private final Setting<Boolean> renderSwing = this.sgRender.booleanSetting("Render Swing", true,
            "Displays the hand swing animation on your client.");
    private final Setting<SwingHand> swingHand = this.sgRender.enumSetting("Swing Hand", SwingHand.RealHand,
            "Which hand should perform the swing animation.");

    private double throwsLeft = 0.0;

    public ExpThrower() {
        super("Exp Thrower", "High-priority experience thrower. Unlike AutoMend, it ignores combat pauses and movement to repair gear as fast as possible.", SubCategory.DEFENSIVE, true);
        INSTANCE = this;
    }

    public static ExpThrower getInstance() {
        return INSTANCE;
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.throwsLeft = this.throwsLeft + this.throwSpeed.get() / 20.0;
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
        if (this.shouldMend() && bottlesLeft >= 1) {
            if (!this.rotate.get() || this.rotate(Managers.ROTATION.nextYaw, 90.0F, RotationType.Other.withInstant(this.instantRotate.get()), "throwing")) {
                if (hand != null || (switched = this.switchMode.get().swap(result.slot()))) {
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
            this.end("throwing");
        }
    }

    private boolean shouldMend() {
        float max = -1.0F;
        float lowest = 500.0F;
        boolean found = false;

        for (ItemStack stack : BlackOut.mc.player.getArmorItems()) {
            if (!stack.isEmpty() && stack.isDamageable()) {
                found = true;
                float dur = (float) (stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage() * 100.0F;
                if (dur > max) {
                    max = dur;
                }

                if (dur < lowest) {
                    lowest = dur;
                }
            }
        }

        if (!found) {
            return false;
        } else {
            return lowest <= this.forceMend.get() || max < this.antiWaste.get();
        }
    }

    private void throwBottle(Hand hand) {
        this.useItem(hand);
        if (this.renderSwing.get()) {
            this.clientSwing(this.swingHand.get(), hand);
        }
    }
}
