package bodevelopment.client.blackout.mixin.accessors;

import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityStatusS2CPacket.class)
public interface AccessorEntityStatusC2SPacket {
    @Accessor("entityId")
    int getId();
}
