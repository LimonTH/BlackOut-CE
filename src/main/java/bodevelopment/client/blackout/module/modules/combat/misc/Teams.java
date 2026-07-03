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

package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;

public class Teams extends Module {
    private static Teams INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Boolean> colorCheck = this.sgGeneral.booleanSetting("Color Check", true, "Treats players with the same Tab/Name color as allies.");

    public Teams() {
        super("Teams", "Inhibits combat modules from targeting allies based on team criteria.", SubCategory.MISC_COMBAT, true);
        INSTANCE = this;
    }

    public static Teams getInstance() {
        return INSTANCE;
    }

    public boolean isTeammate(Player player) {
        if (this.colorCheck.get()) {
            TextColor localColor = BlackOut.mc.player.getDisplayName().getStyle().getColor();
            TextColor playerColor = player.getDisplayName().getStyle().getColor();
            return localColor == playerColor;
        } else {
            return false;
        }
    }
}
