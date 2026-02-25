package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.misc.Timer;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.List;

public class Step extends Module {
    private static Step INSTANCE;

    public final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<StepMode> stepMode = this.sgGeneral.enumSetting("Elevation Logic", StepMode.NCP, "Determines the packet sequence used to transition between different heights.");
    public final Setting<Boolean> slow = this.sgGeneral.booleanSetting("Interpolated Step", false, "Spreads the elevation packets over multiple ticks to mimic natural movement.", () -> this.stepMode.get() != StepMode.Vanilla);
    public final Setting<Boolean> useTimer = this.sgGeneral.booleanSetting("Tick Synchronization", false, "Engages the client timer during the stepping process to maintain velocity.", () -> this.slow.get() && this.stepMode.get() != StepMode.Vanilla);
    public final Setting<Double> timer = this.sgGeneral.doubleSetting("Timer Intensity", 2.0, 0.0, 10.0, 0.1, "The multiplier applied to game speed while stepping.", () -> this.slow.get() && this.stepMode.get() != StepMode.Vanilla && this.useTimer.get());
    public final Setting<Double> cooldown = this.sgGeneral.doubleSetting("Activation Delay", 0.0, 0.0, 1.0, 0.01, "The minimum time interval required between successful step executions.", () -> this.stepMode.get() != StepMode.Vanilla);
    public final Setting<Double> height = this.sgGeneral.doubleSetting("Maximum Rise", 2.0, 0.0, 4.0, 0.05, "The maximum vertical distance the module is permitted to traverse.");

    public boolean shouldResetTimer = false;
    public int stepProgress = -1;
    public int sinceStep = 0;
    public double[] offsets = null;
    public double lastSlow = 0.0;
    public long lastStep = 0L;
    public Vec3d prevMovement = Vec3d.ZERO;

    public Step() {
        super("Step", "Allows the player to instantly ascend blocks of varying heights without jumping by utilizing coordinate packet offsets.", SubCategory.MOVEMENT, true);
        INSTANCE = this;
    }

    public static Step getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        this.shouldResetTimer = false;
        this.stepProgress = -1;
        this.offsets = null;
        this.lastSlow = 0.0;
    }

    @Override
    public void onDisable() {
        if (this.shouldResetTimer) {
            Timer.reset();
        }

        this.shouldResetTimer = false;
    }

    @Override
    public String getInfo() {
        return this.stepMode.get().name();
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        this.sinceStep++;
    }

    public boolean isActive() {
        return this.enabled || HoleSnap.getInstance().shouldStep() || TickShift.getInstance().shouldStep();
    }

    public boolean cooldownCheck() {
        return System.currentTimeMillis() - this.lastStep > this.cooldown.get() * 1000.0;
    }

    public void start(double step) {
        this.offsets = this.getOffsets(step);
        if (this.offsets != null) {
            this.lastSlow = step - this.sum();
        }
    }

    private double[] getOffsets(double step) {
        switch (this.stepMode.get()) {
            case NCP:
                if (step > 2.019) {
                    return new double[]{0.425, 0.396, -0.122, -0.1, 0.423, 0.35, 0.28, 0.217, 0.15, -0.1};
                }

                if (step > 1.5) {
                    return new double[]{0.42, 0.36, -0.15, -0.12, 0.39, 0.31, 0.24, -0.02};
                }

                if (step > 1.015) {
                    return new double[]{0.42, 0.3332, 0.2568, 0.083, -0.078};
                }

                if (step > 0.6) {
                    return new double[]{0.42 * step, 0.3332 * step};
                }
                break;
            case UpdatedNCP:
                if (step > 1.1661) {
                    return new double[]{0.42, 0.3332, 0.2478, 0.1651};
                }

                if (step > 1.015) {
                    return new double[]{0.42, 0.3332, 0.2478};
                }

                if (step > 0.6) {
                    return new double[]{0.42 * step, 0.3332 * step};
                }
        }

        return null;
    }

    public boolean canStrafeJump(Vec3d movement) {
        if (!this.enabled) {
            return true;
        } else if (this.sinceStep <= 1) {
            return false;
        } else {
            Box box = BlackOut.mc.player.getBoundingBox();
            List<VoxelShape> list = BlackOut.mc.world.getEntityCollisions(BlackOut.mc.player, box.stretch(movement));
            Vec3d vec3d = movement.lengthSquared() == 0.0
                    ? movement
                    : Entity.adjustMovementForCollisions(BlackOut.mc.player, movement, box, BlackOut.mc.world, list);
            boolean collidedX = movement.x != vec3d.x;
            boolean collidedZ = movement.z != vec3d.z;
            boolean collidedHorizontally = collidedX || collidedZ;
            if (!collidedHorizontally) {
                return true;
            } else {
                Vec3d stepMovement = Entity.adjustMovementForCollisions(
                        BlackOut.mc.player, new Vec3d(movement.x, 0.6, movement.z), box, BlackOut.mc.world, list
                );
                Vec3d stepMovementUp = Entity.adjustMovementForCollisions(
                        BlackOut.mc.player,
                        new Vec3d(0.0, 0.6, 0.0),
                        box.stretch(movement.x, 0.0, movement.z),
                        BlackOut.mc.world,
                        list
                );
                if (stepMovementUp.y < this.height.get()) {
                    Vec3d vec3d4 = Entity.adjustMovementForCollisions(
                                    BlackOut.mc.player,
                                    new Vec3d(movement.x, 0.0, movement.z),
                                    box.offset(stepMovementUp),
                                    BlackOut.mc.world,
                                    list
                            )
                            .add(stepMovementUp);
                    if (vec3d4.horizontalLengthSquared() > stepMovement.horizontalLengthSquared()) {
                        stepMovement = vec3d4;
                    }
                }

                return stepMovement.horizontalLengthSquared() <= vec3d.horizontalLengthSquared();
            }
        }
    }

    private double sum() {
        double v = 0.0;

        for (double d : this.offsets) {
            v += d;
        }

        return v;
    }

    public enum StepMode {
        Vanilla,
        NCP,
        UpdatedNCP
    }
}
