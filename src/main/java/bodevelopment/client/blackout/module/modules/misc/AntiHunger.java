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

package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class AntiHunger extends Module {
    private static AntiHunger INSTANCE;

    public final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Boolean> sprint = this.sgGeneral.booleanSetting("Cancel Sprint Packets", true, "Suppresses sprint state synchronization with the server to minimize exhaustion.");
    public final Setting<Boolean> moving = this.sgGeneral.booleanSetting("On-Ground Spoofing", true, "Spoofs the player's ground state to prevent the server from calculating movement-based hunger loss.");

    public AntiHunger() {
        super("Anti Hunger", "Reduces or eliminates hunger depletion by intercepting movement and action packets.", SubCategory.MISC, true);
        INSTANCE = this;
    }

    public static AntiHunger getInstance() {
        return INSTANCE;
    }
}
