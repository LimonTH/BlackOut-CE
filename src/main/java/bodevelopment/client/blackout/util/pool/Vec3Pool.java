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
