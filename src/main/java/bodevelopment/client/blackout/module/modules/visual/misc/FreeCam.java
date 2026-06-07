package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MouseScrollEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.interfaces.mixin.IVec3;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

public class FreeCam extends Module {
    private static FreeCam INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Mode> mode = this.sgGeneral.enumSetting("Movement Mode", Mode.Normal, "The interpolation logic used for camera translation.");
    private final Setting<Double> speedH = this.sgGeneral.doubleSetting("Horizontal Velocity", 1.0, 0.1, 10.0, 0.1, "The movement speed multiplier for the lateral X and Z axes.");
    private final Setting<Double> speedV = this.sgGeneral.doubleSetting("Vertical Velocity", 1.0, 0.1, 10.0, 0.1, "The movement speed multiplier for the vertical Y axis.");
    private final Setting<Double> speedScrollSensitivity = this.sgGeneral.doubleSetting("Speed Scroll Sensitivity", 0.0, 0.0, 2.0, 0.1, "Allows you to change speed value using scroll wheel. 0 to disable.");

    public final Vec3 velocity = new Vec3(0.0, 0.0, 0.0);
    public Vec3 pos = Vec3.ZERO;
    public float yaw, pitch, lastYaw, lastPitch;
    public double currentSpeedH;
    public double currentSpeedV;
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

    @Override
    public void onEnable() {
        super.onEnable();
        if (BlackOut.mc.player != null) {
            this.yaw = BlackOut.mc.player.getYRot();
            this.pitch = BlackOut.mc.player.getXRot();
            this.lastYaw = this.yaw;
            this.lastPitch = this.pitch;
            this.currentSpeedH = this.speedH.get();
            this.currentSpeedV = this.speedV.get();

            if (BlackOut.mc.gameRenderer.getMainCamera().isInitialized()) {
                this.pos = BlackOut.mc.gameRenderer.getMainCamera().getPosition();
            } else {
                this.pos = BlackOut.mc.player.getEyePosition();
            }
        }
    }

    @Event
    public void onRender(TickEvent.Pre event) {
        if (BlackOut.mc.level == null || BlackOut.mc.player == null) {
            this.disable();
        }
    }

    @Event
    public void onMouseScroll(MouseScrollEvent event) {
        if (this.speedScrollSensitivity.get() > 0 && BlackOut.mc.screen == null) {
            double multiplier = event.vertical * 0.25 * this.speedScrollSensitivity.get();

            this.currentSpeedH += multiplier * this.currentSpeedH;
            if (this.currentSpeedH < 0.1) this.currentSpeedH = 0.1;

            this.currentSpeedV += multiplier * this.currentSpeedV;
            if (this.currentSpeedV < 0.1) this.currentSpeedV = 0.1;

            event.cancel();
        }
    }

    public void resetInput(KeyboardInput input) {
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.keyPresses = Input.EMPTY;
    }

    public void changeLookDirection(double deltaX, double deltaY) {
        this.lastYaw = this.yaw;
        this.lastPitch = this.pitch;

        this.yaw += (float) deltaX;
        this.pitch += (float) deltaY;

        this.pitch = net.minecraft.util.Mth.clamp(this.pitch, -90.0F, 90.0F);
    }

    public Vec3 getPos(float cameraYaw, float cameraPitch) {
        this.inputYaw(cameraYaw);
        Vec3 movement;
        double rad;
        double x;
        double y;
        double z;
        switch (this.mode.get()) {
            case Normal:
                rad = Math.toRadians(this.moveYaw + 90.0F);
                x = 0.0;
                y = this.vertical * this.currentSpeedV;
                z = 0.0;
                if (this.move) {
                    x = Math.cos(rad) * this.currentSpeedH;
                    z = Math.sin(rad) * this.currentSpeedH;
                }
                movement = new Vec3(x, y, z);
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
                ((IVec3) this.velocity).blackout_Client$set(x, y, z);
                movement = this.velocity.multiply(this.currentSpeedH, this.currentSpeedV, this.currentSpeedH);
                break;
            case Directional:
                double rYaw = Math.toRadians(cameraYaw);
                double rPitch = Math.toRadians(-cameraPitch);
                double cosPitch = Math.cos(rPitch);
                double sinPitch = Math.sin(rPitch);
                double cosYaw = Math.cos(rYaw);
                double sinYaw = Math.sin(rYaw);
                double lookX = -sinYaw * cosPitch;
                double lookY = sinPitch;
                double lookZ = cosYaw * cosPitch;
                Vec3 direction = new Vec3(lookX, lookY, lookZ);
                if (this.move) {
                    movement = direction.scale(this.currentSpeedH);
                } else {
                    movement = new Vec3(0, this.vertical * this.currentSpeedV, 0);
                }
                break;
            case Simple:
                double radS = Math.toRadians(this.yaw);
                double xS = 0.0;
                double yS = 0.0;
                double zS = 0.0;

                double s = BlackOut.mc.options.keySprint.isDown() ? 1.0 : 0.5;
                boolean forwardS = BlackOut.mc.options.keyUp.isDown();
                boolean backwardS = BlackOut.mc.options.keyDown.isDown();
                boolean rightS = BlackOut.mc.options.keyRight.isDown();
                boolean leftS = BlackOut.mc.options.keyLeft.isDown();
                boolean upS = BlackOut.mc.options.keyJump.isDown();
                boolean downS = BlackOut.mc.options.keyShift.isDown();

                Vec3 forwardVec = new Vec3(-Math.sin(radS), 0, Math.cos(radS));
                Vec3 rightVec = new Vec3(-Math.sin(radS + Math.PI / 2), 0, Math.cos(radS + Math.PI / 2));

                if (forwardS) {
                    xS += forwardVec.x * s * this.currentSpeedH;
                    zS += forwardVec.z * s * this.currentSpeedH;
                }
                if (backwardS) {
                    xS -= forwardVec.x * s * this.currentSpeedH;
                    zS -= forwardVec.z * s * this.currentSpeedH;
                }
                if (rightS) {
                    xS += rightVec.x * s * this.currentSpeedH;
                    zS += rightVec.z * s * this.currentSpeedH;
                }
                if (leftS) {
                    xS -= rightVec.x * s * this.currentSpeedH;
                    zS -= rightVec.z * s * this.currentSpeedH;
                }

                if (upS) yS += s * this.currentSpeedV;
                if (downS) yS -= s * this.currentSpeedV;

                movement = new Vec3(xS, yS, zS);
                break;
            default:
                return this.pos;
        }

        return this.pos = this.pos.add(movement.scale(BlackOut.mc.getDeltaTracker().getGameTimeDeltaTicks()));
    }

    private double smoothen(double from, double to) {
        return (from + to * BlackOut.mc.getDeltaTracker().getGameTimeDeltaTicks() / 4.0) * (1.0F - BlackOut.mc.getDeltaTracker().getGameTimeDeltaTicks() / 4.0F);
    }

    private void inputYaw(float yawIn) {
        this.moveYaw = yawIn;
        float forward = this.getMovementMultiplier(BlackOut.mc.options.keyUp.isDown(), BlackOut.mc.options.keyDown.isDown());
        float strafing = this.getMovementMultiplier(BlackOut.mc.options.keyLeft.isDown(), BlackOut.mc.options.keyRight.isDown());
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

        this.vertical = this.getMovementMultiplier(BlackOut.mc.options.keyJump.isDown(), BlackOut.mc.options.keyShift.isDown());
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
        Directional,
        Simple
    }
}