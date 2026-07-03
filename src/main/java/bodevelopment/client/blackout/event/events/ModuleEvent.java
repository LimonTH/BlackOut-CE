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

import bodevelopment.client.blackout.module.Module;

public class ModuleEvent {
    public final Module module;

    public ModuleEvent(Module module) {
        this.module = module;
    }

    public static class Enable extends ModuleEvent {
        public Enable(Module module) {
            super(module);
        }
    }

    public static class Disable extends ModuleEvent {
        public Disable(Module module) {
            super(module);
        }
    }
}