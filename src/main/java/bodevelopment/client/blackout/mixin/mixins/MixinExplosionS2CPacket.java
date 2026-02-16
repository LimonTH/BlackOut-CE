package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ExplosionS2CPacket.class)
public class MixinExplosionS2CPacket implements IExplosionS2CPacket {
    @Mutable
    @Shadow
    @Final
    private float playerVelocityX;
    @Mutable
    @Shadow
    @Final
    private float playerVelocityY;
    @Mutable
    @Shadow
    @Final
    private float playerVelocityZ;

    @Override
    public void blackout_Client$multiplyXZ(float multiplier) {
        this.playerVelocityX *= multiplier;
        this.playerVelocityY *= multiplier;
    }

    @Override
    public void blackout_Client$multiplyY(float multiplier) {
        this.playerVelocityZ *= multiplier;
    }
}
