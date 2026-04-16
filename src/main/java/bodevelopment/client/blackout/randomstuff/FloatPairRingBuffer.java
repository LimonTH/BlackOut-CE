package bodevelopment.client.blackout.randomstuff;

/**
 * Zero-allocation ring buffer for pairs of floats.
 * Uses power-of-2 capacity for O(1) index wrapping via bitwise AND.
 */
public final class FloatPairRingBuffer {
    private static final int CAPACITY = 32; // >= 20, power of 2
    private static final int MASK = CAPACITY - 1;

    private final float[] a = new float[CAPACITY];
    private final float[] b = new float[CAPACITY];
    private int head = 0;
    private int size = 0;

    /** Insert a new pair at the front (index 0). O(1), no allocation. */
    public void addFirst(float av, float bv) {
        head = (head - 1) & MASK;
        this.a[head] = av;
        this.b[head] = bv;
        if (this.size < CAPACITY) this.size++;
    }

    public float getA(int index) {
        return this.a[(this.head + index) & MASK];
    }

    public float getB(int index) {
        return this.b[(this.head + index) & MASK];
    }

    public int size() {
        return this.size;
    }

    public void clear() {
        this.head = 0;
        this.size = 0;
    }
}
