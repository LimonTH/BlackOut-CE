package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.interfaces.mixin.IVec3;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MovementPrediction {
    public static Vec3 adjustMovementForCollisions(Entity entity, Vec3 movement) {
        AABB box = entity.getBoundingBox();
        List<VoxelShape> list = entity.getCommandSenderWorld().getEntityCollisions(entity, box.expandTowards(movement));
        Vec3 vec3d = movement.lengthSqr() == 0.0 ? movement : Entity.collideBoundingBox(entity, movement, box, entity.getCommandSenderWorld(), list);
        boolean bl = movement.x != vec3d.x;
        boolean bl2 = movement.y != vec3d.y;
        boolean bl3 = movement.z != vec3d.z;
        boolean bl4 = entity.onGround() || bl2 && movement.y < 0.0;
        if (entity.maxUpStep() > 0.0F && bl4 && (bl || bl3)) {
            Vec3 vec3d2 = Entity.collideBoundingBox(
                    entity, new Vec3(movement.x, entity.maxUpStep(), movement.z), box, entity.getCommandSenderWorld(), list
            );
            Vec3 vec3d3 = Entity.collideBoundingBox(
                    entity, new Vec3(0.0, entity.maxUpStep(), 0.0), box.expandTowards(movement.x, 0.0, movement.z), entity.getCommandSenderWorld(), list
            );
            if (vec3d3.y < entity.maxUpStep()) {
                Vec3 vec3d4 = Entity.collideBoundingBox(
                                entity, new Vec3(movement.x, 0.0, movement.z), box.move(vec3d3), entity.getCommandSenderWorld(), list
                        )
                        .add(vec3d3);
                if (vec3d4.horizontalDistanceSqr() > vec3d2.horizontalDistanceSqr()) {
                    vec3d2 = vec3d4;
                }
            }

            if (vec3d2.horizontalDistanceSqr() > vec3d.horizontalDistanceSqr()) {
                return vec3d2.add(
                        Entity.collideBoundingBox(
                                entity, new Vec3(0.0, -vec3d2.y + movement.y, 0.0), box.move(vec3d2), entity.getCommandSenderWorld(), list
                        )
                );
            }
        }

        return vec3d;
    }

    public static Vec3 predict(Player player) {
        Vec3 movement = new Vec3(player.getDeltaMovement().x, player.getDeltaMovement().y, player.getDeltaMovement().z);
        collide(movement, player);
        return player.position().add(movement);
    }

    public static void collide(Vec3 movement, Player player) {
        set(movement, player.maybeBackOffFromEdge(movement, MoverType.SELF));
        set(movement, adjustMovementForCollisions(player, movement));
    }

    private static void set(Vec3 vec, Vec3 to) {
        ((IVec3) vec).blackout_Client$set(to.x, to.y, to.z);
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
