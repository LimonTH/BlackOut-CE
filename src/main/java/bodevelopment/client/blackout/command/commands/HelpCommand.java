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

import bodevelopment.client.blackout.command.Command;
import bodevelopment.client.blackout.manager.Managers;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "Usage: help");
    }

    @Override
    public boolean canUseOutsideWorld() {
        return true;
    }

    @Override
    public String execute(String[] args) {
        StringBuilder sb = new StringBuilder("Available commands:\n");

        List<Command> sorted = new ArrayList<>(Managers.COMMANDS.getCommands());
        sorted.sort(Comparator.comparing(c -> c.name));

        for (Command cmd : sorted) {
            sb.append(ChatFormatting.GREEN).append(cmd.name)
                    .append(ChatFormatting.GRAY).append(" - ").append(cmd.format);
            if (cmd != sorted.getLast()) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
