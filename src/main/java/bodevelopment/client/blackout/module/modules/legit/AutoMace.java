package bodevelopment.client.blackout.module.modules.legit;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

public class AutoMace extends Module {

    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Double> fallThreshold = sgGeneral.doubleSetting("Fall Threshold", 3.0, 0.5, 20.0, 0.5,
            "Minimum fall distance in blocks before switching to the mace.");
    private final Setting<Boolean> restoreSlot = sgGeneral.booleanSetting("Restore Slot", true,
            "Switch back to the previous hotbar slot after landing.");

    private int previousSlot = -1;
    private boolean switched = false;
    private double trackedFallDistance = 0.0;
    private double lastY = Double.MAX_VALUE;

    public AutoMace() {
        super("AutoMace", "Switches to Mace when falling.", SubCategory.LEGIT, true);
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        if (switched && restoreSlot.get() && previousSlot != -1
                && BlackOut.mc.player != null) {
            BlackOut.mc.player.getInventory().selected = previousSlot;
        }
        reset();
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player == null || BlackOut.mc.level == null) return;

        double currentY = BlackOut.mc.player.getY();

        if (lastY == Double.MAX_VALUE) {
            lastY = currentY;
            return;
        }

        double dy = currentY - lastY;
        lastY = currentY;

        if (BlackOut.mc.player.onGround()) {
            if (switched && restoreSlot.get() && previousSlot != -1) {
                BlackOut.mc.player.getInventory().selected = previousSlot;
            }
            previousSlot = -1;
            switched = false;
            trackedFallDistance = 0.0;
            return;
        }

        if (dy < 0) {
            trackedFallDistance += Math.abs(dy);
        } else {
            trackedFallDistance = 0.0;
        }

        if (trackedFallDistance >= fallThreshold.get() && !switched) {
            int maceSlot = findMaceInHotbar();
            if (maceSlot != -1 && BlackOut.mc.player.getInventory().selected != maceSlot) {
                previousSlot = BlackOut.mc.player.getInventory().selected;
                BlackOut.mc.player.getInventory().selected = maceSlot;
                switched = true;
            }
        }
    }

    private int findMaceInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = BlackOut.mc.player.getInventory().getItem(i);
            if (stack.getItem() == Items.MACE) {
                return i;
            }
        }
        return -1;
    }

    private void reset() {
        previousSlot = -1;
        switched = false;
        trackedFallDistance = 0.0;
        lastY = Double.MAX_VALUE;
    }
}