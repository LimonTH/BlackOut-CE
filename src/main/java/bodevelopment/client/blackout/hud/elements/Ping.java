package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.TextElement;
import net.minecraft.client.multiplayer.PlayerInfo;

public class Ping extends TextElement {

    public Ping() {
        super("Ping", "Displays the round-trip latency between the client and the server in milliseconds.");
    }

    @Override
    public void render() {
        this.drawElement(this.stack, "Ping:", this.getPing());
    }

    private String getPing() {
        if (BlackOut.mc.player == null || BlackOut.mc.getConnection() == null) {
            return "-";
        }
        PlayerInfo entry = BlackOut.mc.getConnection().getPlayerInfo(BlackOut.mc.player.getGameProfile().getName());
        return entry == null ? "-" : String.valueOf(entry.getLatency());
    }
}
