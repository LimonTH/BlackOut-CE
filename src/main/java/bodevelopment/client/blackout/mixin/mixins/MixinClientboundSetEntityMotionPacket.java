package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientboundSetEntityMotionPacket.class)
public class MixinClientboundSetEntityMotionPacket implements IClientboundSetEntityMotionPacket {
    @Mutable
    @Shadow
    @Final
    private int xa;
    @Mutable
    @Shadow
    @Final
    private int ya;
    @Mutable
    @Shadow
    @Final
    private int za;

    @Override
    public void blackout_Client$setX(int x) {
        this.xa = x;
    }

    @Override
    public void blackout_Client$setY(int y) {
        this.ya = y;
    }

    @Override
    public void blackout_Client$setZ(int z) {
        this.za = z;
    }
}
