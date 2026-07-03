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

public class Reach extends Module {
    private static Reach INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Double> entityReach = this.sgGeneral.doubleSetting("Entity Reach", 3.0, 0.0, 10.0, 0.1, "The maximum distance from which you can attack or interact with entities.");
    public final Setting<Double> blockReach = this.sgGeneral.doubleSetting("Block Reach", 4.5, 0.0, 10.0, 0.1, "The maximum distance from which you can break or place blocks.");

    public Reach() {
        super("Reach", "Extends your interaction range, allowing you to hit entities and manipulate blocks from a greater distance.", SubCategory.MISC, false);
        INSTANCE = this;
    }

    public static Reach getInstance() {
        return INSTANCE;
    }

    @Override
    public String getInfo() {
        return String.format("E: %.1f B: %.1f", this.entityReach.get(), this.blockReach.get());
    }
}
