package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IClipContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClipContext.class)
public class MixinClipContext implements IClipContext {
    @Mutable
    @Shadow
    @Final
    private ClipContext.Block block;
    @Mutable
    @Shadow
    @Final
    private ClipContext.Fluid fluid;
    @Mutable
    @Shadow
    @Final
    private CollisionContext collisionContext;
    @Mutable
    @Shadow
    @Final
    private Vec3 from;
    @Mutable
    @Shadow
    @Final
    private Vec3 to;

    @Override
    public void blackout_Client$set(Vec3 start, Vec3 end, ClipContext.Block shapeType, ClipContext.Fluid fluidHandling, Entity entity) {
        this.block = shapeType;
        this.fluid = fluidHandling;
        this.collisionContext = CollisionContext.of(entity);
        this.from = start;
        this.to = end;
    }

    @Override
    public void blackout_Client$set(Vec3 start, Vec3 end) {
        this.from = start;
        this.to = end;
    }

    @Override
    public void blackout_Client$set(ClipContext.Block shapeType, ClipContext.Fluid fluidHandling, Entity entity) {
        this.block = shapeType;
        this.fluid = fluidHandling;
        this.collisionContext = CollisionContext.of(entity);
    }

    @Override
    public void blackout_Client$setStart(Vec3 start) {
        this.from = start;
    }

    @Override
    public void blackout_Client$setEnd(Vec3 end) {
        this.to = end;
    }
}
