package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.manager.managers.ExtrapolationManager;
import bodevelopment.client.blackout.randomstuff.MotionData;
import java.util.List;
import net.minecraft.world.phys.Vec3;

public class HorizontalExtrapolation {
    public static MotionData getMotion(ExtrapolationManager.ExtrapolationData data) {
        List<Vec3> motions = CollectionUtils.reversed(data.motions);
        if (motions.size() < 5) {
            return MotionData.of(motions.isEmpty() ? new Vec3(0.0, 0.0, 0.0) : motions.getFirst());
        } else {
            double prev = motionYaw(motions.getFirst());
            Yaw[] yaws = new Yaw[motions.size() - 1];

            for (int i = 0; i < yaws.length; i++) {
                Vec3 motion = motions.get(i + 1);
                double yaw = motionYaw(motion);
                yaws[i] = new Yaw(yaw, yaw - prev, motion.horizontalDistance());
                prev = yaw;
            }

            double avg = avgDiff(yaws);
            double lastDiff = yaws[3].diff();
            if (Math.abs(lastDiff) > 115.0 && Math.abs(avg) > 10.0) {
                Vec3 average = averageMotion(motions);
                return average.horizontalDistance() > 0.15
                        ? MotionData.of(averageMotion(motions).scale(0.0)).reset()
                        : MotionData.of(averageMotion(motions).scale(0.0));
            } else {
                return MotionData.of(averageMotion(motions));
            }
        }
    }

    private static Vec3 averageMotion(List<Vec3> motions) {
        Vec3 total = new Vec3(0.0, 0.0, 0.0);

        for (Vec3 motion : motions) {
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

    private static double motionYaw(Vec3 motion) {
        return RotationUtils.getYaw(Vec3.ZERO, motion, 0.0);
    }

    private record Yaw(double yaw, double diff, double length) {
    }
}
