package bodevelopment.client.blackout.mixin.accessors;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundMovePlayerPacket.class)
public interface AccessorPlayerMoveC2SPacket {
    @Accessor("yRot")
    @Mutable
    void setYaw(float yaw);

    @Accessor("xRot")
    @Mutable
    void setPitch(float pitch);

    @Accessor("onGround")
    @Mutable
    void setOnGround(boolean isOnGround);
}
