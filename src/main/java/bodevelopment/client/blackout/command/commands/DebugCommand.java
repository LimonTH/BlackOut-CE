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

package bodevelopment.client.blackout.command.commands;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.command.Command;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Collections;
import java.util.List;

public class DebugCommand extends Command {
    public DebugCommand() {
        super("debug", "Usage: debug [send]");
    }

    @Override
    public String execute(String[] args) {
        if (args.length > 0 && args[0].equals("send")) {

            String ip = "Main Menu";

            if (BlackOut.mc.isLocalServer()) {
                ip = "Singleplayer";
            }
            if (!BlackOut.mc.isLocalServer() && BlackOut.mc.getConnection() != null) {
                ServerData serverInfo = BlackOut.mc.getConnection().getServerData();
                if (serverInfo != null) {
                    ip = serverInfo.ip;
                }
            }

            return BlackOut.NAME + " " + BlackOut.VERSION + " " + BlackOut.TYPE + " " + ip;
        } else {
            return this.format;
        }
    }

    @Override
    public List<String> getSuggestions(String[] args) {
        if (args.length == 1) {
            return List.of("send");
        }
        return Collections.emptyList();
    }

    @Override
    public boolean canUseOutsideWorld() {
        return true;
    }
}
