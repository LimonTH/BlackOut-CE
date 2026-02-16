package bodevelopment.client.blackout.mixin.accessors;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerMoveC2SPacket.class)
public interface AccessorPlayerMoveC2SPacket {
    @Accessor("yaw")
    @Mutable
    void setYaw(float yaw);

    @Accessor("pitch")
    @Mutable
    void setPitch(float pitch);

    @Accessor("onGround")
    @Mutable
    void setOnGround(boolean isOnGround);
}
