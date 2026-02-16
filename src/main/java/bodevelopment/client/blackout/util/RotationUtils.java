package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RotationUtils {
    public static double getYaw(Entity entity) {
        return getYaw(BlackOut.mc.player.getEyePos(), entity.getPos(), 0.0);
    }

    public static double getPitch(Entity entity) {
        return getPitch(entity.getPos());
    }

    public static double getYaw(BlockPos pos) {
        return getYaw(pos.toCenterPos());
    }

    public static double getPitch(BlockPos pos) {
        return getPitch(pos.toCenterPos());
    }

    public static double getYaw(Vec3d vec) {
        return getYaw(BlackOut.mc.player.getEyePos(), vec, 0.0);
    }

    public static double getPitch(Vec3d vec) {
        return getPitch(BlackOut.mc.player.getEyePos(), vec);
    }

    public static float nextYaw(double current, double target, double step) {
        double i = yawAngle(current, target);
        return step >= Math.abs(i) ? (float) (current + i) : (float) (current + (i < 0.0 ? -1 : 1) * step);
    }

    public static double yawAngle(double current, double target) {
        double c = MathHelper.wrapDegrees(current) + 180.0;
        double t = MathHelper.wrapDegrees(target) + 180.0;
        if (c > t) {
            return t + 360.0 - c < Math.abs(c - t) ? 360.0 - c + t : t - c;
        } else {
            return 360.0 - t + c < Math.abs(c - t) ? -(360.0 - t + c) : t - c;
        }
    }

    public static double pitchAngle(double current, double target) {
        return target - current;
    }

    public static float nextPitch(double current, double target, double step) {
        double i = pitchAngle(current, target);
        return step >= Math.abs(i) ? (float) target : (float) (i >= 0.0 ? current + step : current - step);
    }

    public static double radAngle(Vec3d vec1, Vec3d vec2) {
        return Math.acos(Math.min(1.0, Math.max(vec1.dotProduct(vec2) / (vec1.length() * vec2.length()), -1.0)));
    }

    public static double getYaw(Vec3d start, Vec3d target, double yaw) {
        double diffX = target.x - start.x;
        double diffZ = target.z - start.z;

        double angle = Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0;

        return yaw + MathHelper.wrapDegrees(angle - yaw);
    }

    public static double getPitch(Vec3d start, Vec3d target) {
        double dx = target.x - start.x;
        double dy = target.y - start.y;
        double dz = target.z - start.z;

        double distanceXZ = Math.sqrt(dx * dx + dz * dz);

        return MathHelper.wrapDegrees(-Math.toDegrees(Math.atan2(dy, distanceXZ)));
    }

    public static Vec3d rotationVec(double yaw, double pitch, Vec3d from, double distance) {
        return from.add(rotationVec(yaw, pitch, distance));
    }

    public static Vec3d rotationVec(double yaw, double pitch, double range) {
        double rp = Math.toRadians(pitch);
        double ry = -Math.toRadians(yaw);
        double c = Math.cos(rp);
        return new Vec3d(range * Math.sin(ry) * c, range * -Math.sin(rp), range * Math.cos(ry) * c);
    }
}
