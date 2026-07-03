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

package bodevelopment.client.blackout.module;

import java.util.ArrayList;
import java.util.List;

public record ParentCategory(String name) implements Category {
    public static List<ParentCategory> categories = new ArrayList<>();

    public static ParentCategory COMBAT = new ParentCategory("Combat");
    public static ParentCategory MOVEMENT = new ParentCategory("Movement");
    public static ParentCategory VISUAL = new ParentCategory("Visual");
    public static ParentCategory MISC = new ParentCategory("Misc");
    public static ParentCategory LEGIT = new ParentCategory("Legit");
    public static ParentCategory CLIENT = new ParentCategory("Client");

    public ParentCategory {
        categories.add(this);
    }

    public static ParentCategory of(String name) {
        return categories.stream()
                .filter(c -> c.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> new ParentCategory(name));
    }
}
