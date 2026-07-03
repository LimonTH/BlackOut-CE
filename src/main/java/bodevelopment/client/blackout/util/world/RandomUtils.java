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

package bodevelopment.client.blackout.util.world;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public final class RandomUtils {
    private static final long REGION_X_MULT = 341873128712L;
    private static final long REGION_Z_MULT = 132897987541L;

    private RandomUtils() {
    }

    public static long largeFeatureWithSaltSeed(int regionX, int regionZ, long worldSeed, int salt) {
        return (long) regionX * REGION_X_MULT + (long) regionZ * REGION_Z_MULT + worldSeed + (long) salt;
    }

    public static WorldgenRandom createLargeFeatureWithSalt(long worldSeed, int regionX, int regionZ, int salt) {
        WorldgenRandom rng = createFresh();
        rng.setLargeFeatureWithSalt(worldSeed, regionX, regionZ, salt);
        return rng;
    }

    public static long largeFeatureSeed(long worldSeed, int chunkX, int chunkZ) {
        WorldgenRandom rng = createFresh();
        rng.setSeed(worldSeed);
        long a = rng.nextLong() | 1L;
        long b = rng.nextLong() | 1L;
        return worldSeed ^ ((long) chunkX * a) ^ ((long) chunkZ * b);
    }

    public static WorldgenRandom createLargeFeature(long worldSeed, int chunkX, int chunkZ) {
        WorldgenRandom rng = createFresh();
        rng.setSeed(worldSeed);
        long a = rng.nextLong() | 1L;
        long b = rng.nextLong() | 1L;
        rng.setSeed(worldSeed ^ ((long) chunkX * a) ^ ((long) chunkZ * b));
        return rng;
    }

    public static long decorationSeed(long worldSeed, int blockX, int blockZ) {
        WorldgenRandom rng = createFresh();
        return rng.setDecorationSeed(worldSeed, blockX, blockZ);
    }

    public static long featureSeed(long decorationSeed, int featureIndex, int featureStep) {

        return decorationSeed + (long) featureIndex + (10000L * (long) featureStep);
    }

    public static int getSurfaceStructureRandomOffset(long worldSeed, int blockX, int blockZ) {
        WorldgenRandom random = createFresh();
        long decoSeed = random.setDecorationSeed(worldSeed, blockX, blockZ);
        random.setFeatureSeed(decoSeed, 0, 4);
        random.nextInt(4);
        return random.nextInt(3);
    }

    public static int getPyramidRandomOffset(long worldSeed, int blockX, int blockZ) {
        return getSurfaceStructureRandomOffset(worldSeed, blockX, blockZ);
    }

    public static boolean isNetherFortress(long worldSeed, int chunkX, int chunkZ) {
        WorldgenRandom rng = createFresh();
        rng.setLargeFeatureSeed(worldSeed, chunkX, chunkZ);
        return rng.nextInt(5) < 2;
    }

    public static int getBastionTypeIndex(long worldSeed, int chunkX, int chunkZ) {
        WorldgenRandom rng = createFresh();
        rng.setLargeFeatureSeed(worldSeed, chunkX, chunkZ);
        return rng.nextInt(4);
    }

    public static WorldgenRandom chunkGenerateRandom(long worldSeed, int chunkX, int chunkZ) {
        WorldgenRandom rng = createFresh();
        rng.setLargeFeatureSeed(worldSeed, chunkX, chunkZ);
        return rng;
    }

    public static boolean hasIglooBasement(long worldSeed, int chunkX, int chunkZ) {
        WorldgenRandom rng = createFresh();
        rng.setLargeFeatureSeed(worldSeed, chunkX, chunkZ);
        rng.nextInt(4);
        return rng.nextDouble() < 0.5;
    }

    public static boolean isLargeOceanRuin(long worldSeed, int chunkX, int chunkZ) {
        WorldgenRandom rng = createFresh();
        rng.setLargeFeatureSeed(worldSeed, chunkX, chunkZ);
        rng.nextInt(4);
        return rng.nextFloat() < 0.3F;
    }

    public static boolean isZombieVillage(long worldSeed, int chunkX, int chunkZ,
                                          int normalWeight, int totalWeight) {
        WorldgenRandom rng = createFresh();
        rng.setLargeFeatureSeed(worldSeed, chunkX, chunkZ);
        rng.nextInt(4);
        return rng.nextInt(totalWeight) >= normalWeight;
    }

    public static WorldgenRandom endCityRandom(long worldSeed, int chunkX, int chunkZ) {
        return createLargeFeature(worldSeed, chunkX, chunkZ);
    }

    public static WorldgenRandom createFresh() {
        return new WorldgenRandom(new LegacyRandomSource(0L));
    }
}

