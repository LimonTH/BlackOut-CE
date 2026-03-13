package bodevelopment.client.blackout.randomstuff;


import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class MotionData {
    public Vec3 motion;
    public double yawDiff = 0.0;
    public boolean reset = false;

    public MotionData(Vec3 motion) {
        this.motion = motion;
    }

    public static MotionData of(Vec3 motion) {
        return new MotionData(motion);
    }

    public MotionData yaw(double yawDiff) {
        this.yawDiff = yawDiff;
        return this;
    }

    public MotionData reset() {
        this.reset = true;
        return this;
    }

    public MotionData y(double y) {
        this.motion = this.motion.with(Direction.Axis.Y, y);
        return this;
    }
}
