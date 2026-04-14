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
