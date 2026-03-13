package bodevelopment.client.blackout.mixin.accessors;

import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundEntityEventPacket.class)
public interface AccessorEntityStatusC2SPacket {
    @Accessor("entityId")
    int getId();
}
