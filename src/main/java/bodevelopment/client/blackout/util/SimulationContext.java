package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.interfaces.functional.DoubleConsumer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.List;
import java.util.function.Consumer;

public class SimulationContext {
    public final Entity entity;
    public final int ticks;
    public final Consumer<Box> consumer;
    public final DoubleConsumer<SimulationContext, Integer> onTick;
    public final double jumpHeight;
    public final Vec3d originalMotion;
    public final boolean originalWater;
    public final boolean originalLava;
    public Box box;
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
            Vec3d originalMotion,
            Consumer<Box> consumer,
            DoubleConsumer<SimulationContext, Integer> onTick
    ) {
        this.entity = entity;
        this.box = entity.getBoundingBox();
        this.ticks = ticks;
        this.jumpHeight = jumpHeight;
        this.originalMotion = originalMotion;
        this.originalWater = OLEPOSSUtils.inWater(this.box);
        this.originalLava = OLEPOSSUtils.inLava(this.box);
        this.consumer = consumer;
        this.onTick = onTick;
        this.motionX = originalMotion.x;
        this.motionY = originalMotion.y;
        this.motionZ = originalMotion.z;
    }

    public void move(Vec3d movement) {
        this.box = this.box.offset(movement);
    }

    public boolean isOnGround() {
        return Simulator.isOnGround(this.entity, this.box) && this.motionY < 0.0;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public void updateCollisions() {
        this.collisions = BlackOut.mc.world.getEntityCollisions(this.entity, this.box.stretch(this.motionX, this.motionY, this.motionZ));
    }

    public Vec3d collide(Vec3d motion, Box box) {
        return Entity.adjustMovementForCollisions(this.entity, motion, box, BlackOut.mc.world, this.collisions);
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
