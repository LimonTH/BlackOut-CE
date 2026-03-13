package bodevelopment.client.blackout.event.events;

import net.minecraft.network.protocol.game.ClientboundLoginPacket;

public class GameJoinEvent {
    private static final GameJoinEvent INSTANCE = new GameJoinEvent();
    public ClientboundLoginPacket packet = null;

    public static GameJoinEvent get(ClientboundLoginPacket packet) {
        INSTANCE.packet = packet;
        return INSTANCE;
    }
}
