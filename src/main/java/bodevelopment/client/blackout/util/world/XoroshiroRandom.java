package bodevelopment.client.blackout.util.world;

import bodevelopment.client.blackout.annotations.PublicAPI;

@PublicAPI
public class XoroshiroRandom {
    private static final long GOLDEN_RATIO_64 = -7046029254386353131L;
    private static final long SILVER_RATIO_64 = 7640891576956012809L;

    private long seedLo;
    private long seedHi;

    public XoroshiroRandom(long seed) {
        long lo = seed ^ SILVER_RATIO_64;
        long hi = lo + GOLDEN_RATIO_64;
        this.seedLo = mixStafford13(lo);
        this.seedHi = mixStafford13(hi);
        if ((this.seedLo | this.seedHi) == 0L) {
            this.seedLo = GOLDEN_RATIO_64;
            this.seedHi = SILVER_RATIO_64;
        }
    }

    public static long chestSeed(long worldSeed, int x, int y, int z) {
        long pos = ((long) x & 0x3FFFFFFL) << 38
                | ((long) y & 0xFFFL) << 26
                | (long) z & 0x3FFFFFFL;
        return worldSeed ^ pos;
    }

    private static long mixStafford13(long l) {
        long result = l;
        result = (result ^ result >>> 30) * -4658895280553007687L;
        result = (result ^ result >>> 27) * -7723592293110705685L;
        return result ^ result >>> 31;
    }

    public long nextLong() {
        long l = this.seedLo;
        long m = this.seedHi;
        long n = Long.rotateLeft(l + m, 17) + l;
        m ^= l;
        this.seedLo = Long.rotateLeft(l, 49) ^ m ^ (m << 21);
        this.seedHi = Long.rotateLeft(m, 28);
        return n;
    }

    public int nextInt() {
        return (int) this.nextLong();
    }

    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("Bound must be positive");
        }
        long l = Integer.toUnsignedLong(this.nextInt());
        long m = l * (long) bound;
        long n = m & 0xFFFF_FFFFL;
        if (n < (long) bound) {
            int j = Integer.remainderUnsigned(~bound + 1, bound);
            while (n < (long) j) {
                l = Integer.toUnsignedLong(this.nextInt());
                m = l * (long) bound;
                n = m & 0xFFFF_FFFFL;
            }
        }
        return (int) (m >>> 32);
    }

    public boolean chance(double chance) {
        return (this.nextLong() >>> 40) < (long) (chance * (1L << 24));
    }

    public boolean simulatePool(int rolls, int totalWeight, int appleWeight) {
        for (int i = 0; i < rolls; i++) {
            if (this.nextInt(totalWeight) < appleWeight) {
                return true;
            }
        }
        return false;
    }
}

