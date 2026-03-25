package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EntityUtils {
    public static BlockPos roundedPos() {
        return roundedPos(BlackOut.mc.player);
    }

    public static BlockPos roundedPos(Entity entity) {
        return new BlockPos(entity.getBlockX(), (int) Math.round(entity.getY()), entity.getBlockZ());
    }

    public static Vec3 getLerpedPos(Entity entity, double tickDelta) {
        double x = Mth.lerp(tickDelta, entity.xo, entity.getX());
        double y = Mth.lerp(tickDelta, entity.yo, entity.getY());
        double z = Mth.lerp(tickDelta, entity.zo, entity.getZ());
        return new Vec3(x, y, z);
    }

    public static AABB getLerpedBox(Entity entity, double tickDelta) {
        double x = Mth.lerp(tickDelta, entity.xo, entity.getX());
        double y = Mth.lerp(tickDelta, entity.yo, entity.getY());
        double z = Mth.lerp(tickDelta, entity.zo, entity.getZ());
        double halfX = entity.getBoundingBox().getXsize() / 2.0;
        double halfZ = entity.getBoundingBox().getZsize() / 2.0;
        return new AABB(x - halfX, y, z - halfZ, x + halfX, y + entity.getBoundingBox().getYsize(), z + halfZ);
    }

    public static boolean intersects(AABB box, Predicate<Entity> predicate) {
        return intersects(box, predicate, null);
    }

    public static List<Entity> getEntities(AABB box, Predicate<Entity> predicate) {
        List<Entity> list = new ArrayList<>();
        if (BlackOut.mc.level == null) return list;

        try {
            BlackOut.mc.level.getEntities().get(box, entity -> {
                if (entity != null && predicate.test(entity) && !Managers.ENTITY.isDead(entity.getId())) {
                    list.add(entity);
                }
            });
        } catch (Exception ignored) {
        }
        return list;
    }

    public static boolean intersects(AABB box, Predicate<Entity> predicate, Map<Entity, AABB> hitboxes) {
        if (BlackOut.mc.level == null) return false;

        boolean[] found = {false};
        try {
            BlackOut.mc.level.getEntities().get(box, entity -> {
                if (found[0] || entity == null) return;

                if (predicate.test(entity) && !Managers.ENTITY.isDead(entity.getId())) {
                    AABB entityBox = getBox(entity, hitboxes);
                    if (entityBox != null && entityBox.intersects(box)) {
                        found[0] = true;
                    }
                }
            });
        } catch (Exception e) {
            return false;
        }

        return found[0];
    }

    private static AABB getBox(Entity entity, Map<Entity, AABB> map) {
        return map != null && map.containsKey(entity) ? map.get(entity) : entity.getBoundingBox();
    }

    public static boolean intersectsWithSpawningItem(BlockPos pos) {
        return Managers.ENTITY.containsItem(pos) || Managers.ENTITY.containsItem(pos.above());
    }
}