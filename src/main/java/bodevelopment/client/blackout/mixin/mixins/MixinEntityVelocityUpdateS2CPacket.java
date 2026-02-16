package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IEntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityVelocityUpdateS2CPacket.class)
public class MixinEntityVelocityUpdateS2CPacket implements IEntityVelocityUpdateS2CPacket {
    @Mutable
    @Shadow
    @Final
    private int velocityX;
    @Mutable
    @Shadow
    @Final
    private int velocityY;
    @Mutable
    @Shadow
    @Final
    private int velocityZ;

    @Override
    public void blackout_Client$setX(int x) {
        this.velocityX = x;
    }

    @Override
    public void blackout_Client$setY(int y) {
        this.velocityY = y;
    }

    @Override
    public void blackout_Client$setZ(int z) {
        this.velocityZ = z;
    }
}
