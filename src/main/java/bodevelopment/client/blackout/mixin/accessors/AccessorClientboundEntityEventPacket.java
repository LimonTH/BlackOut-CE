package bodevelopment.client.blackout.mixin.accessors;

import bodevelopment.client.blackout.annotations.Internal;

import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundEntityEventPacket.class)
@Internal
public interface AccessorClientboundEntityEventPacket {
    @Accessor("entityId")
    int getId();
}
