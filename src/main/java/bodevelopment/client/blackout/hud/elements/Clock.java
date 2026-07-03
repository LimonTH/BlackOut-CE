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

import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class Clock extends HudElement {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Mode> mode = this.sgGeneral.enumSetting("Time Format", Mode.Normal, "The nomenclature used for displaying the system time (24-hour vs. 12-hour AM/PM).");
    private final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgGeneral, "Time");
    private final Setting<Boolean> bg = this.sgGeneral.booleanSetting("Backdrop", true, "Renders a background panel behind the time string.");
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgGeneral, this.bg::get, null);
    private final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Blur Effect", true, "Applies a blur shader to the background for enhanced contrast.");
    private final Setting<Boolean> rounded = this.sgGeneral.booleanSetting("Rounded Corners", true, "Smooths the corners of the background and blur layers.", () -> this.bg.get() || this.blur.get());

    private float textWidth = 0.0F;

    public Clock() {
        super("Clock", "Displays the current local system time with customizable formatting and post-processing effects.");
    }

    @Override
    public void render() {
        String time = switch (this.mode.get()) {
            case Normal -> LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            case American -> new SimpleDateFormat("hh:mm a").format(new Date());
        };
        this.textWidth = BlackOut.FONT.getWidth(time);
        this.setSize(this.textWidth, BlackOut.FONT.getHeight());
        this.stack.pushPose();
        if (this.blur.get()) {
            Render2DUtils.drawLoadedBlur(
                    "hudblur", this.stack, renderer -> renderer.rounded(0.0F, 0.0F, this.textWidth, BlackOut.FONT.getHeight(), this.rounded.get() ? 3.0F : 0.0F, 10)
            );
            Renderer.onHUDBlur();
        }

        if (this.bg.get()) {
            this.background.render(this.stack, 0.0F, 0.0F, this.textWidth, BlackOut.FONT.getHeight(), this.rounded.get() ? 3.0F : 0.0F, 3.0F);
        }

        this.textColor.render(this.stack, time, 1.0F, 0.0F, 0.0F, false, false);
        this.stack.popPose();
    }

    public enum Mode {
        Normal,
        American
    }
}
