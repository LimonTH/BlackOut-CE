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

package bodevelopment.client.blackout.rendering.font;

import bodevelopment.client.blackout.annotations.Internal;

@Internal
public class CharInfo {
    public int x;
    public int y;
    public int width;
    public int height;
    public float tx;
    public float ty;
    public float tw;
    public float th;

    public CharInfo(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void calcTexCoords(int width, int height) {
        this.tx = (float) this.x / width;
        this.ty = (float) this.y / height;
        this.tw = (float) this.width / width;
        this.th = (float) this.height / height;
    }
}
