package bodevelopment.client.blackout.mixin.accessors;

import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerInteractEntityC2SPacket.class)
public interface AccessorInteractEntityC2SPacket {
    @Accessor("entityId")
    int getId();

    @Accessor("entityId")
    @Final
    @Mutable
    void setId(int id);

    @Accessor("type")
    PlayerInteractEntityC2SPacket.InteractTypeHandler getType();
}
