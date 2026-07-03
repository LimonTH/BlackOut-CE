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
import net.minecraft.world.phys.AABB;

public class CollisionShrink extends Module {
    private static CollisionShrink INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Integer> shrinkAmount = this.sgGeneral.intSetting("Compression Level", 1, 1, 10, 1, "The intensity of the bounding box contraction. Higher values increase the likelihood of phasing through block edges.");

    public CollisionShrink() {
        super("Collision Shrink", "Slightly contracts your horizontal bounding box to allow for clipping through narrow gaps or block corners.", SubCategory.MOVEMENT, false);
        INSTANCE = this;
    }

    public static CollisionShrink getInstance() {
        return INSTANCE;
    }

    public AABB getBox(AABB normal) {
        double amount = 0.0625 * Math.pow(10.0, this.shrinkAmount.get()) / 1.0E10;
        return normal.deflate(amount, 0.0, amount);
    }
}
