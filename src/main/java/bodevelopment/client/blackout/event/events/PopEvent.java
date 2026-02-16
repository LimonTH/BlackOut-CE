package bodevelopment.client.blackout.event.events;

import net.minecraft.client.network.AbstractClientPlayerEntity;

public class PopEvent {
    private static final PopEvent INSTANCE = new PopEvent();
    public AbstractClientPlayerEntity player;
    public int number = 0;

    public static PopEvent get(AbstractClientPlayerEntity player, int number) {
        INSTANCE.player = player;
        INSTANCE.number = number;
        return INSTANCE;
    }
}
