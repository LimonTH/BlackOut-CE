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

package bodevelopment.client.blackout.command;

import bodevelopment.client.blackout.annotations.PublicAPI;

import java.util.Collections;
import java.util.List;

@PublicAPI
public class Command {
    public final String name;
    public final String usage;
    public final String format;

    public Command(String name, String format) {
        this.name = name;
        this.usage = name.toLowerCase();
        this.format = format;
    }

    public boolean canUseOutsideWorld() {
        return false;
    }

    public List<String> getSuggestions(String[] args) {
        return Collections.emptyList();
    }

    public String execute(String[] args) {
        return null;
    }
}
