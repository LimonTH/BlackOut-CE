/*
 * Blackout Client (CE) - A cutting-edge, feature-rich cheat client for Minecraft.
 * A modernized continuation of the original Blackout project by OLEPOSSU & KassuK.
 * Copyright (C) 2026  LimonTH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.manager.managers.ExtrapolationManager;
import bodevelopment.client.blackout.randomstuff.MotionData;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class HorizontalExtrapolation {
    public static MotionData getMotion(ExtrapolationManager.ExtrapolationData data) {
        List<Vec3> motions = data.motions.reversed();
        if (motions.size() < 5) {
            return MotionData.of(motions.isEmpty() ? Vec3.ZERO : motions.getFirst());
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
            double x = 0.0, z = 0.0;
            for (Vec3 m : motions) {
                x += m.x;
                z += m.z;
            }
            double inv = 1.0 / motions.size();
            x *= inv;
            z *= inv;
            if (Math.abs(lastDiff) > 115.0 && Math.abs(avg) > 10.0) {
                return x * x + z * z > 0.0225
                        ? MotionData.of(Vec3.ZERO).reset()
                        : MotionData.of(Vec3.ZERO);
            } else {
                return MotionData.of(new Vec3(x, 0.0, z));
            }
        }
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
