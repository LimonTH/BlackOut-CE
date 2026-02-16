package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IRaycastContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RaycastContext.class)
public class MixinRaycastContext implements IRaycastContext {
    @Mutable
    @Shadow
    @Final
    private RaycastContext.ShapeType shapeType;
    @Mutable
    @Shadow
    @Final
    private RaycastContext.FluidHandling fluid;
    @Mutable
    @Shadow
    @Final
    private ShapeContext shapeContext;
    @Mutable
    @Shadow
    @Final
    private Vec3d start;
    @Mutable
    @Shadow
    @Final
    private Vec3d end;

    @Override
    public void blackout_Client$set(Vec3d start, Vec3d end, RaycastContext.ShapeType shapeType, RaycastContext.FluidHandling fluidHandling, Entity entity) {
        this.shapeType = shapeType;
        this.fluid = fluidHandling;
        this.shapeContext = ShapeContext.of(entity);
        this.start = start;
        this.end = end;
    }

    @Override
    public void blackout_Client$set(Vec3d start, Vec3d end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public void blackout_Client$set(RaycastContext.ShapeType shapeType, RaycastContext.FluidHandling fluidHandling, Entity entity) {
        this.shapeType = shapeType;
        this.fluid = fluidHandling;
        this.shapeContext = ShapeContext.of(entity);
    }

    @Override
    public void blackout_Client$setStart(Vec3d start) {
        this.start = start;
    }

    @Override
    public void blackout_Client$setEnd(Vec3d end) {
        this.end = end;
    }
}
