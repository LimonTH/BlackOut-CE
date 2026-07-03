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

import bodevelopment.client.blackout.annotations.PublicAPI;

import bodevelopment.client.blackout.module.SettingsModule;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.theme.Theme;

import java.util.ArrayList;
import java.util.List;

@PublicAPI
public class ThemeSettings extends SettingsModule {
    private static ThemeSettings INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Theme> theme = this.sgGeneral.enumSetting("Theme", Theme.BLACKOUT,
            "Selects the global color palette for the client. This affects buttons, sliders, and highlights across all modules.");
    public final Setting<Integer> alpha = this.sgGeneral.intSetting("Color Alpha", 175, 0, 255, 1,
            "The primary transparency level for main background elements. Lower values create a more translucent, glass-like effect.");
    public final Setting<Integer> lowAlpha = this.sgGeneral.intSetting("Low Alpha", 50, 0, 255, 1,
            "A secondary, lower transparency level used for subtle overlays, disabled states, and accent backgrounds.");

    public static final List<Theme> themes = new ArrayList<>();

    public ThemeSettings() {
        super("Theme", true, false);
        INSTANCE = this;
    }

    public static ThemeSettings getInstance() {
        return INSTANCE;
    }

    public Theme getTheme() {
        return this.theme.get();
    }

    public List<Theme> getThemes() {
        return themes;
    }

    public int getMain(int alpha) {
        return this.getTheme().mainWithAlpha(alpha);
    }

    public int getSecond(int alpha) {
        return this.getTheme().secondaryWithAlpha(alpha);
    }

    public int getMain() {
        return this.getTheme().getMain();
    }

    public int getSecond() {
        return this.getTheme().getSecondary();
    }

    public int alpha() {
        return this.alpha.get();
    }

}
