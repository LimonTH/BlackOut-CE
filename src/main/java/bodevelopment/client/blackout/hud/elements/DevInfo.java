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

package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BackgroundMultiSetting;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.render.Render2DUtils;

import java.util.ArrayList;
import java.util.List;

public class DevInfo extends HudElement {
    public final SettingGroup sgGeneral = this.addGroup("General");

    private final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgGeneral, "Label");
    private final Setting<Boolean> bg = this.sgGeneral.booleanSetting("Backdrop", true, "Renders a background panel behind the version information.");
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgGeneral, this.bg::get, null);
    private final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Blur Effect", true, "Applies a real-time blur effect to the backdrop for improved clarity.");
    private final Setting<Boolean> rounded = this.sgGeneral.booleanSetting("Rounded Corners", true, "Smooths the corners of the background and blur layers.", () -> this.bg.get() || this.blur.get());
    private final Setting<Boolean> typeColor = this.sgGeneral.booleanSetting("Thematic Color", false, "Uses a unique color palette based on the specific build type (e.g., Release, Beta, or Debug).");

    private final List<Component> components = new ArrayList<>();
    private float offset = 0.0F;

    public DevInfo() {
        super("Dev Info", "Displays comprehensive internal client metadata, including the current build type and versioning information.");
        this.setSize(10.0F, 10.0F);
    }

    @Override
    public void render() {
        this.components.clear();
        String text = BlackOut.TYPE + " Build — " + BlackOut.VERSION;
        this.components.add(new Component(""));
        this.components.add(new Component(BlackOut.TYPE.name(), this.typeColor.get() ? BlackOut.TYPECOLOR : null, true));
        this.components.add(new Component(" Build — " + BlackOut.VERSION));
        this.stack.pushPose();
        if (this.blur.get()) {
            Render2DUtils.drawLoadedBlur(
                    "hudblur",
                    this.stack,
                    renderer -> renderer.rounded(0.0F, 0.0F, BlackOut.FONT.getWidth(text), BlackOut.FONT.getHeight(), this.rounded.get() ? 3.0F : 0.0F, 10)
            );
            Renderer.onHUDBlur();
        }

        if (this.bg.get()) {
            this.background.render(this.stack, 0.0F, 0.0F, BlackOut.FONT.getWidth(text), BlackOut.FONT.getHeight(), this.rounded.get() ? 3.0F : 0.0F, 3.0F);
        }

        this.setSize(BlackOut.FONT.getWidth(text), BlackOut.FONT.getHeight());
        this.offset = 0.0F;
        this.components.forEach(component -> {
            if (component.color == null) {
                this.textColor.render(this.stack, component.text, 1.0F, this.offset, 0.0F, false, false, component.bold);
            } else if (component.bold) {
                BlackOut.BOLD_FONT.text(this.stack, component.text, 1.0F, this.offset, 0.0F, component.color, false, false);
            } else {
                BlackOut.FONT.text(this.stack, component.text, 1.0F, this.offset, 0.0F, component.color, false, false);
            }

            this.offset = this.offset + component.width;
        });
        this.stack.popPose();
    }
}
