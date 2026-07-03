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

public class MouseScrollEvent {
    private static final MouseScrollEvent INSTANCE = new MouseScrollEvent();
    public double horizontal = 0.0;
    public double vertical = 0.0;
    private boolean cancelled = false;

    public static MouseScrollEvent get(double horizontal, double vertical) {
        INSTANCE.horizontal = horizontal;
        INSTANCE.vertical = vertical;
        INSTANCE.cancelled = false;
        return INSTANCE;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }
}