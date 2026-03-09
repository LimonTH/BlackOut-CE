package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class EntityUtils {

    public static boolean intersects(Box box, Predicate<Entity> predicate) {
        return intersects(box, predicate, null);
    }

    public static List<Entity> getEntities(Box box, Predicate<Entity> predicate) {
        List<Entity> list = new ArrayList<>();
        if (BlackOut.mc.world == null) return list;

        try {
            BlackOut.mc.world.getEntityLookup().forEachIntersects(box, entity -> {
                if (entity != null && predicate.test(entity) && !Managers.ENTITY.isDead(entity.getId())) {
                    list.add(entity);
                }
            });
        } catch (Exception ignored) {
        }
        return list;
    }

    public static boolean intersects(Box box, Predicate<Entity> predicate, Map<Entity, Box> hitboxes) {
        if (BlackOut.mc.world == null) return false;

        boolean[] found = {false};
        try {
            BlackOut.mc.world.getEntityLookup().forEachIntersects(box, entity -> {
                if (found[0] || entity == null) return;

                if (predicate.test(entity) && !Managers.ENTITY.isDead(entity.getId())) {
                    Box entityBox = getBox(entity, hitboxes);
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

    private static Box getBox(Entity entity, Map<Entity, Box> map) {
        return map != null && map.containsKey(entity) ? map.get(entity) : entity.getBoundingBox();
    }

    public static boolean intersectsWithSpawningItem(BlockPos pos) {
        return Managers.ENTITY.containsItem(pos) || Managers.ENTITY.containsItem(pos.up());
    }
}