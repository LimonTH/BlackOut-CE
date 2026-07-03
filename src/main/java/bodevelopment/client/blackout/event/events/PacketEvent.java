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
import net.minecraft.network.protocol.Packet;

public class PacketEvent {
    public static class Receive {
        public static class Post extends Cancellable {
            private static final ThreadLocal<Post> INSTANCE = ThreadLocal.withInitial(Post::new);
            public Packet<?> packet = null;

            public static Post get(Packet<?> packet) {
                Post instance = INSTANCE.get();
                instance.packet = packet;
                instance.setCancelled(false);
                return instance;
            }
        }

        public static class Pre extends Cancellable {
            private static final ThreadLocal<Pre> INSTANCE = ThreadLocal.withInitial(Pre::new);
            public Packet<?> packet = null;

            public static Pre get(Packet<?> packet) {
                Pre instance = INSTANCE.get();
                instance.packet = packet;
                instance.setCancelled(false);
                return instance;
            }
        }
    }

    public static class Received {
        private static final ThreadLocal<Received> INSTANCE = ThreadLocal.withInitial(Received::new);
        public Packet<?> packet = null;

        public static Received get(Packet<?> packet) {
            Received instance = INSTANCE.get();
            instance.packet = packet;
            return instance;
        }
    }

    public static class Send extends Cancellable {
        private static final ThreadLocal<Send> INSTANCE = ThreadLocal.withInitial(Send::new);
        public Packet<?> packet = null;

        public static Send get(Packet<?> packet) {
            Send instance = INSTANCE.get();
            instance.packet = packet;
            instance.setCancelled(false);
            return instance;
        }
    }

    public static class Sent {
        private static final ThreadLocal<Sent> INSTANCE = ThreadLocal.withInitial(Sent::new);
        public Packet<?> packet = null;

        public static Sent get(Packet<?> packet) {
            Sent instance = INSTANCE.get();
            instance.packet = packet;
            return instance;
        }
    }
}
