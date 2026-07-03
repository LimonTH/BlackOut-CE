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

package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;

public class PortalGodMode extends Module {
    public PortalGodMode() {
        super("Portal God Mode", "Grants temporary invulnerability by canceling teleport confirmation packets while standing inside a portal.", SubCategory.MISC, true);
    }

    @Event
    public void onSend(PacketEvent.Send event) {
        if (event.packet instanceof ServerboundAcceptTeleportationPacket) {
            event.setCancelled(true);
        }
    }
}
