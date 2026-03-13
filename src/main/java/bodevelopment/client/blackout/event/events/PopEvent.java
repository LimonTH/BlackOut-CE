package bodevelopment.client.blackout.event.events;

import net.minecraft.client.player.AbstractClientPlayer;

public class PopEvent {
    private static final PopEvent INSTANCE = new PopEvent();
    public AbstractClientPlayer player;
    public int number = 0;

    public static PopEvent get(AbstractClientPlayer player, int number) {
        INSTANCE.player = player;
        INSTANCE.number = number;
        return INSTANCE;
    }
}
