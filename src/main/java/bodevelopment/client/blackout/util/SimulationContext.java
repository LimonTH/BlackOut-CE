package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.interfaces.functional.DoubleConsumer;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SimulationContext {
    public final Entity entity;
    public final int ticks;
    public final Consumer<AABB> consumer;
    public final DoubleConsumer<SimulationContext, Integer> onTick;
    public final double jumpHeight;
    public final Vec3 originalMotion;
    public final boolean originalWater;
    public final boolean originalLava;
    public AABB box;
    public boolean onGround = false;
    public boolean prevOnGround = false;
    public boolean jump = false;
    public boolean inWater = false;
    public boolean inLava = false;
    public double motionX;
    public double motionY;
    public double motionZ;
    public double reverseStep = 0.0;
    public double step = 0.0;
    private List<VoxelShape> collisions;

    public SimulationContext(
            Entity entity,
            int ticks,
            double jumpHeight,
            Vec3 originalMotion,
            Consumer<AABB> consumer,
            DoubleConsumer<SimulationContext, Integer> onTick
    ) {
        this.entity = entity;
        this.box = entity.getBoundingBox();
        this.ticks = ticks;
        this.jumpHeight = jumpHeight;
        this.originalMotion = originalMotion;
        this.originalWater = BlockUtils.inWater(this.box);
        this.originalLava = BlockUtils.inLava(this.box);
        this.consumer = consumer;
        this.onTick = onTick;
        this.motionX = originalMotion.x;
        this.motionY = originalMotion.y;
        this.motionZ = originalMotion.z;
    }

    public void move(Vec3 movement) {
        this.box = this.box.move(movement);
    }

    public boolean isOnGround() {
        return Simulator.isOnGround(this.entity, this.box) && this.motionY < 0.0;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public void updateCollisions() {
        this.collisions = BlackOut.mc.level.getEntityCollisions(this.entity, this.box.expandTowards(this.motionX, this.motionY, this.motionZ));
    }

    public Vec3 collide(Vec3 motion, AABB box) {
        return Entity.collideBoundingBox(this.entity, motion, box, BlackOut.mc.level, this.collisions);
    }

    public void accept() {
        if (this.consumer != null) {
            this.consumer.accept(this.box);
        }
    }

    public boolean inFluid() {
        return this.inWater || this.inLava;
    }

    public boolean inFluidOriginal() {
        return this.originalWater || this.originalLava;
    }
}
