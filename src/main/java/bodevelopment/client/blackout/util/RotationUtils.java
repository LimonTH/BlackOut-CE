package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class RotationUtils {
    public static double getYaw(Entity entity) {
        return getYaw(BlackOut.mc.player.getEyePosition(), entity.position(), 0.0);
    }

    public static double getPitch(Entity entity) {
        return getPitch(entity.position());
    }

    public static double getYaw(BlockPos pos) {
        return getYaw(pos.getCenter());
    }

    public static double getPitch(BlockPos pos) {
        return getPitch(pos.getCenter());
    }

    public static double getYaw(Vec3 vec) {
        return getYaw(BlackOut.mc.player.getEyePosition(), vec, 0.0);
    }

    public static double getPitch(Vec3 vec) {
        return getPitch(BlackOut.mc.player.getEyePosition(), vec);
    }

    public static float nextYaw(double current, double target, double step) {
        double i = yawAngle(current, target);
        return step >= Math.abs(i) ? (float) (current + i) : (float) (current + (i < 0.0 ? -1 : 1) * step);
    }

    public static double yawAngle(double current, double target) {
        double c = Mth.wrapDegrees(current) + 180.0;
        double t = Mth.wrapDegrees(target) + 180.0;
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

    public static double radAngle(Vec3 vec1, Vec3 vec2) {
        return Math.acos(Math.min(1.0, Math.max(vec1.dot(vec2) / (vec1.length() * vec2.length()), -1.0)));
    }

    public static double getYaw(Vec3 start, Vec3 target, double yaw) {
        double diffX = target.x - start.x;
        double diffZ = target.z - start.z;

        double angle = Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0;

        return yaw + Mth.wrapDegrees(angle - yaw);
    }

    public static double getPitch(Vec3 start, Vec3 target) {
        double dx = target.x - start.x;
        double dy = target.y - start.y;
        double dz = target.z - start.z;

        double distanceXZ = Math.sqrt(dx * dx + dz * dz);

        return Mth.wrapDegrees(-Math.toDegrees(Math.atan2(dy, distanceXZ)));
    }

    public static Vec3 rotationVec(double yaw, double pitch, Vec3 from, double distance) {
        return from.add(rotationVec(yaw, pitch, distance));
    }

    public static Vec3 rotationVec(double yaw, double pitch, double range) {
        double rp = Math.toRadians(pitch);
        double ry = -Math.toRadians(yaw);
        double c = Math.cos(rp);
        return new Vec3(range * Math.sin(ry) * c, range * -Math.sin(rp), range * Math.cos(ry) * c);
    }
}
