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

package bodevelopment.client.blackout.helpers;

import bodevelopment.client.blackout.interfaces.functional.SingleOut;

public class SmoothScrollHelper extends ScrollHelper {
    public SmoothScrollHelper(float friction, float speedMulti, SingleOut<Float> max, SingleOut<Float> min) {
        super(friction, speedMulti, max, min);
    }

    @Override
    protected void clamp(float min, float max) {
        this.scroll = Math.max(this.scroll, min + this.speed);
        this.scroll = Math.min(this.scroll, max + this.speed);
        if (this.scroll == max) {
            this.speed = Math.min(this.speed, 0.0F);
        }

        if (this.scroll == min) {
            this.speed = Math.max(this.speed, 0.0F);
        }
    }
}
