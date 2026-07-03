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

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.menu.AltManagerScreen;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class Streamer extends Module {
    private static Streamer INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<String> spoofedName = this.sgGeneral.stringSetting("Spoofed Name", "Limon", "The alias that will replace your real username in the HUD, chat, and other UI elements.");
    public final Setting<Boolean> skin = this.sgGeneral.booleanSetting("Skin Protection", true, "Prevents your actual player skin from being rendered to hide your identity.");

    public Streamer() {
        super("Streamer", "Anonymizes your account details by spoofing your name and skin, preventing viewers from seeing your real credentials.", SubCategory.MISC, true);
        INSTANCE = this;
    }

    public static Streamer getInstance() {
        return INSTANCE;
    }

    public String replace(String string) {
        if (BlackOut.mc.screen instanceof AltManagerScreen) {
            return string;
        }

        String currentName = BlackOut.mc.getUser().getName();
        if (currentName == null || string == null) return string;

        return string.replace(currentName, this.spoofedName.get());
    }
}
