package bodevelopment.client.blackout.randomstuff;


import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class MotionData {
    public Vec3d motion;
    public double yawDiff = 0.0;
    public boolean reset = false;

    public MotionData(Vec3d motion) {
        this.motion = motion;
    }

    public static MotionData of(Vec3d motion) {
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
        this.motion = this.motion.withAxis(Direction.Axis.Y, y);
        return this;
    }
}
