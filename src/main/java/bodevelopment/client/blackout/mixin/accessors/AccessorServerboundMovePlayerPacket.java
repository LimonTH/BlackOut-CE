package bodevelopment.client.blackout.mixin.accessors;

import bodevelopment.client.blackout.annotations.Internal;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundMovePlayerPacket.class)
@Internal
public interface AccessorServerboundMovePlayerPacket {
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
