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

package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.PublicAPI;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.interfaces.mixin.IAABB;
import bodevelopment.client.blackout.interfaces.mixin.IVec3;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.util.pool.AABBPool;
import bodevelopment.client.blackout.util.pool.Vec3Pool;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Centralized position data provider for all combat modules.
 * <p>
 * Pre-computes entity positions, bounding boxes, and distances once per tick,
 * storing them in pooled mutable arrays. Modules query this manager instead of
 * creating their own {@link Vec3} / {@link AABB} instances, eliminating
 * allocations in hot paths (onTick, onRender).
 * <p>
 * <b>Zero-allocation query API</b> — all returned {@link Vec3} and {@link AABB}
 * references come from ring-buffer pools and will be overwritten. Do NOT store
 * references across frames.
 */
@PublicAPI
public class PositionManager extends Manager {
    /** Maximum entities tracked per tick. */
    private static final int MAX_ENTITIES = 2048;
    /** Pool sizes — must be powers of 2. */
    private static final int POOL_SIZE = 8192;

    private final Entity[] entities = new Entity[MAX_ENTITIES];
    private final AABB[] boxes = new AABB[MAX_ENTITIES];
    private final double[] distances = new double[MAX_ENTITIES];
    /** Raw entity coordinates (no allocation). */
    private final double[] entityX = new double[MAX_ENTITIES];
    private final double[] entityY = new double[MAX_ENTITIES];
    private final double[] entityZ = new double[MAX_ENTITIES];
    /** Previous-tick coordinates for motion calculation. */
    private final double[] prevX = new double[MAX_ENTITIES];
    private final double[] prevY = new double[MAX_ENTITIES];
    private final double[] prevZ = new double[MAX_ENTITIES];
    private int count;

    private final Vec3Pool vecPool = new Vec3Pool(POOL_SIZE);
    private final AABBPool boxPool = new AABBPool(POOL_SIZE);

    private static PositionManager INSTANCE;
    public static PositionManager getInstance() { return INSTANCE; }

    public PositionManager() {
        INSTANCE = this;
        for (int i = 0; i < MAX_ENTITIES; i++) {
            this.boxes[i] = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public void init() {
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
    }

    @Event
    public void onPreTick(TickEvent.Pre event) {
        refresh();
    }

    /** Called manually from render or tick to refresh the cache. */
    public void refresh() {
        if (BlackOut.mc.level == null || BlackOut.mc.player == null) {
            this.count = 0;
            return;
        }

        // Shift current coordinates to prev for motion tracking
        System.arraycopy(this.entityX, 0, this.prevX, 0, this.count);
        System.arraycopy(this.entityY, 0, this.prevY, 0, this.count);
        System.arraycopy(this.entityZ, 0, this.prevZ, 0, this.count);

        this.count = 0;
        Vec3 playerPos = BlackOut.mc.player.position();

        for (Entity entity : BlackOut.mc.level.entitiesForRendering()) {
            if (this.count >= MAX_ENTITIES) break;
            if (entity == BlackOut.mc.player) continue;

            this.entities[this.count] = entity;
            AABB entityBox = entity.getBoundingBox();
            IAABB mutable = (IAABB) this.boxes[this.count];
            mutable.blackout_Client$set(
                    entityBox.minX, entityBox.minY, entityBox.minZ,
                    entityBox.maxX, entityBox.maxY, entityBox.maxZ
            );
            this.entityX[this.count] = entity.getX();
            this.entityY[this.count] = entity.getY();
            this.entityZ[this.count] = entity.getZ();
            this.distances[this.count] = playerPos.distanceTo(entity.position());
            this.count++;
        }
    }

    /** Number of valid entities in the current cache. */
    public int entityCount() { return this.count; }

    /** Entity at index {@code i}. */
    public Entity entity(int i) { return this.entities[i]; }

    /**
     * Pooled AABB for entity at index {@code i}.
     * <b>Will be overwritten next tick.</b>
     */
    public AABB box(int i) { return this.boxes[i]; }

    /** Pre-computed distance from local player to entity at index {@code i}. */
    public double distance(int i) { return this.distances[i]; }

    /** Raw entity X coordinate at index {@code i}. */
    public double entityX(int i) { return this.entityX[i]; }
    /** Raw entity Y coordinate at index {@code i}. */
    public double entityY(int i) { return this.entityY[i]; }
    /** Raw entity Z coordinate at index {@code i}. */
    public double entityZ(int i) { return this.entityZ[i]; }

    /** Motion (delta from previous tick) X at index {@code i}. */
    public double motionX(int i) { return this.entityX[i] - this.prevX[i]; }
    /** Motion (delta from previous tick) Y at index {@code i}. */
    public double motionY(int i) { return this.entityY[i] - this.prevY[i]; }
    /** Motion (delta from previous tick) Z at index {@code i}. */
    public double motionZ(int i) { return this.entityZ[i] - this.prevZ[i]; }

    /**
     * Iterates over all cached entities, calling {@code consumer} for each.
     * The consumer receives (entity, pooledAABB, distance).
     */
    public void forEach(EntityBoxConsumer consumer) {
        for (int i = 0; i < this.count; i++) {
            consumer.accept(this.entities[i], this.boxes[i], this.distances[i]);
        }
    }

    /**
     * Iterates over filtered entities. Only entities matching {@code filter}
     * are passed to {@code consumer}.
     */
    public void forEachFiltered(Predicate<Entity> filter, EntityBoxConsumer consumer) {
        for (int i = 0; i < this.count; i++) {
            Entity e = this.entities[i];
            if (filter.test(e)) {
                consumer.accept(e, this.boxes[i], this.distances[i]);
            }
        }
    }

    /**
     * Finds the entity closest to the local player that matches {@code filter}.
     * Returns index or -1 if none found.
     */
    public int findClosest(Predicate<Entity> filter) {
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < this.count; i++) {
            if (filter.test(this.entities[i])) {
                double d = this.distances[i];
                if (d < bestDist) {
                    bestDist = d;
                    best = i;
                }
            }
        }
        return best;
    }

    /**
     * Finds the index of a specific entity in the cache.
     * @return index or -1 if not found.
     */
    public int indexOf(Entity entity) {
        for (int i = 0; i < this.count; i++) {
            if (this.entities[i] == entity) return i;
        }
        return -1;
    }

    /**
     * Returns a pooled AABB for the given entity from cache, or a fresh copy
     * if not cached (fallback).
     */
    public AABB getBox(Entity entity) {
        int idx = indexOf(entity);
        if (idx >= 0) return this.boxes[idx];
        return this.boxPool.get(entity.getBoundingBox());
    }

    /**
     * Returns a pooled Vec3 of the entity's position from the entity directly
     * (position() creates a new Vec3 — we pool it).
     */
    public Vec3 getPosition(Entity entity) {
        return this.vecPool.get(entity.getX(), entity.getY(), entity.getZ());
    }

    public Vec3Pool vec3() { return this.vecPool; }
    public AABBPool aabb() { return this.boxPool; }

    @FunctionalInterface
    public interface EntityBoxConsumer {
        void accept(Entity entity, AABB box, double distance);
    }
}
