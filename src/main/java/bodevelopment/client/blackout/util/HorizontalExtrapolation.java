package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.manager.managers.ExtrapolationManager;
import bodevelopment.client.blackout.randomstuff.MotionData;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class HorizontalExtrapolation {
    public static MotionData getMotion(ExtrapolationManager.ExtrapolationData data) {
        List<Vec3d> motions = OLEPOSSUtils.reverse(data.motions);
        if (motions.size() < 5) {
            return MotionData.of(motions.isEmpty() ? new Vec3d(0.0, 0.0, 0.0) : motions.getFirst());
        } else {
            double prev = motionYaw(motions.getFirst());
            Yaw[] yaws = new Yaw[motions.size() - 1];

            for (int i = 0; i < yaws.length; i++) {
                Vec3d motion = motions.get(i + 1);
                double yaw = motionYaw(motion);
                yaws[i] = new Yaw(yaw, yaw - prev, motion.horizontalLength());
                prev = yaw;
            }

            double avg = avgDiff(yaws);
            double lastDiff = yaws[3].diff();
            if (Math.abs(lastDiff) > 115.0 && Math.abs(avg) > 10.0) {
                Vec3d average = averageMotion(motions);
                return average.horizontalLength() > 0.15
                        ? MotionData.of(averageMotion(motions).multiply(0.0)).reset()
                        : MotionData.of(averageMotion(motions).multiply(0.0));
            } else {
                return MotionData.of(averageMotion(motions));
            }
        }
    }

    private static Vec3d averageMotion(List<Vec3d> motions) {
        Vec3d total = new Vec3d(0.0, 0.0, 0.0);

        for (Vec3d motion : motions) {
            total = total.add(motion);
        }

        return total.multiply(1.0F / motions.size(), 0.0, 1.0F / motions.size());
    }

    private static double avgDiff(Yaw[] yaws) {
        double avg = 0.0;

        for (Yaw yaw : yaws) {
            avg += yaw.diff() / yaws.length;
        }

        return avg;
    }

    private static double motionYaw(Vec3d motion) {
        return RotationUtils.getYaw(Vec3d.ZERO, motion, 0.0);
    }

    private record Yaw(double yaw, double diff, double length) {
    }
}
