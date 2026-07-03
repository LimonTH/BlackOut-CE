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

package bodevelopment.client.blackout.module.modules.client;

import bodevelopment.client.blackout.module.SettingsModule;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class BlurSettings extends SettingsModule {
    private static BlurSettings INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Integer> hudBlur = this.sgGeneral.intSetting("HUD Blur", 5, 1, 20, 1,
            "The intensity of the blur effect applied behind HUD elements. Higher values look smoother but can impact performance.");
    public final Setting<Integer> threeDBlur = this.sgGeneral.intSetting("3D Blur", 5, 1, 20, 1,
            "The strength of the blur shader for in-game 3D menus and background overlays. Great for visual depth.");

    public BlurSettings() {
        super("Blur", true, false);
        INSTANCE = this;
    }

    public static BlurSettings getInstance() {
        return INSTANCE;
    }

    public int getHUDBlurStrength() {
        return this.hudBlur.get();
    }

    public int get3DBlurStrength() {
        return this.threeDBlur.get();
    }
}
