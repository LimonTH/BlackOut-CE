package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;

public class PortalGodMode extends Module {
    public PortalGodMode() {
        super("Portal God Mode", "Grants temporary invulnerability by canceling teleport confirmation packets while standing inside a portal.", SubCategory.MISC, true);
    }

    @Event
    public void onSend(PacketEvent.Send event) {
        if (event.packet instanceof TeleportConfirmC2SPacket) {
            event.setCancelled(true);
        }
    }
}
