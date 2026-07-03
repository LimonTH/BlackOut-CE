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
import bodevelopment.client.blackout.util.StringUtils;

public class AntiSpam extends Module {
    private static AntiSpam INSTANCE;

    public final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Double> similarity = this.sgGeneral.doubleSetting("Similarity Threshold", 0.9, 0.0, 1.0, 0.01, "The percentage of character matching required to identify and stack similar messages.");

    public AntiSpam() {
        super("Anti Spam", "Reduces chat clutter by grouping and stacking highly similar or repetitive messages.", SubCategory.MISC, false);
        INSTANCE = this;
    }

    public static AntiSpam getInstance() {
        return INSTANCE;
    }

    public boolean isSimilar(String string1, String string2) {
        return StringUtils.similarity(string1, string2) >= this.similarity.get();
    }
}
