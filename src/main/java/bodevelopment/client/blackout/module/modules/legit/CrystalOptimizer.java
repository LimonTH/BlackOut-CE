package bodevelopment.client.blackout.module.modules.legit;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.mixin.accessors.AccessorServerboundInteractPacket;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

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
        if (event.packet instanceof ServerboundInteractPacket packet
                && ((AccessorServerboundInteractPacket) packet).getType().getType() == ServerboundInteractPacket.ActionType.ATTACK
                && BlackOut.mc.level.getEntity(((AccessorServerboundInteractPacket) packet).getId()) instanceof EndCrystal entity) {
            BlackOut.mc.level.removeEntity(entity.getId(), Entity.RemovalReason.KILLED);
        }
    }
}
