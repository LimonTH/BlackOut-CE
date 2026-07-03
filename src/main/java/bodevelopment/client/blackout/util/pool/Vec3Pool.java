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

package bodevelopment.client.blackout.util.pool;

import bodevelopment.client.blackout.interfaces.mixin.IVec3;
import net.minecraft.world.phys.Vec3;

/**
 * Ring-buffer pool for {@link Vec3} instances.
 * <p>
 * Uses the {@link IVec3} mixin to mutate returned instances in-place,
 * eliminating allocation in hot paths.
 * <p>
 * Thread-safety: not required; intended for single-thread (render/tick) usage.
 */
public class Vec3Pool {
    private final Vec3[] pool;
    private int cursor;

    /**
     * @param size Number of pre-allocated Vec3 instances. Must be a power of 2 for
     *             optimal masking, but any positive int works.
     */
    public Vec3Pool(int size) {
        this.pool = new Vec3[size];
        for (int i = 0; i < size; i++) {
            this.pool[i] = new Vec3(0.0, 0.0, 0.0);
        }
        this.cursor = 0;
    }

    /**
     * Returns a pooled {@link Vec3} set to (x, y, z).
     * <b>Important:</b> The returned instance will be overwritten on the next
     * {@code get()} call when the ring wraps around. Do not store references.
     */
    public Vec3 get(double x, double y, double z) {
        Vec3 v = next();
        ((IVec3) v).blackout_Client$set(x, y, z);
        return v;
    }

    /**
     * Returns a pooled {@link Vec3} copied from an existing one.
     */
    public Vec3 get(Vec3 other) {
        Vec3 v = next();
        ((IVec3) v).blackout_Client$set(other.x, other.y, other.z);
        return v;
    }

    /**
     * Returns the next pooled instance without resetting its values.
     * Useful when you intend to set fields manually via {@link IVec3}.
     */
    public Vec3 next() {
        Vec3 v = this.pool[this.cursor];
        this.cursor = (this.cursor + 1) & (this.pool.length - 1);
        return v;
    }

    /** Resets cursor to the beginning (allows re-use of the entire pool). */
    public void reset() {
        this.cursor = 0;
    }
}
