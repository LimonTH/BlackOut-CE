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

package bodevelopment.client.blackout.event.events;

import bodevelopment.client.blackout.event.Cancellable;
import net.minecraft.world.entity.Entity;

public class EntityAddEvent {
    public static class Post {
        private static final Post INSTANCE = new Post();
        public int id = 0;
        public Entity entity = null;

        public static Post get(int id, Entity entity) {
            INSTANCE.id = id;
            INSTANCE.entity = entity;
            return INSTANCE;
        }
    }

    public static class Pre extends Cancellable {
        private static final Pre INSTANCE = new Pre();
        public int id = 0;
        public Entity entity = null;

        public static Pre get(int id, Entity entity) {
            INSTANCE.id = id;
            INSTANCE.entity = entity;
            INSTANCE.setCancelled(false);
            return INSTANCE;
        }
    }
}
