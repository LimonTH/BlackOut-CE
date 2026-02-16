package bodevelopment.client.blackout.event.events;

import bodevelopment.client.blackout.event.Cancellable;
import net.minecraft.network.packet.Packet;

public class PacketEvent {
    public static class Receive {
        public static class Post extends Cancellable {
            private static final Post INSTANCE = new Post();
            public Packet<?> packet = null;

            public static Post get(Packet<?> packet) {
                INSTANCE.packet = packet;
                INSTANCE.setCancelled(false);
                return INSTANCE;
            }
        }

        public static class Pre {
            private static final Pre INSTANCE = new Pre();
            public Packet<?> packet = null;

            public static Pre get(Packet<?> packet) {
                INSTANCE.packet = packet;
                return INSTANCE;
            }
        }
    }

    public static class Received {
        private static final Received INSTANCE = new Received();
        public Packet<?> packet = null;

        public static Received get(Packet<?> packet) {
            INSTANCE.packet = packet;
            return INSTANCE;
        }
    }

    public static class Send extends Cancellable {
        private static final Send INSTANCE = new Send();
        public Packet<?> packet = null;

        public static Send get(Packet<?> packet) {
            INSTANCE.packet = packet;
            INSTANCE.setCancelled(false);
            return INSTANCE;
        }
    }

    public static class Sent {
        private static final Sent INSTANCE = new Sent();
        public Packet<?> packet = null;

        public static Sent get(Packet<?> packet) {
            INSTANCE.packet = packet;
            return INSTANCE;
        }
    }
}
