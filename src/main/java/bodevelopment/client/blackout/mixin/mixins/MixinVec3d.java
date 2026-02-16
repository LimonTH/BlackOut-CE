package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IVec3d;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Vec3d.class)
public class MixinVec3d implements IVec3d {
    @Mutable
    @Shadow
    @Final
    public double x;
    @Mutable
    @Shadow
    @Final
    public double y;
    @Mutable
    @Shadow
    @Final
    public double z;

    @Override
    public void blackout_Client$set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void blackout_Client$setXZ(double x, double z) {
        this.x = x;
        this.z = z;
    }

    @Override
    public void blackout_Client$setX(double x) {
        this.x = x;
    }

    @Override
    public void blackout_Client$setY(double y) {
        this.y = y;
    }

    @Override
    public void blackout_Client$setZ(double z) {
        this.z = z;
    }
}
