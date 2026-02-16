package bodevelopment.client.blackout.event.events;

import bodevelopment.client.blackout.event.Cancellable;
import net.minecraft.entity.Entity;

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
