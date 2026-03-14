package bodevelopment.client.blackout.mixin.accessors;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundInteractPacket.class)
public interface AccessorServerboundInteractPacket {
    @Accessor("entityId")
    int getId();

    @Accessor("entityId")
    @Final
    @Mutable
    void setId(int id);

    @Accessor("action")
    ServerboundInteractPacket.Action getType();
}
