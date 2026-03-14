package bodevelopment.client.blackout.mixin.accessors;

import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundEntityEventPacket.class)
public interface AccessorClientboundEntityEventPacket {
    @Accessor("entityId")
    int getId();
}
