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

public class SelectedComponent {
    private static int id = -1;
    private static int prevId = 0;

    public static boolean isSelected() {
        return id != -1;
    }

    public static void reset() {
        id = -1;
    }

    public static boolean is(int newId) {
        return id == newId;
    }

    public static int getId() {
        return id;
    }

    public static void setId(int newId) {
        id = newId;
    }

    public static int nextId() {
        return prevId++;
    }
}
