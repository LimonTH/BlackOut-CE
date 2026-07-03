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
import bodevelopment.client.blackout.util.ChatUtils;
import bodevelopment.client.blackout.util.PlayerUtils;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

public class AutoGG extends Module {
    public AutoGG() {
        super("Auto GG", "Automatically sends a congratulatory message in chat upon the conclusion of a match or after a death.", SubCategory.MISC, true);
    }

    @Event
    public void onReceive(PacketEvent.Receive.Pre event) {
        if (PlayerUtils.isInGame()) {
            if (event.packet instanceof ClientboundSystemChatPacket packet) {
                String unformattedText = packet.content().getString();
                String[] look = new String[]{
                        "You won! Want to play again? Click here! ", "You lost! Want to play again? Click here! ", "You died! Want to play again? Click here! "
                };
                if (unformattedText == null) {
                    return;
                }

                for (String s : look) {
                    if (unformattedText.contains(s)) {
                        ChatUtils.sendMessage("gg");
                    }
                }
            }
        }
    }
}
