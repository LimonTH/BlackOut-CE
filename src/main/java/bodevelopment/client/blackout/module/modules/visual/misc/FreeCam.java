package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.interfaces.mixin.IVec3d;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.math.Vec3d;

public class FreeCam extends Module {
    private static FreeCam INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Mode> mode = this.sgGeneral.enumSetting("Movement Mode", Mode.Normal, "The interpolation logic used for camera translation.");
    private final Setting<Double> speedH = this.sgGeneral.doubleSetting("Horizontal Velocity", 1.0, 0.1, 10.0, 0.1, "The movement speed multiplier for the lateral X and Z axes.");
    private final Setting<Double> speedV = this.sgGeneral.doubleSetting("Vertical Velocity", 1.0, 0.1, 10.0, 0.1, "The movement speed multiplier for the vertical Y axis.");

    public final Vec3d velocity = new Vec3d(0.0, 0.0, 0.0);
    public Vec3d pos = Vec3d.ZERO;
    private float moveYaw;
    private float vertical;
    private boolean move;

    public FreeCam() {
        super("Freecam", "Detaches the camera from the player entity, allowing independent exploration of the environment while maintaining the player's position.", SubCategory.MISC_VISUAL, true);
        INSTANCE = this;
    }

    public static FreeCam getInstance() {
        return INSTANCE;
    }

    @Event
    public void onRender(TickEvent.Pre event) {
        if (BlackOut.mc.world == null || BlackOut.mc.player == null) {
            this.disable();
        }
    }

    public void resetInput(KeyboardInput input) {
        input.pressingForward = false;
        input.pressingBack = false;
        input.pressingLeft = false;
        input.pressingRight = false;
        input.movementForward = 0.0F;
        input.movementSideways = 0.0F;
        input.jumping = false;
        input.sneaking = false;
    }

    public Vec3d getPos(float yaw, float pitch) {
        this.inputYaw(yaw);
        Vec3d movement;
        double rad;
        double x;
        double y;
        double z;
        switch (this.mode.get()) {
            case Normal:
                rad = Math.toRadians(this.moveYaw + 90.0F);
                x = 0.0;
                y = this.vertical * this.speedV.get();
                z = 0.0;
                if (this.move) {
                    x = Math.cos(rad) * this.speedH.get();
                    z = Math.sin(rad) * this.speedH.get();
                }

                movement = new Vec3d(x, y, z);
                break;
            case Smooth:
                rad = Math.toRadians(this.moveYaw + 90.0F);
                x = 0.0;
                y = this.vertical;
                z = 0.0;
                if (this.move) {
                    x = Math.cos(rad);
                    z = Math.sin(rad);
                }

                x = this.smoothen(this.velocity.x, x);
                y = this.smoothen(this.velocity.y, y);
                z = this.smoothen(this.velocity.z, z);
                ((IVec3d) this.velocity).blackout_Client$set(x, y, z);
                movement = this.velocity.multiply(this.speedH.get(), this.speedV.get(), this.speedH.get());
                break;
            case Directional:
                double rYaw = Math.toRadians(yaw);
                double rPitch = Math.toRadians(-pitch);

                double cosPitch = Math.cos(rPitch);
                double sinPitch = Math.sin(rPitch);
                double cosYaw = Math.cos(rYaw);
                double sinYaw = Math.sin(rYaw);

                double lookX = -sinYaw * cosPitch;
                double lookY = sinPitch;
                double lookZ = cosYaw * cosPitch;

                Vec3d direction = new Vec3d(lookX, lookY, lookZ);

                if (this.move) {
                    movement = direction.multiply(this.speedH.get());
                } else {
                    movement = new Vec3d(0, this.vertical * this.speedV.get(), 0);
                }
                break;
            default:
                return this.pos;
        }

        return this.pos = this.pos.add(movement.multiply(BlackOut.mc.getRenderTickCounter().getLastFrameDuration()));
    }

    private double smoothen(double from, double to) {
        return (from + to * BlackOut.mc.getRenderTickCounter().getLastFrameDuration() / 4.0) * (1.0F - BlackOut.mc.getRenderTickCounter().getLastFrameDuration() / 4.0F);
    }

    private void inputYaw(float yaw) {
        this.moveYaw = yaw;
        float forward = this.getMovementMultiplier(BlackOut.mc.options.forwardKey.isPressed(), BlackOut.mc.options.backKey.isPressed());
        float strafing = this.getMovementMultiplier(BlackOut.mc.options.leftKey.isPressed(), BlackOut.mc.options.rightKey.isPressed());
        if (forward > 0.0F) {
            this.move = true;
            this.moveYaw += strafing > 0.0F ? -45.0F : (strafing < 0.0F ? 45.0F : 0.0F);
        } else if (forward < 0.0F) {
            this.move = true;
            this.moveYaw += strafing > 0.0F ? -135.0F : (strafing < 0.0F ? 135.0F : 180.0F);
        } else {
            this.move = strafing != 0.0F;
            this.moveYaw += strafing > 0.0F ? -90.0F : (strafing < 0.0F ? 90.0F : 0.0F);
        }

        this.vertical = this.getMovementMultiplier(BlackOut.mc.options.jumpKey.isPressed(), BlackOut.mc.options.sneakKey.isPressed());
    }

    private float getMovementMultiplier(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0F;
        } else {
            return positive ? 1.0F : -1.0F;
        }
    }

    public enum Mode {
        Normal,
        Smooth,
        Directional
    }
}
