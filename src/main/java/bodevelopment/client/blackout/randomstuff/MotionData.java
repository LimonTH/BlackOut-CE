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
