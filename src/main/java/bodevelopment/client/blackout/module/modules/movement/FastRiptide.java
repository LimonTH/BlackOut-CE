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

package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class FastRiptide extends Module {
    private static FastRiptide INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Double> cooldown = this.sgGeneral.doubleSetting("Riptide Delay", 0.0, 0.0, 1.0, 0.01, "The minimum interval in seconds between consecutive Riptide launches.");

    public long prevRiptide = 0L;

    public FastRiptide() {
        super("Fast Riptide", "Reduces or removes the internal cooldown of the Riptide enchantment for rapid trident launches.", SubCategory.MOVEMENT, false);
        INSTANCE = this;
    }

    public static FastRiptide getInstance() {
        return INSTANCE;
    }
}
