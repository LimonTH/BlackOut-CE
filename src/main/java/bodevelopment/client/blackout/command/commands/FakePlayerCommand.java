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
import bodevelopment.client.blackout.module.modules.client.settings.FakeplayerSettings;

import java.util.Collections;
import java.util.List;

public class FakePlayerCommand extends Command {
    public FakePlayerCommand() {
        super("fakeplayer", "Usage: fakeplayer [add, record, restart, heal, clear]");
    }

    @Override
    public String execute(String[] args) {
        if (args.length > 0) {
            String action = args[0];
            switch (action) {
                case "restart":
                    Managers.FAKE_PLAYER.restart();
                    return "Restarted moving";
                case "record":
                    Managers.FAKE_PLAYER.startRecording();
                    return "Started recording movement";
                case "add":
                    Managers.FAKE_PLAYER.add(FakeplayerSettings.getInstance().fakePlayerName.get());
                    return "Added a fake player";
                case "heal":
                    Managers.FAKE_PLAYER.fakePlayers.forEach(entity -> {
                        entity.setHealth(20.0F);
                        entity.setAbsorptionAmount(16.0F);
                        entity.deathTime = 0;
                        entity.popped = 0;
                    });
                    break;
                case "clear":
                    Managers.FAKE_PLAYER.clear();
                    return "Removed all fake players";
            }
        }

        return this.format;
    }

    @Override
    public List<String> getSuggestions(String[] args) {
        if (args.length == 1) {
            return List.of(
                    "add",
                    "record",
                    "restart",
                    "heal",
                    "clear"
            );
        }
        return Collections.emptyList();
    }
}
