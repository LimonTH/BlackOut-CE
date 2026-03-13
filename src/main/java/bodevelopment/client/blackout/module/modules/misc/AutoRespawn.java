package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;

public class AutoRespawn extends Module {
    public AutoRespawn() {
        super("AutoRespawn", "Automatically respawns after dying.", SubCategory.MISC, true);
    }

    @Event
    public void onReceive(PacketEvent.Receive.Pre event) {
        if (BlackOut.mc.player == null) return;
        if (event.packet instanceof ClientboundEntityEventPacket packet) {
            if (packet.getEventId() == 3 && packet.getEntity(BlackOut.mc.level) == BlackOut.mc.player) {
                BlackOut.mc.player.respawn();
                if (BlackOut.mc.screen != null) {
                    BlackOut.mc.setScreen(null);
                }
            }
        }
    }
}
