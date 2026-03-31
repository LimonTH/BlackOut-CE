package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.util.PlayerUtils;
import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.TextElement;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.setting.Setting;

public class TPS extends TextElement {
    private final Setting<Boolean> showRegion = this.sgGeneral.booleanSetting("Region TPS", false, "Displays the estimated TPS for the chunk region around you based on entity update frequency.");

    public TPS() {
        super("TPS", "Displays the current server-side Ticks Per Second (TPS) to monitor game state synchronization.");
        this.setSize(10.0F, 10.0F);
    }

    @Override
    public void render() {
        if (PlayerUtils.isInGame()) {
            String tps = String.format("%.1f", Managers.TPS.tps);

            if (this.showRegion.get()) {
                String region = String.format("%.1f", Managers.TPS.regionTps);
                this.drawElement(this.stack, "TPS", tps + " | " + region);
            } else {
                this.drawElement(this.stack, "TPS", tps);
            }
        }
    }
}
