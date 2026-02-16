package bodevelopment.client.blackout.interfaces.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public interface IRaycastContext {
    void blackout_Client$set(Vec3d startPos, Vec3d endPos, RaycastContext.ShapeType shapeType, RaycastContext.FluidHandling fluidHandling, Entity entity);

    void blackout_Client$set(Vec3d startPos, Vec3d endPos);

    void blackout_Client$set(RaycastContext.ShapeType shapeType, RaycastContext.FluidHandling fluidHandling, Entity entity);

    void blackout_Client$setStart(Vec3d startPos);

    void blackout_Client$setEnd(Vec3d endPos);
}
