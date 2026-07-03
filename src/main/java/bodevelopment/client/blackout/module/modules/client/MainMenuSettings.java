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

import bodevelopment.client.blackout.gui.menu.types.ColorMainMenu;
import bodevelopment.client.blackout.gui.menu.types.PanoramaMainMenu;
import bodevelopment.client.blackout.gui.menu.types.SmokeMainMenu;
import bodevelopment.client.blackout.module.SettingsModule;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.mainmenu.MainMenuRenderer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MainMenuSettings extends SettingsModule {
    /**
     * Registry for addon-provided custom menu renderers. Key = display name, Value = renderer instance.
     */
    public static final Map<String, MainMenuRenderer> CUSTOM_RENDERERS = new ConcurrentHashMap<>();

    private static MainMenuSettings INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Integer> blur = this.sgGeneral.intSetting("Blur", 5, 0, 20, 1,
            "The intensity of the blur effect applied to the background. Set to 0 to keep the background sharp.");
    private final Setting<MenuMode> mode = this.sgGeneral.enumSetting("Mode", MenuMode.Smoke,
            "Selects the visual engine used to render the main menu background.");
    public final Setting<String> customRenderer = this.sgGeneral.stringSetting("Custom Renderer", "",
            "Name of the addon-provided menu renderer to use (visible when Mode is Custom).",
            () -> this.mode.get() == MenuMode.Custom);
    public final Setting<BlackOutColor> shitfuckingmenucolor = this.sgGeneral.colorSetting("Background Color", new BlackOutColor(125, 125, 125, 255),
            "The solid background color used when the menu mode is set to 'Color'.",
            () -> this.mode.get() == MenuMode.Color);
    public final Setting<BlackOutColor> color = this.sgGeneral.colorSetting("Color", new BlackOutColor(10, 10, 10, 255),
            "The primary accent color for the animated smoke particles.",
            () -> this.mode.get() == MenuMode.Smoke);
    public final Setting<BlackOutColor> color2 = this.sgGeneral.colorSetting("Color 2", new BlackOutColor(125, 125, 125, 255),
            "The secondary color used for gradient transitions in the smoke animation.",
            () -> this.mode.get() == MenuMode.Smoke);
    public final Setting<Double> speed = this.sgGeneral.doubleSetting("Speed", 1.0, 0.0, 10.0, 0.1,
            "Controls the movement and evolution speed of the smoke effect. Higher values create a more chaotic visual.",
            () -> this.mode.get() == MenuMode.Smoke);

    public MainMenuSettings() {
        super("Main Menu", true, false);
        INSTANCE = this;
    }

    public static MainMenuSettings getInstance() {
        return INSTANCE;
    }

    public MainMenuRenderer getRenderer() {
        if (this.mode.get() == MenuMode.Custom) {
            String name = this.customRenderer.get();
            MainMenuRenderer custom = CUSTOM_RENDERERS.get(name);
            if (custom != null) return custom;
            return MenuMode.Smoke.renderer;
        }
        return this.mode.get().renderer;
    }

    public enum MenuMode {
        Smoke(new SmokeMainMenu()),
        Color(new ColorMainMenu()),
        Panorama(new PanoramaMainMenu()),
        Custom(null);

        private final MainMenuRenderer renderer;

        MenuMode(MainMenuRenderer renderer) {
            this.renderer = renderer;
        }
    }
}
