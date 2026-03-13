package bodevelopment.client.blackout.interfaces.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

public interface IRaycastContext {
    void blackout_Client$set(Vec3 startPos, Vec3 endPos, ClipContext.Block shapeType, ClipContext.Fluid fluidHandling, Entity entity);

    void blackout_Client$set(Vec3 startPos, Vec3 endPos);

    void blackout_Client$set(ClipContext.Block shapeType, ClipContext.Fluid fluidHandling, Entity entity);

    void blackout_Client$setStart(Vec3 startPos);

    void blackout_Client$setEnd(Vec3 endPos);
}
