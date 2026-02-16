package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.interfaces.mixin.IVec3d;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.List;

public class MovementPrediction {
    public static Vec3d adjustMovementForCollisions(Entity entity, Vec3d movement) {
        Box box = entity.getBoundingBox();
        List<VoxelShape> list = entity.getEntityWorld().getEntityCollisions(entity, box.stretch(movement));
        Vec3d vec3d = movement.lengthSquared() == 0.0 ? movement : Entity.adjustMovementForCollisions(entity, movement, box, entity.getEntityWorld(), list);
        boolean bl = movement.x != vec3d.x;
        boolean bl2 = movement.y != vec3d.y;
        boolean bl3 = movement.z != vec3d.z;
        boolean bl4 = entity.isOnGround() || bl2 && movement.y < 0.0;
        if (entity.getStepHeight() > 0.0F && bl4 && (bl || bl3)) {
            Vec3d vec3d2 = Entity.adjustMovementForCollisions(
                    entity, new Vec3d(movement.x, entity.getStepHeight(), movement.z), box, entity.getEntityWorld(), list
            );
            Vec3d vec3d3 = Entity.adjustMovementForCollisions(
                    entity, new Vec3d(0.0, entity.getStepHeight(), 0.0), box.stretch(movement.x, 0.0, movement.z), entity.getEntityWorld(), list
            );
            if (vec3d3.y < entity.getStepHeight()) {
                Vec3d vec3d4 = Entity.adjustMovementForCollisions(
                                entity, new Vec3d(movement.x, 0.0, movement.z), box.offset(vec3d3), entity.getEntityWorld(), list
                        )
                        .add(vec3d3);
                if (vec3d4.horizontalLengthSquared() > vec3d2.horizontalLengthSquared()) {
                    vec3d2 = vec3d4;
                }
            }

            if (vec3d2.horizontalLengthSquared() > vec3d.horizontalLengthSquared()) {
                return vec3d2.add(
                        Entity.adjustMovementForCollisions(
                                entity, new Vec3d(0.0, -vec3d2.y + movement.y, 0.0), box.offset(vec3d2), entity.getEntityWorld(), list
                        )
                );
            }
        }

        return vec3d;
    }

    public static Vec3d predict(PlayerEntity player) {
        Vec3d movement = new Vec3d(player.getVelocity().x, player.getVelocity().y, player.getVelocity().z);
        collide(movement, player);
        return player.getPos().add(movement);
    }

    public static void collide(Vec3d movement, PlayerEntity player) {
        set(movement, player.adjustMovementForSneaking(movement, MovementType.SELF));
        set(movement, adjustMovementForCollisions(player, movement));
    }

    private static void set(Vec3d vec, Vec3d to) {
        ((IVec3d) vec).blackout_Client$set(to.x, to.y, to.z);
    }

    public static double approximateYVelocity(double deltaY, int tickDelta, int iterations) {
        double min = -5.0;
        double max = 5.0;
        double[] array = new double[2];

        for (int i = 0; i < iterations; i++) {
            double average = (min + max) / 2.0;
            simulate(average, tickDelta, array);
            if (array[0] > deltaY) {
                max = average;
            } else {
                min = average;
            }
        }

        return array[1];
    }

    private static void simulate(double vel, int tickDelta, double[] array) {
        double y = 0.0;

        for (int tick = 0; tick < tickDelta; tick++) {
            y += vel;
            if (tick < tickDelta - 1) {
                vel = (vel - 0.08) * 0.98;
            }
        }

        array[0] = y;
        array[1] = vel;
    }
}
