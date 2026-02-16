package bodevelopment.client.blackout.event.events;

import net.minecraft.entity.Entity;

public class RemoveEvent {
    private static final RemoveEvent INSTANCE = new RemoveEvent();
    public Entity entity;
    public Entity.RemovalReason removalReason;

    public static RemoveEvent get(Entity entity, Entity.RemovalReason removalReason) {
        INSTANCE.entity = entity;
        INSTANCE.removalReason = removalReason;
        return INSTANCE;
    }
}
