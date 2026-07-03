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

package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;

public class CustomChat extends Module {
    private static CustomChat INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgGeneral, "Label");
    public final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Gaussian Blur", true, "Applies a blur effect behind the chat window to improve legibility.");
    public final Setting<Boolean> background = this.sgGeneral.booleanSetting("Custom Background", true, "Enables a specialized background plate for the chat history.");
    public final Setting<Boolean> shadow = this.sgGeneral.booleanSetting("Drop Shadow", true, "Renders a soft shadow beneath the background plate.", this.background::get);
    public final Setting<BlackOutColor> shadowColor = this.sgGeneral.colorSetting("Shadow Color", new BlackOutColor(0, 0, 0, 100), "The color and opacity of the background shadow.", () -> this.background.get() && this.shadow.get());
    public final Setting<BlackOutColor> bgColor = this.sgGeneral.colorSetting("Plate Color", new BlackOutColor(0, 0, 0, 50), "The base color and transparency of the chat background.", this.background::get);

    public CustomChat() {
        super("Custom Chat", "Enhances the chat interface with customizable background geometry, blur effects, and font rendering options.", SubCategory.MISC_VISUAL, true);
        INSTANCE = this;
    }

    public static CustomChat getInstance() {
        return INSTANCE;
    }
}
