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

package bodevelopment.client.blackout.randomstuff;

import bodevelopment.client.blackout.interfaces.functional.EpicInterface;
import bodevelopment.client.blackout.manager.Managers;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public class ExtrapolationMap {
    private final Map<Entity, AABB> boxMap = new ConcurrentHashMap<>();

    public void update(EpicInterface<Entity, Integer> extrapolation) {
        Managers.EXTRAPOLATION.extrapolateMap(this.boxMap, extrapolation);
    }

    public AABB get(Entity player) {
        AABB cached = this.boxMap.get(player);
        return cached != null ? cached : player.getBoundingBox();
    }

    public Map<Entity, AABB> getMap() {
        return this.boxMap;
    }

    public int size() {
        return this.boxMap.size();
    }

    public boolean contains(Entity player) {
        return this.boxMap.containsKey(player);
    }

    public Set<Entry<Entity, AABB>> entrySet() {
        return this.boxMap.entrySet();
    }

    public void forEach(BiConsumer<Entity, AABB> consumer) {
        this.boxMap.forEach(consumer);
    }

    public void clear() {
        this.boxMap.clear();
    }
}
