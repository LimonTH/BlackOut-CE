package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.randomstuff.Rotation;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ProjectileUtils {
    private static double[] hitPos;

    public static Rotation calcShootingRotation(Vec3 from, Vec3 to, double speed, boolean playerVelocity, Consumer<double[]> velocityUpdate) {
        Vec3 interpolated = to.subtract(from).add(0.0, 0.1, 0.0);
        double min = -180.0;
        double max = 180.0;
        double pitch = 0.0;
        double yawTo = getYaw(interpolated.x, interpolated.z);

        for (int i = 0; i < 100; i++) {
            double middle = (min + max) / 2.0;
            double yaw = yawTo + middle;
            pitch = getPitch(interpolated, yaw, speed, playerVelocity, velocityUpdate);
            if (hitPos != null) {
                double yawOffset = RotationUtils.yawAngle(getYaw(hitPos[0], hitPos[2]), yawTo);
                if (yawOffset > 0.0) {
                    min = middle;
                } else {
                    max = middle;
                }
            }
        }

        return new Rotation((float) (yawTo + (min + max) / 2.0), (float) pitch);
    }

    private static double getYaw(double x, double z) {
        return Mth.wrapDegrees(Math.toDegrees(Math.atan2(z, x)) - 90.0);
    }

    private static double getPitch(Vec3 to, double yaw, double speed, boolean playerVelocity, Consumer<double[]> velocityUpdate) {
        double min = -90.0;
        double max = 90.0;

        for (int i = 0; i < 100; i++) {
            double middle = (min + max) / 2.0;
            double[] hitPos = calcAngleHitPos(to, getShootingVelocity(yaw, middle, speed, playerVelocity), velocityUpdate);
            if (hitPos != null) {
                if (hitPos[1] > to.y) {
                    min = middle;
                } else {
                    max = middle;
                }
            }
        }

        double middle = (min + max) / 2.0;
        ProjectileUtils.hitPos = calcAngleHitPos(to, getShootingVelocity(yaw, middle, speed, playerVelocity), velocityUpdate);
        return middle;
    }

    private static double[] calcAngleHitPos(Vec3 to, Vec3 velocity, Consumer<double[]> velocityUpdate) {
        double[] vel = new double[]{velocity.x, velocity.y, velocity.z};
        double distToTarget = to.horizontalDistanceSqr();
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;

        for (int i = 0; i < 100; i++) {
            velocityUpdate.accept(vel);
            x += vel[0];
            y += vel[1];
            z += vel[2];
            double dist = horizontalDistSq(x, z);
            if (!(dist < distToTarget)) {
                return new double[]{x, y, z};
            }
        }

        return null;
    }

    private static double horizontalDistSq(double x, double z) {
        return x * x + z * z;
    }

    public static Vec3 getShootingVelocity(double yaw, double pitch, double speed, boolean playerVelocity) {
        Vec3 vec = RotationUtils.rotationVec(yaw, pitch, speed);
        if (playerVelocity) {
            Vec3 velocity = BlackOut.mc.player.getDeltaMovement();
            vec = vec.add(velocity.x, BlackOut.mc.player.onGround() ? 0.0 : velocity.y, velocity.z);
        }

        return vec;
    }
}
