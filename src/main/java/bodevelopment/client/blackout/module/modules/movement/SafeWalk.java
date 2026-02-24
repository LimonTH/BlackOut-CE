package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

public class SafeWalk extends Module {
    private static SafeWalk INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Boolean> sneak = this.sgGeneral.booleanSetting("Sneak", false, ".");

    public SafeWalk() {
        super("Safe Walk", "Doesn't let you die (i would).", SubCategory.MOVEMENT, true);
        INSTANCE = this;
    }

    public static boolean shouldSafeWalk() {
        return INSTANCE.active();
    }

    @Override
    public boolean shouldSkipListeners() {
        return !this.active();
    }

    @Event
    public void onTick(TickEvent.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null && this.sneak.get()) {
            Vec3d movement = BlackOut.mc.player.getVelocity();
            Vec3d newMovement = BlackOut.mc.player.adjustMovementForSneaking(movement, MovementType.SELF);
            if (!movement.equals(newMovement)) {
                BlackOut.mc.player.setSneaking(true);
                BlackOut.mc.options.sneakKey.setPressed(true);
            } else {
                BlackOut.mc.player.setSneaking(false);
                BlackOut.mc.options.sneakKey.setPressed(false);
            }
        }
    }

    private boolean active() {
        if (this.enabled) {
            return true;
        } else {
            Scaffold scaffold = Scaffold.getInstance();
            return scaffold.enabled && scaffold.safeWalk.get();
        }
    }
}
