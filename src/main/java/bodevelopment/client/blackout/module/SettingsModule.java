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

package bodevelopment.client.blackout.module;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.PublicAPI;

/**
 * Non-toggleable module that serves as a global settings panel.
 * Unlike {@link Module}, it has no keybind, no enabled/disabled state,
 * and its event listeners are always active.
 */
@PublicAPI
public class SettingsModule extends AbstractModule {
    public SettingsModule(String name, boolean client, boolean subscribe) {
        super(name, "Global " + name.toLowerCase() + " settings for all BlackOut modules.", client ? SubCategory.CLIENT : SubCategory.SETTINGS);

        if (subscribe) {
            BlackOut.EVENT_BUS.subscribe(this, this::shouldSkipListeners);
        }
    }

    @Override
    public boolean shouldSkipListeners() {
        return false;
    }
}
