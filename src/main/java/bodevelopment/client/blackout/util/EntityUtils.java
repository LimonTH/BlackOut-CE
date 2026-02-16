package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.entity.EntityLookup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class EntityUtils {

    public static boolean intersects(Box box, Predicate<Entity> predicate) {
        return intersects(box, predicate, null);
    }

    public static boolean intersects(Box box, Predicate<Entity> predicate, Map<Entity, Box> hitboxes) {
        if (BlackOut.mc.world == null) return false;

        // В 1.21.1 EntityLookup — самый эффективный способ
        EntityLookup<Entity> lookup = BlackOut.mc.world.getEntityLookup();
        boolean[] found = {false};

        lookup.forEachIntersects(box, entity -> {
            // Эмуляция break: если уже нашли, пропускаем тяжелые проверки
            if (found[0]) return;

            // Базовая проверка: не мертва ли сущность и подходит ли под условие
            if (predicate.test(entity) && !Managers.ENTITY.isDead(entity.getId())) {
                Box entityBox = getBox(entity, hitboxes);
                if (entityBox.intersects(box)) {
                    found[0] = true;
                }
            }
        });

        return found[0];
    }

    /**
     * Возвращает список всех сущностей в коробке, подходящих под условие.
     * Полезно для выбора приоритетной цели в AutoCrystal.
     */
    public static List<Entity> getEntities(Box box, Predicate<Entity> predicate) {
        List<Entity> list = new ArrayList<>();
        if (BlackOut.mc.world == null) return list;

        BlackOut.mc.world.getEntityLookup().forEachIntersects(box, entity -> {
            if (predicate.test(entity) && !Managers.ENTITY.isDead(entity.getId())) {
                list.add(entity);
            }
        });
        return list;
    }

    private static Box getBox(Entity entity, Map<Entity, Box> map) {
        // Backtrack: если у нас есть записанная история хитбоксов, берем её
        return map != null && map.containsKey(entity) ? map.get(entity) : entity.getBoundingBox();
    }

    public static boolean intersectsWithSpawningItem(BlockPos pos) {
        // Проверяем наличие предметов, которые могут заблокировать установку (Burrow/Surround)
        // В 1.21.1 предметы часто имеют "плавающий" хитбокс
        return Managers.ENTITY.containsItem(pos) || Managers.ENTITY.containsItem(pos.up());
    }
}