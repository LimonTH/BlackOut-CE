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

import bodevelopment.client.blackout.hud.TextElement;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.setting.Setting;

public class TPS extends TextElement {
    private final Setting<Boolean> showRegion = this.sgGeneral.booleanSetting("Region TPS", false, "Displays the estimated TPS for the chunk region around you based on entity update frequency.");

    public TPS() {
        super("TPS", "Displays the current server-side Ticks Per Second (TPS) to monitor game state synchronization.");
    }

    @Override
    public void render() {
        String tps = String.format("%.1f", Managers.TPS.tps);

        if (this.showRegion.get()) {
            String region = String.format("%.1f", Managers.TPS.regionTps);
            this.drawElement(this.stack, "TPS", tps + " | " + region);
        } else {
            this.drawElement(this.stack, "TPS", tps);
        }
    }
}
