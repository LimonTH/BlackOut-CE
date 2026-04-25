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

    private final Setting<Double> fallThreshold = sgGeneral.doubleSetting("Fall Threshold", 3.0, 0.5, 20.0, 0.5, "Minimum fall distance in blocks before switching to the mace.");
    private final Setting<Boolean> restoreSlot   = sgGeneral.booleanSetting("Restore Slot", true, "Switch back to the previous hotbar slot after landing.");

    private int previousSlot = -1;
    private boolean switched = false;

    public AutoMace() {
        super("AutoMace", "Swaps to a mace when falling.", SubCategory.LEGIT, true);
    }

    @Override
    public void onEnable() {
        previousSlot = -1;
        switched = false;
    }

    @Override
    public void onDisable() {
        if (switched && restoreSlot.get() && previousSlot != -1) {
            BlackOut.mc.player.getInventory().selected = previousSlot;
        }
        previousSlot = -1;
        switched = false;
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player == null || BlackOut.mc.level == null) return;

        boolean isFalling = !BlackOut.mc.player.onGround()
                && BlackOut.mc.player.getDeltaMovement().y < 0
                && BlackOut.mc.player.fallDistance >= fallThreshold.get().floatValue();

        if (isFalling && !switched) {
            int maceSlot = findMaceInHotbar();
            if (maceSlot != -1 && BlackOut.mc.player.getInventory().selected != maceSlot) {
                previousSlot = BlackOut.mc.player.getInventory().selected;
                BlackOut.mc.player.getInventory().selected = maceSlot;
                switched = true;
            }
        } else if (!isFalling && switched) {
            if (restoreSlot.get() && previousSlot != -1) {
                BlackOut.mc.player.getInventory().selected = previousSlot;
            }
            previousSlot = -1;
            switched = false;
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
}