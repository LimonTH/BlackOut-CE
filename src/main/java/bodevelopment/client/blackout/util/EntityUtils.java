/*
 * Blackout Client (CE) - A cutting-edge, feature-rich cheat client for Minecraft.
 * A modernized continuation of the original Blackout project by OLEPOSSU & KassuK.
 * Copyright (C) 2026  LimonTH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.PublicAPI;
import bodevelopment.client.blackout.manager.Managers;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@PublicAPI
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
        return Managers.POSITION.vec3().get(x, y, z);
    }

    public static AABB getLerpedBox(Entity entity, double tickDelta) {
        double x = Mth.lerp(tickDelta, entity.xo, entity.getX());
        double y = Mth.lerp(tickDelta, entity.yo, entity.getY());
        double z = Mth.lerp(tickDelta, entity.zo, entity.getZ());
        double halfX = entity.getBoundingBox().getXsize() / 2.0;
        double halfZ = entity.getBoundingBox().getZsize() / 2.0;
        return Managers.POSITION.aabb().get(x - halfX, y, z - halfZ, x + halfX, y + entity.getBoundingBox().getYsize(), z + halfZ);
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
        if (map == null) return Managers.POSITION.getBox(entity);
        AABB cached = map.get(entity);
        return cached != null ? cached : Managers.POSITION.getBox(entity);
    }

    public static boolean intersectsWithSpawningItem(BlockPos pos) {
        return Managers.ENTITY.containsItem(pos) || Managers.ENTITY.containsItem(pos.above());
    }
}