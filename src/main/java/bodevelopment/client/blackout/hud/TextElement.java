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

package bodevelopment.client.blackout.hud;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BackgroundMultiSetting;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.render.Render2DUtils;
import com.mojang.blaze3d.vertex.PoseStack;

public class TextElement extends HudElement {
    public final SettingGroup sgGeneral = this.addGroup("General");
    public final SettingGroup sgColor = this.addGroup("Color");

    private final Setting<Boolean> bg = this.sgGeneral.booleanSetting("Backdrop", true, "Enables the rendering of a solid or gradient background layer.");
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgGeneral, this.bg::get, null);
    private final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Gaussian Blur", true, "Applies a real-time blur effect behind the element for improved legibility.");
    private final Setting<Boolean> rounded = this.sgGeneral.booleanSetting("Rounded Corners", true, "Smooths the edges of the background and blur layers using a rounding radius.", () -> this.bg.get() || this.blur.get());

    private final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgColor, "Text");
    private final TextColorMultiSetting infoColor = TextColorMultiSetting.of(this.sgColor, "Info");

    public TextElement(String name, String description) {
        super(name, description);
    }

    protected void drawElement(PoseStack stack, String text, String info) {
        stack.pushPose();
        float width = BlackOut.FONT.getWidth(text + " " + info);
        this.setSize(width, BlackOut.FONT.getHeight());
        if (this.blur.get()) {
            Render2DUtils.drawLoadedBlur(
                    "hudblur", stack, renderer -> renderer.rounded(0.0F, 0.0F, width, BlackOut.FONT.getHeight(), this.rounded.get() ? 3.0F : 0.0F, 10)
            );
            Renderer.onHUDBlur();
        }

        if (this.bg.get()) {
            this.background.render(stack, 0.0F, 0.0F, width, BlackOut.FONT.getHeight(), this.rounded.get() ? 3.0F : 0.0F, 3.0F);
        }

        this.textColor.render(stack, text, 1.0F, 0.0F, 0.0F, false, false);
        this.infoColor.render(stack, info, 1.0F, BlackOut.FONT.getWidth(text + " "), 0.0F, false, false);
        stack.popPose();
    }
}
