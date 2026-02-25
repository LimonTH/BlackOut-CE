package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.TextElement;
import net.minecraft.client.network.PlayerListEntry;

public class Ping extends TextElement {

    public Ping() {
        super("Ping", "Displays the round-trip latency between the client and the server in milliseconds.");
        this.setSize(10.0F, 10.0F);
    }

    @Override
    public void render() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            String ping = this.getPing();
            this.drawElement(this.stack, "Ping:", ping);
        }
    }

    private String getPing() {
        PlayerListEntry entry = BlackOut.mc.getNetworkHandler().getPlayerListEntry(BlackOut.mc.player.getGameProfile().getName());
        return entry == null ? "-" : String.valueOf(entry.getLatency());
    }
}
