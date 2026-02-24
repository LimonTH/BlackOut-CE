package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.interfaces.mixin.IMinecraftClient;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

public class FastUse extends Module {
    private static FastUse INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Timing> timing = this.sgGeneral.enumSetting("Update Logic", Timing.Tick, "The event hook used to trigger item usage; Render allows for sub-tick speeds.");
    public final Setting<Integer> delayTicks = this.sgGeneral.intSetting("Tick Delay", 1, 0, 4, 1, "The number of game ticks to wait between uses.", () -> this.timing.get() == Timing.Tick);
    public final Setting<Double> delaySeconds = this.sgGeneral.doubleSetting("Time Delay", 0.2, 0.0, 1.0, 0.002, "The cooldown in seconds between uses when using Render timing.", () -> this.timing.get() == Timing.Render);
    private final Setting<List<Item>> items = this.sgGeneral.itemListSetting("Affected Items", "The specific items that will be used at an accelerated rate.", Items.EXPERIENCE_BOTTLE);
    public final Setting<RotationMode> rotate = this.sgGeneral.enumSetting("EXP Rotation", RotationMode.Normal, "Automatically looks downward when throwing experience bottles to maximize efficiency.", () -> this.items.get().contains(Items.EXPERIENCE_BOTTLE));

    private long prevUse = 0L;

    public FastUse() {
        super("Fast Use", "Reduces or removes the delay between using consumable or throwable items.", SubCategory.MISC, true);
        INSTANCE = this;
    }

    public static FastUse getInstance() {
        return INSTANCE;
    }

    @Override
    public String getInfo() {
        return this.timing.get().name();
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (this.timing.get() == Timing.Tick) {
            ItemStack stack = this.getStack();
            if (this.isValid(stack)) {
                this.rotateIfNeeded(this.getStack());
            }
        }
    }

    @Event
    public void onRender(RenderEvent.World.Pre event) {
        if (this.timing.get() == Timing.Render) {
            ItemStack stack = this.getStack();
            if (this.isValid(stack)) {
                if (!this.rotateIfNeeded(this.getStack())) {
                    if (!(System.currentTimeMillis() - this.prevUse < this.delaySeconds.get() * 1000.0)) {
                        this.prevUse = System.currentTimeMillis();
                        ((IMinecraftClient) BlackOut.mc).blackout_Client$useItem();
                    }
                }
            }
        }
    }

    public boolean rotateIfNeeded(ItemStack stack) {
        if (stack.isOf(Items.EXPERIENCE_BOTTLE)) {
            switch (this.rotate.get()) {
                case Vanilla:
                    BlackOut.mc.player.setPitch(90.0F);
                    return Math.abs(Managers.ROTATION.prevPitch - 90.0F) > 1.0F;
                case Normal:
                    return !this.rotatePitch(90.0F, RotationType.Other, "exp");
                case Instant:
                    return !this.rotatePitch(90.0F, RotationType.InstantOther, "exp");
            }
        }

        return false;
    }

    public boolean isValid(ItemStack stack) {
        return stack != null && this.items.get().contains(stack.getItem());
    }

    public ItemStack getStack() {
        if (BlackOut.mc.player != null
                && BlackOut.mc.world != null
                && !BlackOut.mc.player.isUsingItem()
                && BlackOut.mc.options.useKey.isPressed()) {
            ItemStack stack = BlackOut.mc.player.getMainHandStack();
            return stack != null && !stack.isEmpty() ? stack : null;
        } else {
            return null;
        }
    }

    public enum RotationMode {
        Vanilla,
        Normal,
        Instant,
        Disabled
    }

    public enum Timing {
        Tick,
        Render
    }
}
