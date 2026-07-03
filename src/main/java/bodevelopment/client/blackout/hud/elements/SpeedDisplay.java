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
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.world.phys.Vec3;

public class SpeedDisplay extends TextElement {
    public final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Boolean> onlyHorizontal = this.sgGeneral.booleanSetting("Only Horizontal", true, "Excludes vertical velocity from calculation.");
    private final Setting<Integer> decimals = this.sgGeneral.intSetting("Decimals", 1, 0, 3, 1, "How many decimal places to show.");

    public SpeedDisplay() {
        super("Speed", "Displays your current movement velocity in blocks per second.");
        this.setSize(40.0F, 10.0F);
    }

    @Override
    public void render() {
        Vec3 vel = BlackOut.mc.player != null ? BlackOut.mc.player.getDeltaMovement() : Vec3.ZERO;
        double speed = onlyHorizontal.get() ? vel.horizontalDistance() : vel.length();
        speed *= 20.0;

        String speedString = String.format("%." + decimals.get() + "f", speed);

        this.drawElement(this.stack, "Speed", speedString + " bps");
    }
}