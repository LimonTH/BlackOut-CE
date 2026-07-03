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

import java.util.Collections;
import java.util.List;

public class HClipCommand extends Command {
    public HClipCommand() {
        super("hclip", "Usage: hclip <xdist>");
    }

    @Override
    public String execute(String[] args) {
        if (args.length > 0) {
            try {
                double value = Double.parseDouble(args[0].replace(",", "."));
                double yaw = Math.toRadians(BlackOut.mc.player.getYRot() + 90.0F);
                BlackOut.mc.player.setPos(
                        BlackOut.mc.player.getX() + Math.cos(yaw) * value,
                        BlackOut.mc.player.getY(),
                        BlackOut.mc.player.getZ() + Math.sin(yaw) * value
                );
                return "Teleported " + value + " blocks horizontally.";
            } catch (Exception e) {
                return "invalid amount";
            }
        }
        return this.format;
    }

    @Override
    public List<String> getSuggestions(String[] args) {
        if (args.length == 1) {
            return List.of(
                    "<xdist>"
            );
        }
        return Collections.emptyList();
    }
}
