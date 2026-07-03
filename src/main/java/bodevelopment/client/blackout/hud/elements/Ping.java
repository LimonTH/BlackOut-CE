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
import net.minecraft.client.multiplayer.PlayerInfo;

public class Ping extends TextElement {

    public Ping() {
        super("Ping", "Displays the round-trip latency between the client and the server in milliseconds.");
    }

    @Override
    public void render() {
        this.drawElement(this.stack, "Ping:", this.getPing());
    }

    private String getPing() {
        if (BlackOut.mc.player == null || BlackOut.mc.getConnection() == null) {
            return "-1";
        }
        PlayerInfo entry = BlackOut.mc.getConnection().getPlayerInfo(BlackOut.mc.player.getGameProfile().getName());
        return entry == null ? "-1" : String.valueOf(entry.getLatency());
    }
}
