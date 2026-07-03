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

import bodevelopment.client.blackout.module.modules.visual.world.SeedSearcher.FoundStructure;
import bodevelopment.client.blackout.module.modules.visual.world.SeedSearcher.StructureType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.WorldgenRandom;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class StructureAlgorithms {
    private static final Set<ResourceKey<Biome>> OCEAN_BIOMES = Set.of(
            Biomes.OCEAN, Biomes.DEEP_OCEAN,
            Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN,
            Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN,
            Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN
    );

    private static final Set<ResourceKey<Biome>> OCEAN_RIVER_BIOMES = Set.of(
            Biomes.OCEAN, Biomes.DEEP_OCEAN,
            Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN,
            Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN,
            Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN,
            Biomes.RIVER, Biomes.FROZEN_RIVER,
            Biomes.SWAMP, Biomes.MANGROVE_SWAMP,
            Biomes.STONY_SHORE, Biomes.BEACH, Biomes.SNOWY_BEACH
    );

    private static final Set<ResourceKey<Biome>> OVERWORLD_CAVE_BIOMES = Set.of(
            Biomes.LUSH_CAVES, Biomes.DRIPSTONE_CAVES, Biomes.DEEP_DARK
    );

    private static final Set<StructureType> SURFACE_STRUCTURE_TYPES = Set.of(
            StructureType.DESERT_PYRAMID, StructureType.JUNGLE_TEMPLE,
            StructureType.SWAMP_HUT, StructureType.IGLOO
    );

    private static final int NETHER_SPACING = 27;
    private static final int NETHER_SEPARATION = 4;
    private static final int NETHER_SALT = 30084232;

    private static final int GATEWAY_COUNT = 20;
    private static final double GATEWAY_RADIUS = 96.0;

    private static final int STRONGHOLD_COUNT = 128;
    private static final int STRONGHOLD_DISTANCE = 32;
    private static final int STRONGHOLD_SPREAD = 3;
    private static final int[][] FIXED_GATEWAY_POSITIONS = {
            {96, 0}, {91, 29}, {77, 56}, {56, 77}, {29, 91},
            {-1, 96}, {-30, 91}, {-57, 77}, {-78, 56}, {-92, 29},
            {-96, -1}, {-92, -30}, {-78, -57}, {-57, -78}, {-30, -92},
            {0, -96}, {29, -92}, {56, -78}, {77, -57}, {91, -30},
    };

    private StructureAlgorithms() {
    }

    public static void findInArea(List<FoundStructure> results, long worldSeed,
                                  SeedSourceUtils source, Level level,
                                  ResourceKey<Level> dim,
                                  int centerX, int centerZ, int radius,
                                  boolean limitRadius,
                                  java.util.function.Predicate<StructureType> enabledCheck) {

        int playerChunkX = centerX >> 4;
        int playerChunkZ = centerZ >> 4;
        int radiusChunks = radius >> 4;
        double radiusSq = (double) radius * radius;

        if (dim == Level.NETHER) {
            findNetherStructures(results, worldSeed, source, level,
                    playerChunkX, playerChunkZ, radiusChunks, radiusSq,
                    limitRadius, enabledCheck, centerX, centerZ);
        } else if (dim == Level.END) {
            if (enabledCheck.test(StructureType.END_CITY))
                findEndCities(results, worldSeed, source, level,
                        playerChunkX, playerChunkZ, radiusChunks, radiusSq,
                        limitRadius, centerX, centerZ);
            if (enabledCheck.test(StructureType.END_GATEWAY))
                findEndGateways(results, worldSeed, centerX, centerZ, radiusSq, limitRadius);
        } else {

            if (enabledCheck.test(StructureType.SPAWN))
                findSpawnPos(results, source);

            for (StructureType type : StructureType.values()) {
                if (!enabledCheck.test(type)) continue;
                if (type.isCaveBiome()) continue;

                switch (type) {
                    case MINESHAFT -> findMineshafts(results, worldSeed, source,
                            centerX, centerZ, radius, radiusSq, limitRadius);
                    case BURIED_TREASURE -> findBuriedTreasure(results, worldSeed, source,
                            centerX, centerZ, radius, radiusSq, limitRadius);
                    case STRONGHOLD -> findStrongholds(results, worldSeed, source,
                            centerX, centerZ, radiusSq, limitRadius);
                    case GEODE -> {
                    }
                    case DUNGEON -> findDungeons(results, worldSeed, source,
                            centerX, centerZ, radius, radiusSq, limitRadius);
                    case LAVA_POOL_SURFACE, LAVA_POOL_CAVE -> findLavaPools(results, worldSeed,
                            type, centerX, centerZ, radius, radiusSq, limitRadius);
                    case RAVINE -> findRavines(results, source,
                            centerX, centerZ, radius, radiusSq, limitRadius);
                    case DESERT_WELL -> findDesertWells(results, worldSeed, source,
                            centerX, centerZ, radius, radiusSq, limitRadius);
                    default -> {
                        if (type.spacing > 0) {
                            findRandomSpread(results, worldSeed, source, level, type,
                                    playerChunkX, playerChunkZ, radiusChunks, radiusSq,
                                    limitRadius, centerX, centerZ);
                        }
                    }
                }
            }

            findCaveBiomes(results, source, centerX, centerZ, radius, radiusSq,
                    limitRadius, enabledCheck);
        }
    }

    public static void findRandomSpread(List<FoundStructure> results, long worldSeed,
                                        SeedSourceUtils source, Level level,
                                        StructureType type,
                                        int playerChunkX, int playerChunkZ,
                                        int radiusChunks, double radiusSq,
                                        boolean limitRadius,
                                        int centerX, int centerZ) {
        int spacing = type.spacing;
        int separation = type.separation;
        int range = spacing - separation;

        int minRegX = Math.floorDiv(playerChunkX - radiusChunks, spacing);
        int maxRegX = Math.floorDiv(playerChunkX + radiusChunks, spacing);
        int minRegZ = Math.floorDiv(playerChunkZ - radiusChunks, spacing);
        int maxRegZ = Math.floorDiv(playerChunkZ + radiusChunks, spacing);

        for (int rx = minRegX; rx <= maxRegX; rx++) {
            for (int rz = minRegZ; rz <= maxRegZ; rz++) {

                WorldgenRandom regionRng = RandomUtils.createLargeFeatureWithSalt(
                        worldSeed, rx, rz, type.salt);

                int offsetX, offsetZ;
                if (type.triangular) {

                    offsetX = (regionRng.nextInt(range) + regionRng.nextInt(range)) / 2;
                    offsetZ = (regionRng.nextInt(range) + regionRng.nextInt(range)) / 2;
                } else {
                    offsetX = regionRng.nextInt(range);
                    offsetZ = regionRng.nextInt(range);
                }

                int chunkX = rx * spacing + offsetX;
                int chunkZ = rz * spacing + offsetZ;
                int blockX = chunkX << 4;
                int blockZ = chunkZ << 4;

                if (limitRadius) {
                    double dx = (double) blockX - centerX;
                    double dz = (double) blockZ - centerZ;
                    if (dx * dx + dz * dz > radiusSq) continue;
                }

                if (type.frequency < 1.0F) {
                    if (!passesFrequencyCheck(worldSeed, chunkX, chunkZ, type))
                        continue;
                }

                ResourceKey<Biome> biome = null;
                if (type.validBiomes != null && source != null) {
                    int biomeY = getBiomeCheckY(type);
                    biome = source.getBiomeAt(blockX, biomeY, blockZ);
                    if (!type.validBiomes.contains(biome)) {
                        continue;
                    }
                }

                if (!hasValidTerrain(source, type, blockX, blockZ)) continue;

                String extra = getStructureExtra(worldSeed, chunkX, chunkZ, type, biome);

                int blockY = computeStructureY(source, level, worldSeed, type, blockX, blockZ);

                results.add(new FoundStructure(type, blockX, blockY, blockZ, extra));
            }
        }
    }

    private static boolean passesFrequencyCheck(long worldSeed, int chunkX, int chunkZ,
                                                StructureType type) {
        WorldgenRandom rng = RandomUtils.chunkGenerateRandom(worldSeed, chunkX, chunkZ);
        return switch (type) {

            case PILLAGER_OUTPOST -> rng.nextDouble() < (double) type.frequency;
            case BURIED_TREASURE -> rng.nextFloat() < 0.01f;
            case MINESHAFT -> rng.nextDouble() < 0.004;
            default -> rng.nextDouble() < (double) type.frequency;
        };
    }

    private static void findDesertWells(List<FoundStructure> results, long worldSeed,
                                        SeedSourceUtils source,
                                        int centerX, int centerZ, int radius,
                                        double radiusSq, boolean limitRadius) {
        int minChunkX = Math.floorDiv(centerX - radius, 16);
        int maxChunkX = Math.floorDiv(centerX + radius, 16);
        int minChunkZ = Math.floorDiv(centerZ - radius, 16);
        int maxChunkZ = Math.floorDiv(centerZ + radius, 16);

        long totalChunks = (long) (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
        if (totalChunks > 2_000_000) return;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                WorldgenRandom rng = RandomUtils.chunkGenerateRandom(worldSeed, cx, cz);
                if (rng.nextFloat() >= 0.001F) continue;

                int blockX = (cx << 4) + rng.nextInt(16);
                int blockZ = (cz << 4) + rng.nextInt(16);

                if (source != null && source.getBiome(blockX, blockZ) != Biomes.DESERT)
                    continue;

                if (limitRadius) {
                    double dx = (double) blockX - centerX;
                    double dz = (double) blockZ - centerZ;
                    if (dx * dx + dz * dz > radiusSq) continue;
                }

                int dwY = 64;
                if (source != null) {
                    int sy = source.getSurfaceY(blockX, blockZ);
                    if (sy != Integer.MIN_VALUE) dwY = sy;
                }
                results.add(new FoundStructure(StructureType.DESERT_WELL, blockX, dwY, blockZ, ""));
            }
        }
    }

    private static void findMineshafts(List<FoundStructure> results, long worldSeed,
                                       SeedSourceUtils source,
                                       int centerX, int centerZ, int radius,
                                       double radiusSq, boolean limitRadius) {
        int minChunkX = Math.floorDiv(centerX - radius, 16);
        int maxChunkX = Math.floorDiv(centerX + radius, 16);
        int minChunkZ = Math.floorDiv(centerZ - radius, 16);
        int maxChunkZ = Math.floorDiv(centerZ + radius, 16);

        long totalChunks = (long) (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
        if (totalChunks > 2_000_000) return;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                WorldgenRandom rng = RandomUtils.chunkGenerateRandom(worldSeed, cx, cz);
                if (rng.nextDouble() >= 0.004) continue;

                int blockX = cx << 4;
                int blockZ = cz << 4;

                if (limitRadius) {
                    double dx = (double) blockX - centerX;
                    double dz = (double) blockZ - centerZ;
                    if (dx * dx + dz * dz > radiusSq) continue;
                }

                int mineY = 50;
                if (source != null) {
                    int sy = source.getSurfaceY(blockX + 8, blockZ + 8);
                    if (sy != Integer.MIN_VALUE && sy > 40) mineY = Math.min(sy, 70);
                }
                results.add(new FoundStructure(StructureType.MINESHAFT, blockX, mineY, blockZ, ""));
            }
        }
    }

    private static void findBuriedTreasure(List<FoundStructure> results, long worldSeed,
                                           SeedSourceUtils source,
                                           int centerX, int centerZ, int radius,
                                           double radiusSq, boolean limitRadius) {
        int minChunkX = Math.floorDiv(centerX - radius, 16);
        int maxChunkX = Math.floorDiv(centerX + radius, 16);
        int minChunkZ = Math.floorDiv(centerZ - radius, 16);
        int maxChunkZ = Math.floorDiv(centerZ + radius, 16);

        long totalChunks = (long) (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
        if (totalChunks > 2_000_000) return;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                WorldgenRandom rng = RandomUtils.chunkGenerateRandom(worldSeed, cx, cz);
                if (rng.nextFloat() >= 0.01F) continue;

                int blockX = (cx << 4) + rng.nextInt(16);
                int blockZ = (cz << 4) + rng.nextInt(16);

                if (source != null) {
                    ResourceKey<Biome> biome = source.getBiome(blockX, blockZ);
                    if (biome != Biomes.BEACH && biome != Biomes.SNOWY_BEACH
                            && biome != Biomes.STONY_SHORE) continue;
                }

                if (limitRadius) {
                    double dx = (double) blockX - centerX;
                    double dz = (double) blockZ - centerZ;
                    if (dx * dx + dz * dz > radiusSq) continue;
                }

                int btY = 64;
                if (source != null) {
                    int sy = source.getSurfaceY(blockX, blockZ);
                    if (sy != Integer.MIN_VALUE) btY = sy - 1;
                }
                results.add(new FoundStructure(StructureType.BURIED_TREASURE,
                        blockX, btY, blockZ, ""));
            }
        }
    }

    public static void findNetherStructures(List<FoundStructure> results, long worldSeed,
                                            SeedSourceUtils source, Level level,
                                            int playerChunkX, int playerChunkZ,
                                            int radiusChunks, double radiusSq,
                                            boolean limitRadius,
                                            java.util.function.Predicate<StructureType> enabledCheck,
                                            int centerX, int centerZ) {
        boolean showFortress = enabledCheck.test(StructureType.NETHER_FORTRESS);
        boolean showBastion = enabledCheck.test(StructureType.BASTION_REMNANT);
        boolean showFossil = enabledCheck.test(StructureType.NETHER_FOSSIL);

        int range = NETHER_SPACING - NETHER_SEPARATION;
        int minRegX = Math.floorDiv(playerChunkX - radiusChunks, NETHER_SPACING);
        int maxRegX = Math.floorDiv(playerChunkX + radiusChunks, NETHER_SPACING);
        int minRegZ = Math.floorDiv(playerChunkZ - radiusChunks, NETHER_SPACING);
        int maxRegZ = Math.floorDiv(playerChunkZ + radiusChunks, NETHER_SPACING);

        for (int rx = minRegX; rx <= maxRegX; rx++) {
            for (int rz = minRegZ; rz <= maxRegZ; rz++) {

                WorldgenRandom regionRng = RandomUtils.createLargeFeatureWithSalt(
                        worldSeed, rx, rz, NETHER_SALT);
                int offsetX = regionRng.nextInt(range);
                int offsetZ = regionRng.nextInt(range);

                int chunkX = rx * NETHER_SPACING + offsetX;
                int chunkZ = rz * NETHER_SPACING + offsetZ;
                int blockX = chunkX << 4;
                int blockZ = chunkZ << 4;

                WorldgenRandom chunkRng = RandomUtils.chunkGenerateRandom(worldSeed, chunkX, chunkZ);
                boolean isFortress = chunkRng.nextInt(5) < 2;

                StructureType type = isFortress ? StructureType.NETHER_FORTRESS
                        : StructureType.BASTION_REMNANT;

                if ((isFortress && !showFortress) || (!isFortress && !showBastion))
                    continue;

                if (limitRadius) {
                    double dx = blockX - centerX;
                    double dz = blockZ - centerZ;
                    if (dx * dx + dz * dz > radiusSq) continue;
                }

                if (type.validBiomes != null && source != null
                        && !type.validBiomes.contains(source.getBiome(blockX, blockZ)))
                    continue;

                String extra = "";
                if (!isFortress) {
                    extra = switch (RandomUtils.getBastionTypeIndex(worldSeed, chunkX, chunkZ)) {
                        case 0 -> "housing";
                        case 1 -> "hoglin";
                        case 2 -> "treasure";
                        default -> "bridges";
                    };
                }

                int netherY = 60;
                if (source != null) {
                    int sy = source.getSurfaceY(blockX + 8, blockZ + 8);
                    if (sy != Integer.MIN_VALUE && sy > 0) netherY = sy;
                }
                results.add(new FoundStructure(type, netherY, blockX, blockZ, extra));
            }
        }

        if (showFossil) {
            findRandomSpread(results, worldSeed, source, level, StructureType.NETHER_FOSSIL,
                    playerChunkX, playerChunkZ, radiusChunks, radiusSq,
                    limitRadius, centerX, centerZ);
        }
    }

    public static void findEndCities(List<FoundStructure> results, long worldSeed,
                                     SeedSourceUtils source, Level level,
                                     int playerChunkX, int playerChunkZ,
                                     int radiusChunks, double radiusSq,
                                     boolean limitRadius,
                                     int centerX, int centerZ) {
        int spacing = StructureType.END_CITY.spacing;
        int separation = StructureType.END_CITY.separation;
        int range = spacing - separation;

        int minRegX = Math.floorDiv(playerChunkX - radiusChunks, spacing);
        int maxRegX = Math.floorDiv(playerChunkX + radiusChunks, spacing);
        int minRegZ = Math.floorDiv(playerChunkZ - radiusChunks, spacing);
        int maxRegZ = Math.floorDiv(playerChunkZ + radiusChunks, spacing);

        final long MIN_DIST_SQ = 1008L * 1008L;

        for (int rx = minRegX; rx <= maxRegX; rx++) {
            for (int rz = minRegZ; rz <= maxRegZ; rz++) {
                WorldgenRandom regionRng = RandomUtils.createLargeFeatureWithSalt(
                        worldSeed, rx, rz, StructureType.END_CITY.salt);
                int offsetX = (regionRng.nextInt(range) + regionRng.nextInt(range)) / 2;
                int offsetZ = (regionRng.nextInt(range) + regionRng.nextInt(range)) / 2;

                int chunkX = rx * spacing + offsetX;
                int chunkZ = rz * spacing + offsetZ;
                int blockX = chunkX << 4;
                int blockZ = chunkZ << 4;

                long distSq = (long) blockX * blockX + (long) blockZ * blockZ;
                if (distSq < MIN_DIST_SQ) continue;

                if (limitRadius) {
                    double dx = (double) blockX - centerX;
                    double dz = (double) blockZ - centerZ;
                    if (dx * dx + dz * dz > radiusSq) continue;
                }

                if (source != null) {
                    ResourceKey<Biome> biome = source.getBiome(blockX, blockZ);
                    if (biome != Biomes.END_HIGHLANDS && biome != Biomes.END_MIDLANDS) continue;

                    int cx = blockX + 7;
                    int cz = blockZ + 7;
                    WorldgenRandom rotRng = RandomUtils.endCityRandom(worldSeed, chunkX, chunkZ);
                    int rot = rotRng.nextInt(4);
                    int di = (rot == 1 || rot == 2) ? -5 : 5;
                    int dj = (rot == 2 || rot == 3) ? -5 : 5;
                    if (!source.hasTerrainAtOrAbove60(cx, cz)
                            || !source.hasTerrainAtOrAbove60(cx, cz + dj)
                            || !source.hasTerrainAtOrAbove60(cx + di, cz)
                            || !source.hasTerrainAtOrAbove60(cx + di, cz + dj))
                        continue;
                }

                boolean hasShip = EndCitySimulationUtils.hasShip(worldSeed, chunkX, chunkZ);
                results.add(new FoundStructure(StructureType.END_CITY, blockX, blockZ,
                        hasShip ? "Ship" : ""));
            }
        }
    }

    public static void findEndGateways(List<FoundStructure> results, long worldSeed,
                                       int centerX, int centerZ,
                                       double radiusSq, boolean limitRadius) {

        java.util.Random shuffleRng = new java.util.Random(worldSeed);
        int[] order = java.util.stream.IntStream.range(0, GATEWAY_COUNT).toArray();
        for (int i = order.length - 1; i > 0; i--) {
            int j = shuffleRng.nextInt(i + 1);
            int tmp = order[i];
            order[i] = order[j];
            order[j] = tmp;
        }

        for (int idx = 0; idx < GATEWAY_COUNT; idx++) {
            int i = order[idx];
            int bx = FIXED_GATEWAY_POSITIONS[i][0];
            int bz = FIXED_GATEWAY_POSITIONS[i][1];

            if (limitRadius) {
                double dx = (double) bx - centerX;
                double dz = (double) bz - centerZ;
                if (dx * dx + dz * dz > radiusSq) continue;
            }
            results.add(new FoundStructure(StructureType.END_GATEWAY, bx, bz, ""));
        }
    }

    public static void findStrongholds(List<FoundStructure> results, long worldSeed,
                                       SeedSourceUtils source,
                                       int centerX, int centerZ,
                                       double radiusSq, boolean limitRadius) {
        int distance = STRONGHOLD_DISTANCE;
        int count = STRONGHOLD_COUNT;
        int spread = STRONGHOLD_SPREAD;

        RandomSource random = RandomSource.create();
        random.setSeed(worldSeed);

        double angle = random.nextDouble() * Math.PI * 2.0;
        int placedInRing = 0;
        int ring = 0;

        for (int i = 0; i < count; i++) {

            double dist = 4.0 * distance + 6.0 * distance * ring
                    + (random.nextDouble() - 0.5) * 2.5 * distance;

            int chunkX = (int) Math.round(Math.cos(angle) * dist);
            int chunkZ = (int) Math.round(Math.sin(angle) * dist);

            random.fork();

            if (source != null) {
                int[] snapped = source.findNearestStrongholdChunk(chunkX, chunkZ);
                chunkX = snapped[0];
                chunkZ = snapped[1];
            }

            int[] portalPos = StrongholdSimulationUtils.findPortalRoomPos(worldSeed, chunkX, chunkZ);
            if (portalPos != null) {
                if (!limitRadius || (Math.pow(portalPos[0] - centerX, 2)
                        + Math.pow(portalPos[2] - centerZ, 2) <= radiusSq)) {
                    results.add(new FoundStructure(StructureType.STRONGHOLD,
                            portalPos[0], portalPos[1], portalPos[2], ""));
                }
            } else {

                int blockX = chunkX << 4;
                int blockZ = chunkZ << 4;
                if (!limitRadius || (Math.pow(blockX - centerX, 2)
                        + Math.pow(blockZ - centerZ, 2) <= radiusSq)) {
                    results.add(new FoundStructure(StructureType.STRONGHOLD, blockX, -40, blockZ, ""));
                }
            }

            angle += Math.PI * 2.0 / spread;
            placedInRing++;

            if (placedInRing == spread) {
                ring++;
                placedInRing = 0;
                spread += 2 * spread / (ring + 1);
                spread = Math.min(spread, count - i - 1);
                if (spread <= 0) break;
                angle += random.nextDouble() * Math.PI * 2.0;
            }
        }
    }

    private static void findDungeons(List<FoundStructure> results, long worldSeed,
                                     SeedSourceUtils source,
                                     int centerX, int centerZ, int radius,
                                     double radiusSq, boolean limitRadius) {
        int minChunkX = Math.floorDiv(centerX - radius, 16);
        int maxChunkX = Math.floorDiv(centerX + radius, 16);
        int minChunkZ = Math.floorDiv(centerZ - radius, 16);
        int maxChunkZ = Math.floorDiv(centerZ + radius, 16);

        long totalChunks = (long) (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
        if (totalChunks > 2_000_000) return;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                WorldgenRandom rng = RandomUtils.chunkGenerateRandom(worldSeed, cx, cz);
                if (rng.nextInt(100) != 0) continue;

                int blockX = (cx << 4) + rng.nextInt(16);
                int blockZ = (cz << 4) + rng.nextInt(16);

                if (source != null) {
                    ResourceKey<Biome> biome = source.getBiome(blockX, blockZ);
                    if (!OVERWORLD_CAVE_BIOMES.contains(biome)) continue;
                }

                if (limitRadius) {
                    double dx = (double) blockX - centerX;
                    double dz = (double) blockZ - centerZ;
                    if (dx * dx + dz * dz > radiusSq) continue;
                }

                results.add(new FoundStructure(StructureType.DUNGEON, blockX, -40, blockZ, ""));
            }
        }
    }

    private static void findLavaPools(List<FoundStructure> results, long worldSeed,
                                      StructureType type,
                                      int centerX, int centerZ, int radius,
                                      double radiusSq, boolean limitRadius) {
        int minChunkX = Math.floorDiv(centerX - radius, 16);
        int maxChunkX = Math.floorDiv(centerX + radius, 16);
        int minChunkZ = Math.floorDiv(centerZ - radius, 16);
        int maxChunkZ = Math.floorDiv(centerZ + radius, 16);

        long totalChunks = (long) (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
        if (totalChunks > 2_000_000) return;

        int frequency = type == StructureType.LAVA_POOL_SURFACE ? 33 : 9;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                WorldgenRandom rng = RandomUtils.chunkGenerateRandom(worldSeed, cx, cz);
                if (rng.nextInt(frequency) != 0) continue;

                int blockX = (cx << 4) + rng.nextInt(16);
                int blockZ = (cz << 4) + rng.nextInt(16);

                if (limitRadius) {
                    double dx = (double) blockX - centerX;
                    double dz = (double) blockZ - centerZ;
                    if (dx * dx + dz * dz > radiusSq) continue;
                }

                int lpY = type == StructureType.LAVA_POOL_SURFACE ? 64 : -20;
                results.add(new FoundStructure(type, blockX, lpY, blockZ, ""));
            }
        }
    }

    private static void findRavines(List<FoundStructure> results, SeedSourceUtils source,
                                    int centerX, int centerZ, int radius,
                                    double radiusSq, boolean limitRadius) {
        int minChunkX = Math.floorDiv(centerX - radius, 16);
        int maxChunkX = Math.floorDiv(centerX + radius, 16);
        int minChunkZ = Math.floorDiv(centerZ - radius, 16);
        int maxChunkZ = Math.floorDiv(centerZ + radius, 16);

        long totalChunks = (long) (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
        if (totalChunks > 2_000_000) return;

        for (int cx = minChunkX; cx <= maxChunkX; cx += 4) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz += 4) {
                if (getSimplexNoiseApprox(cx * 0.2, cz * 0.2) <= 0.0) continue;

                int blockX = (cx << 4) + 8;
                int blockZ = (cz << 4) + 8;

                if (limitRadius) {
                    double dx = (double) blockX - centerX;
                    double dz = (double) blockZ - centerZ;
                    if (dx * dx + dz * dz > radiusSq) continue;
                }

                results.add(new FoundStructure(StructureType.RAVINE, blockX, blockZ, ""));
            }
        }
    }

    public static void findCaveBiomes(List<FoundStructure> results, SeedSourceUtils source,
                                      int centerX, int centerZ, int radius, double radiusSq,
                                      boolean limitRadius,
                                      java.util.function.Predicate<StructureType> enabledCheck) {
        int scanStep = 128;
        int sectionSize = 512;

        for (StructureType type : new StructureType[]{StructureType.LUSH_CAVES,
                StructureType.DRIPSTONE_CAVES}) {
            if (!enabledCheck.test(type)) continue;
            ResourceKey<Biome> targetBiome = type.validBiomes.iterator().next();

            int minSecX = Math.floorDiv(centerX - radius, sectionSize);
            int maxSecX = Math.floorDiv(centerX + radius, sectionSize);
            int minSecZ = Math.floorDiv(centerZ - radius, sectionSize);
            int maxSecZ = Math.floorDiv(centerZ + radius, sectionSize);

            List<int[]> rawCandidates = new ArrayList<>();

            for (int sx = minSecX; sx <= maxSecX; sx++) {
                for (int sz = minSecZ; sz <= maxSecZ; sz++) {
                    long sumX = 0, sumZ = 0;
                    int bioCount = 0;
                    int ssX = sx * sectionSize;
                    int ssZ = sz * sectionSize;

                    for (int bx = ssX; bx < ssX + sectionSize; bx += scanStep) {
                        for (int bz = ssZ; bz < ssZ + sectionSize; bz += scanStep) {
                            if (source.getBiomeAt(bx, 16, bz).equals(targetBiome)) {
                                sumX += bx;
                                sumZ += bz;
                                bioCount++;
                            }
                        }
                    }

                    if (bioCount >= 2) {
                        int fx = (int) (sumX / bioCount);
                        int fz = (int) (sumZ / bioCount);

                        if (limitRadius) {
                            double dx = fx - centerX;
                            double dz = fz - centerZ;
                            if (dx * dx + dz * dz > radiusSq) continue;
                        }

                        boolean merged = false;
                        for (int[] existing : rawCandidates) {
                            if (Math.abs(existing[0] - fx) < 400
                                    && Math.abs(existing[1] - fz) < 400) {
                                existing[0] = (existing[0] + fx) / 2;
                                existing[1] = (existing[1] + fz) / 2;
                                merged = true;
                                break;
                            }
                        }
                        if (!merged) rawCandidates.add(new int[]{fx, fz});
                    }
                }
            }

            for (int[] pos : rawCandidates)
                results.add(new FoundStructure(type, pos[0], pos[1], ""));
        }
    }

    public static void findSpawnPos(List<FoundStructure> results, SeedSourceUtils source) {
        int foundX = 0, foundZ = 0;
        outer:
        for (int r = 0; r <= 512; r++) {
            for (int dz = -r; dz <= r; dz++) {
                boolean edgeZ = Math.abs(dz) == r;
                for (int dx = -r; dx <= r; dx++) {
                    if (!edgeZ && Math.abs(dx) != r) continue;
                    ResourceKey<Biome> biome = source.getBiome(dx * 4, dz * 4);
                    if (isValidSpawnBiome(biome)) {
                        foundX = dx * 4;
                        foundZ = dz * 4;
                        break outer;
                    }
                }
            }
        }
        results.add(new FoundStructure(StructureType.SPAWN, foundX, foundZ, ""));
    }

    private static boolean isValidSpawnBiome(ResourceKey<Biome> biome) {
        return biome != Biomes.OCEAN && biome != Biomes.DEEP_OCEAN
                && biome != Biomes.COLD_OCEAN && biome != Biomes.DEEP_COLD_OCEAN
                && biome != Biomes.WARM_OCEAN && biome != Biomes.LUKEWARM_OCEAN
                && biome != Biomes.DEEP_LUKEWARM_OCEAN
                && biome != Biomes.FROZEN_OCEAN && biome != Biomes.DEEP_FROZEN_OCEAN
                && biome != Biomes.RIVER && biome != Biomes.FROZEN_RIVER
                && biome != Biomes.THE_VOID
                && biome != Biomes.NETHER_WASTES && biome != Biomes.SOUL_SAND_VALLEY
                && biome != Biomes.CRIMSON_FOREST && biome != Biomes.WARPED_FOREST
                && biome != Biomes.BASALT_DELTAS
                && biome != Biomes.THE_END && biome != Biomes.END_HIGHLANDS
                && biome != Biomes.END_MIDLANDS && biome != Biomes.END_BARRENS
                && biome != Biomes.SMALL_END_ISLANDS;
    }

    public static int computeStructureY(SeedSourceUtils source, Level level,
                                        long worldSeed, StructureType type,
                                        int blockX, int blockZ) {
        if (source == null) return Integer.MIN_VALUE;

        return switch (type) {
            case DESERT_PYRAMID -> {

                int minY = TerrainUtils.getMinimumSurfaceYInArea(source, level,
                        blockX, blockZ, 21, 21);
                if (minY == Integer.MIN_VALUE) yield Integer.MIN_VALUE;
                int randomOffset = RandomUtils.getPyramidRandomOffset(worldSeed, blockX, blockZ);
                yield minY - randomOffset;
            }
            case JUNGLE_TEMPLE -> {
                int minY = TerrainUtils.getMinimumSurfaceYInArea(source, level,
                        blockX, blockZ, 12, 12);
                if (minY == Integer.MIN_VALUE) yield Integer.MIN_VALUE;
                yield minY - RandomUtils.getSurfaceStructureRandomOffset(worldSeed, blockX, blockZ);
            }
            case SWAMP_HUT, IGLOO -> {
                int minY = TerrainUtils.getMinimumSurfaceYInArea(source, level,
                        blockX, blockZ, 7, 7);
                if (minY == Integer.MIN_VALUE) yield Integer.MIN_VALUE;
                yield minY - RandomUtils.getSurfaceStructureRandomOffset(worldSeed, blockX, blockZ);
            }
            default -> source.getSurfaceY(blockX + 8, blockZ + 8);
        };
    }

    private static int getBiomeCheckY(StructureType type) {
        return switch (type) {
            case ANCIENT_CITY -> -27;
            case TRIAL_CHAMBERS -> -40;
            case DUNGEON -> -40;
            case STRONGHOLD -> -40;
            case MINESHAFT -> -30;
            case LUSH_CAVES, DRIPSTONE_CAVES -> 16;

            case PILLAGER_OUTPOST, TRAIL_RUINS -> 319;
            default -> 319;
        };
    }

    private static boolean hasValidTerrain(SeedSourceUtils source, StructureType type,
                                           int blockX, int blockZ) {
        if (!SURFACE_STRUCTURE_TYPES.contains(type)) return true;

        int[][] samplePoints;
        if (type == StructureType.DESERT_PYRAMID || type == StructureType.JUNGLE_TEMPLE) {
            samplePoints = new int[][]{{0, 0}, {10, 10}, {10, -10}, {-10, 10}, {-10, -10}};
        } else {
            samplePoints = new int[][]{{0, 0}, {3, 3}, {3, -3}, {-3, 3}, {-3, -3}};
        }

        for (int[] pt : samplePoints) {

            ResourceKey<Biome> biome = source.getBiome(
                    blockX + 8 + pt[0], blockZ + 8 + pt[1]);
            if (OCEAN_RIVER_BIOMES.contains(biome)) return false;
        }

        int halfW = type == StructureType.DESERT_PYRAMID
                || type == StructureType.JUNGLE_TEMPLE ? 10 : 3;
        return source.hasAreaAboveSeaLevel(blockX + 8, blockZ + 8, halfW);
    }

    private static String getStructureExtra(long worldSeed, int chunkX, int chunkZ,
                                            StructureType type, ResourceKey<Biome> biome) {
        return switch (type) {
            case IGLOO -> RandomUtils.hasIglooBasement(worldSeed, chunkX, chunkZ)
                    ? "Laboratory" : "";
            case OCEAN_RUIN -> {
                boolean large = RandomUtils.isLargeOceanRuin(worldSeed, chunkX, chunkZ);
                yield large ? "Big" : "Small";
            }
            case VILLAGE -> {
                if (biome == null) yield "";
                int totalWeight = getVillageTotalWeight(biome);
                int normalWeight = getVillageNormalWeight(biome);
                boolean zombie = RandomUtils.isZombieVillage(worldSeed, chunkX, chunkZ,
                        normalWeight, totalWeight);
                yield zombie ? "zombie_" + biome.location().getPath()
                        : biome.location().getPath();
            }
            default -> "";
        };
    }

    private static int getVillageTotalWeight(ResourceKey<Biome> biome) {
        if (biome == Biomes.DESERT) return 250;
        if (biome == Biomes.SAVANNA) return 459;
        if (biome == Biomes.SNOWY_PLAINS || biome == Biomes.SNOWY_TAIGA) return 306;
        if (biome == Biomes.TAIGA) return 100;
        return 204;
    }

    private static int getVillageNormalWeight(ResourceKey<Biome> biome) {
        if (biome == Biomes.DESERT) return 245;
        if (biome == Biomes.SAVANNA) return 450;
        if (biome == Biomes.SNOWY_PLAINS || biome == Biomes.SNOWY_TAIGA) return 300;
        if (biome == Biomes.TAIGA) return 98;
        return 200;
    }

    private static double getSimplexNoiseApprox(double x, double z) {
        int ix = (int) Math.floor(x);
        int iz = (int) Math.floor(z);
        double fx = x - ix;
        double fz = z - iz;
        double sx = fx * fx * (3.0 - 2.0 * fx);
        double sz = fz * fz * (3.0 - 2.0 * fz);
        double n00 = hashNoise(ix, iz);
        double n10 = hashNoise(ix + 1, iz);
        double n01 = hashNoise(ix, iz + 1);
        double n11 = hashNoise(ix + 1, iz + 1);
        return (n00 + (n10 - n00) * sx)
                + ((n01 + (n11 - n01) * sx) - (n00 + (n10 - n00) * sx)) * sz;
    }

    private static double hashNoise(int x, int z) {
        long hash = ((long) x * 341873128712L + (long) z * 132897987541L)
                & 0x7FFFFFFFFFFFFFFFL;
        hash = hash * 0x9E3779B97F4A7C15L;
        hash ^= hash >> 33;
        hash *= 0xC6A4A7935BD1E995L;
        hash ^= hash >> 29;
        return (hash & 0x7FFFFFFFFFFFFFFFL) / (double) Long.MAX_VALUE * 2.0 - 1.0;
    }
}

