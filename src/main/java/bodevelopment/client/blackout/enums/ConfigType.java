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

package bodevelopment.client.blackout.enums;

import bodevelopment.client.blackout.module.AbstractModule;
import bodevelopment.client.blackout.module.ParentCategory;

import java.util.function.Predicate;

public enum ConfigType {
    Combat(module -> module.category != null && module.category.parent() == ParentCategory.COMBAT),
    Movement(module -> module.category != null && module.category.parent() == ParentCategory.MOVEMENT),
    Visual(module -> module.category != null && module.category.parent() == ParentCategory.VISUAL),
    Misc(module -> module.category != null && module.category.parent() == ParentCategory.MISC),
    Legit(module -> module.category != null && module.category.parent() == ParentCategory.LEGIT),
    Client(module -> module.category != null && module.category.parent() == ParentCategory.CLIENT),
    HUD(null),
    Binds(null);

    public final Predicate<AbstractModule> predicate;

    ConfigType(Predicate<AbstractModule> predicate) {
        this.predicate = predicate;
    }
}
