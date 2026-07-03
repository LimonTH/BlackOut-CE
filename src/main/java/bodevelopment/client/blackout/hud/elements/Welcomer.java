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
import bodevelopment.client.blackout.hud.TextElement;
import bodevelopment.client.blackout.module.setting.Setting;

import java.time.LocalTime;

public class Welcomer extends TextElement {
    public final Setting<Mode> mode = this.sgGeneral.enumSetting("Greeting Logic", Mode.Simple, "Determines whether to use a static welcome message or a time-of-day specific greeting.");

    public Welcomer() {
        super("Welcomer", "Displays a personalized greeting and welcome message on the HUD based on the current system time.");
    }

    @Override
    public void render() {
        this.stack.pushPose();
        LocalTime currentTime = LocalTime.now();
        String timetxt;
        if (currentTime.isBefore(LocalTime.NOON)) {
            timetxt = "Good Morning,";
        } else if (currentTime.isBefore(LocalTime.of(18, 0))) {
            timetxt = "Good afternoon,";
        } else if (currentTime.isBefore(LocalTime.of(22, 0))) {
            timetxt = "Good evening,";
        } else {
            timetxt = "Good night,";
        }

        String txt;
        if (this.mode.get() == Mode.Time) {
            txt = timetxt;
        } else {
            txt = "Welcome to Blackout Client";
        }

        String playerName = BlackOut.mc.player != null ? BlackOut.mc.player.getName().getString() : "Player";
        this.setSize(BlackOut.FONT.getWidth(txt), BlackOut.FONT.getHeight());
        this.drawElement(this.stack, txt, playerName);
        this.stack.popPose();
    }

    public enum Mode {
        Simple,
        Time
    }
}
