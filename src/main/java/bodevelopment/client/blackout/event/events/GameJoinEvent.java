package bodevelopment.client.blackout.event.events;

import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;

public class GameJoinEvent {
    private static final GameJoinEvent INSTANCE = new GameJoinEvent();
    public GameJoinS2CPacket packet = null;

    public static GameJoinEvent get(GameJoinS2CPacket packet) {
        INSTANCE.packet = packet;
        return INSTANCE;
    }
}
