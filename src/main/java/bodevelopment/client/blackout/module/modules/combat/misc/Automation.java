package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.defensive.Surround;
import bodevelopment.client.blackout.module.modules.movement.Blink;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.HoleUtils;
import net.minecraft.util.math.BlockPos;

public class Automation extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Boolean> holeSurround = this.sgGeneral.booleanSetting("Hole Surround", true,
            "Automatically activates the Surround module the moment you step into a safe hole.");
    private final Setting<Boolean> leaveHoleBlink = this.sgGeneral.booleanSetting("Leave Hole Blink", true,
            "Automatically enables Blink when leaving a hole to desync your position and confuse opponents.");
    private final Setting<Boolean> enterHoleBlink = this.sgGeneral.booleanSetting("Enter Hole Blink", true,
            "Automatically disables Blink when you enter a new hole to prevent being teleported back outside.");
    private final Setting<Boolean> safeHoleBlink = this.sgGeneral.booleanSetting("Safe Hole Blink", true,
            "Forces Blink to disable if the hole you were previously in becomes compromised (e.g., walls are broken).");

    private BlockPos currentPos = null;
    private BlockPos blinkPos = null;

    public Automation() {
        super("Automation", "Manages combat and movement modules based on your current hole status.", SubCategory.MISC_COMBAT, true);
    }

    @Event
    public void onMove(MoveEvent.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            BlockPos prev = this.currentPos;
            this.currentPos = new BlockPos(
                    BlackOut.mc.player.getBlockX(), (int) Math.round(BlackOut.mc.player.getY()), BlackOut.mc.player.getBlockZ()
            );
            BlockPos prevPos = prev == null ? this.currentPos : prev;
            Blink blink = Blink.getInstance();
            Surround surround = Surround.getInstance();
            if (this.safeHoleBlink.get() && this.blinkPos != null && !HoleUtils.inHole(this.blinkPos)) {
                blink.disable(blink.getDisplayName() + " was disabled by " + this.getDisplayName());
            }

            if (!this.currentPos.equals(prevPos)) {
                if (HoleUtils.inHole(this.currentPos) && !HoleUtils.inHole(prevPos)) {
                    if (this.holeSurround.get()) {
                        surround.enable(surround.getDisplayName() + " was enabled by " + this.getDisplayName());
                    }

                    if (this.enterHoleBlink.get()) {
                        blink.disable(blink.getDisplayName() + " was disabled " + this.getDisplayName());
                    }
                }

                if (this.leaveHoleBlink.get() && !HoleUtils.inHole(this.currentPos) && HoleUtils.inHole(prevPos)) {
                    this.blinkPos = prevPos;
                    blink.enable(blink.getDisplayName() + " was enabled by " + this.getDisplayName());
                }
            }
        }
    }
}
