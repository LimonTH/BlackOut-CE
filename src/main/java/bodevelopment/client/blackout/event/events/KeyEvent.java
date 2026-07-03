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

package bodevelopment.client.blackout.event.events;

import bodevelopment.client.blackout.event.Cancellable;

public class KeyEvent extends Cancellable {
    private static final KeyEvent INSTANCE = new KeyEvent();
    public int key = 0;
    public boolean pressed = false;
    public boolean prev = false;

    public static KeyEvent get(int key, boolean pressed, boolean prev) {
        INSTANCE.key = key;
        INSTANCE.pressed = pressed;
        INSTANCE.prev = prev;
        return INSTANCE;
    }
}