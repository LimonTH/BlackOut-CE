package bodevelopment.client.blackout.module.modules.legit;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.mixin.accessors.AccessorInteractEntityC2SPacket;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;

public class CrystalOptimizer extends Module {
    private static CrystalOptimizer INSTANCE;

    public CrystalOptimizer() {
        super("Crystal Optimizer", "Improves combat fluidness by instantly removing crystal entities from the client-side world upon attack.", SubCategory.LEGIT, true);
        INSTANCE = this;
    }

    public static CrystalOptimizer getInstance() {
        return INSTANCE;
    }

    @Event
    public void onSent(PacketEvent.Sent event) {
        if (event.packet instanceof PlayerInteractEntityC2SPacket packet
                && ((AccessorInteractEntityC2SPacket) packet).getType().getType() == PlayerInteractEntityC2SPacket.InteractType.ATTACK
                && BlackOut.mc.world.getEntityById(((AccessorInteractEntityC2SPacket) packet).getId()) instanceof EndCrystalEntity entity) {
            BlackOut.mc.world.removeEntity(entity.getId(), Entity.RemovalReason.KILLED);
        }
    }
}
