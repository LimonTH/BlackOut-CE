package bodevelopment.client.blackout.util.pool;

import bodevelopment.client.blackout.interfaces.mixin.IAABB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Ring-buffer pool for {@link AABB} instances.
 * <p>
 * Uses the {@link IAABB} mixin to mutate returned instances in-place,
 * eliminating allocation in hot paths.
 * <p>
 * Thread-safety: not required; intended for single-thread (render/tick) usage.
 */
public class AABBPool {
    private final AABB[] pool;
    private int cursor;

    /**
     * @param size Number of pre-allocated AABB instances. Must be a power of 2.
     */
    public AABBPool(int size) {
        this.pool = new AABB[size];
        for (int i = 0; i < size; i++) {
            this.pool[i] = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }
        this.cursor = 0;
    }

    /**
     * Returns a pooled {@link AABB} with the given bounds.
     */
    public AABB get(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        AABB box = next();
        ((IAABB) box).blackout_Client$set(minX, minY, minZ, maxX, maxY, maxZ);
        return box;
    }

    /**
     * Returns a pooled {@link AABB} copied from another.
     */
    public AABB get(AABB other) {
        AABB box = next();
        ((IAABB) box).blackout_Client$set(other.minX, other.minY, other.minZ, other.maxX, other.maxY, other.maxZ);
        return box;
    }

    /**
     * Returns a pooled AABB expanded by the given amount in all directions.
     */
    public AABB inflate(AABB source, double amount) {
        AABB box = next();
        ((IAABB) box).blackout_Client$set(
                source.minX - amount, source.minY - amount, source.minZ - amount,
                source.maxX + amount, source.maxY + amount, source.maxZ + amount
        );
        return box;
    }

    /**
     * Returns a pooled AABB from a BlockPos (full 1x1x1 cube centered at pos).
     */
    public AABB fromBlock(int x, int y, int z) {
        AABB box = next();
        ((IAABB) box).blackout_Client$set(x, y, z, x + 1.0, y + 1.0, z + 1.0);
        return box;
    }

    /**
     * Returns the next pooled instance without resetting values.
     */
    public AABB next() {
        AABB box = this.pool[this.cursor];
        this.cursor = (this.cursor + 1) & (this.pool.length - 1);
        return box;
    }

    /** Resets cursor to the beginning. */
    public void reset() {
        this.cursor = 0;
    }
}
