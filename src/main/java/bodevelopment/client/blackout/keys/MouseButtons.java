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

package bodevelopment.client.blackout.keys;

import java.util.HashMap;
import java.util.Map;

public class MouseButtons {
    public static final boolean[] state = new boolean[50];
    private static final Map<Integer, String> names = new HashMap<>();

    static {
        names.put(0, "LEFT");
        names.put(1, "RIGHT");
        names.put(2, "MIDDLE");
    }

    public static String getKeyName(int key) {
        return names.computeIfAbsent(key, MouseButtons::getNameFromKey);
    }

    private static String getNameFromKey(int key) {
        return "mouse" + key;
    }

    public static boolean get(int key) {
        if (key < 0 || key >= state.length) return false;
        return state[key];
    }

    public static void set(int key, boolean s) {
        if (key >= 0 && key < state.length) {
            state[key] = s;
        }
    }
}
