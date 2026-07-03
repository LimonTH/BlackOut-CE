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

public record SubCategory(String name, ParentCategory parent) implements Category {
    public static List<SubCategory> categories = new ArrayList<>();

    public static SubCategory DEFENSIVE = new SubCategory("Defensive", ParentCategory.COMBAT);
    public static SubCategory OFFENSIVE = new SubCategory("Offensive", ParentCategory.COMBAT);
    public static SubCategory MISC_COMBAT = new SubCategory("Misc", ParentCategory.COMBAT);

    public static SubCategory MOVEMENT = new SubCategory("Movement", ParentCategory.MOVEMENT);

    public static SubCategory ENTITIES = new SubCategory("Entities", ParentCategory.VISUAL);
    public static SubCategory WORLD = new SubCategory("World", ParentCategory.VISUAL);
    public static SubCategory MISC_VISUAL = new SubCategory("Misc", ParentCategory.VISUAL);

    public static SubCategory MISC = new SubCategory("Misc", ParentCategory.MISC);
    public static SubCategory MEMES = new SubCategory("Memes", ParentCategory.MISC);

    public static SubCategory LEGIT = new SubCategory("Legit", ParentCategory.LEGIT);

    public static SubCategory CLIENT = new SubCategory("Client", ParentCategory.CLIENT);
    public static SubCategory SETTINGS = new SubCategory("Settings", ParentCategory.CLIENT);

    public SubCategory {
        categories.add(this);
    }
}
