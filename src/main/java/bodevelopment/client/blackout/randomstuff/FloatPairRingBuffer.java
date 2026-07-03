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
