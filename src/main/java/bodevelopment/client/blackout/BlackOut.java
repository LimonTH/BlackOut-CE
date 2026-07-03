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

package bodevelopment.client.blackout;

import bodevelopment.client.blackout.addon.AddonLoader;
import bodevelopment.client.blackout.event.EventBus;
import bodevelopment.client.blackout.gui.menu.MainMenu;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.setting.RegistryNames;
import bodevelopment.client.blackout.rendering.font.CustomFontRenderer;
import bodevelopment.client.blackout.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import java.awt.*;
import java.io.File;

public final class BlackOut extends bodevelopment.client.blackout.BlackOutInfo implements ClientModInitializer {
    public static final String NAME = bodevelopment.client.blackout.BlackOutInfo.NAME;
    public static final String VERSION = bodevelopment.client.blackout.BlackOutInfo.VERSION;
    /** Current BlackOut addon API version. Addons requiring a higher version are rejected. */
    public static final Integer API_VERSION = bodevelopment.client.blackout.BlackOutInfo.API_VERSION;

    public static final Type TYPE = Type.Beta;
    public static final Color TYPECOLOR = TYPE.getColor();
    public static final Minecraft mc = Minecraft.getInstance();
    public static final File RUN_DIRECTORY = mc.gameDirectory;
    public static final EventBus EVENT_BUS = new EventBus();

    public static final CustomFontRenderer FONT = new CustomFontRenderer("ubuntu");
    public static final CustomFontRenderer BOLD_FONT = new CustomFontRenderer("ubuntu-bold");

    public void onInitializeClient() {
        MainMenu.init();
        EnchantmentNames.init();
        ClassUtils.init();
        FileUtils.init();
        AddonLoader.load();
        Managers.init();
        Managers.CONFIG.readConfigs();
        Managers.CLICK_GUI.CLICK_GUI.initGui();
        RegistryNames.init();
        BlocklistUtil.loadBlocklist();
    }

    public enum Type {
        Dev(new Color(0, 175, 0, 255)),
        Beta(new Color(150, 150, 255, 255)),
        Release(new Color(255, 0, 0, 255));

        private final Color color;

        Type(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return this.color;
        }

        public boolean isDevBuild() {
            return this == Dev;
        }
    }
}
