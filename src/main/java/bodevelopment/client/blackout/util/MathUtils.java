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

import bodevelopment.client.blackout.annotations.NoAlloc;
import bodevelopment.client.blackout.annotations.PublicAPI;

@PublicAPI
@NoAlloc
public class MathUtils {
    // Projectile physics constants
    public static final double THROWABLE_GRAVITY = 0.03;
    public static final double ARROW_GRAVITY = 0.05;
    public static final double EXP_BOTTLE_GRAVITY = 0.07;
    public static final double PROJECTILE_AIR_DRAG = 0.99;
    public static final double PROJECTILE_WATER_DRAG_FAST = 0.8;
    public static final double PROJECTILE_WATER_DRAG_SLOW = 0.6;

    public static double safeDivide(double v1, double v2) {
        double result = v1 / v2;
        return Double.isNaN(result) ? 1.0 : result;
    }

    public static int closerToZero(int x) {
        return (int) (x - Math.signum((float) x));
    }

    public static double approach(double from, double to, double delta) {
        return to > from ? Math.min(from + delta, to) : Math.max(from - delta, to);
    }
}
