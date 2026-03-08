package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

public class AutoRespawn extends Module {
    public AutoRespawn() {
        super("AutoRespawn", "Automatically respawns after dying.", SubCategory.MISC, true);
    }

    @Event
    public void onReceive(PacketEvent.Receive.Pre event) {
        if (BlackOut.mc.player == null) return;
        if (event.packet instanceof EntityStatusS2CPacket packet) {
            if (packet.getStatus() == 3 && packet.getEntity(BlackOut.mc.world) == BlackOut.mc.player) {
                BlackOut.mc.player.requestRespawn();
                if (BlackOut.mc.currentScreen != null) {
                    BlackOut.mc.setScreen(null);
                }
            }
        }
    }
}
