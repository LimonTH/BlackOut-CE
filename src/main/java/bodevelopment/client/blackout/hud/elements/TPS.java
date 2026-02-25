package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.TextElement;
import bodevelopment.client.blackout.manager.Managers;

public class TPS extends TextElement {
    public TPS() {
        super("TPS", "Displays the current server-side Ticks Per Second (TPS) to monitor game state synchronization.");
        this.setSize(10.0F, 10.0F);
    }

    @Override
    public void render() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            String tps = String.format("%.1f", Managers.TPS.tps);
            this.drawElement(this.stack, "TPS", tps);
        }
    }
}
