package bodevelopment.client.blackout.util.world;

import bodevelopment.client.blackout.module.modules.visual.world.SeedSearcher.StructureType;
import net.minecraft.world.level.levelgen.WorldgenRandom;

import java.util.ArrayList;
import java.util.List;

public final class StructurePieceSimulation {
    private StructurePieceSimulation() {
    }

    public static List<ChestInfo> getChests(StructureType type, long worldSeed,
                                            int blockX, int blockZ,
                                            SeedSourceUtils source) {
        if (type == StructureType.MINESHAFT) {
            int chunkX = blockX >> 4;
            int chunkZ = blockZ >> 4;
            return MineshaftSimulation.getMineshaftChests(worldSeed, chunkX, chunkZ);
        }
        return switch (type) {
            case DESERT_PYRAMID -> getDesertPyramidChests(worldSeed, blockX, blockZ, source);
            case JUNGLE_TEMPLE -> getJungleTempleChests(worldSeed, blockX, blockZ, source);
            case IGLOO -> getIglooChests(worldSeed, blockX, blockZ);
            case BASTION_REMNANT -> getBastionChests(worldSeed, blockX, blockZ);
            case ANCIENT_CITY -> getAncientCityChests(worldSeed, blockX, blockZ);
            case RUINED_PORTAL -> getRuinedPortalChests(worldSeed, blockX, blockZ, source);
            case SHIPWRECK -> getShipwreckChests(worldSeed, blockX, blockZ);
            case OCEAN_RUIN -> getOceanRuinChests(worldSeed, blockX, blockZ);
            case WOODLAND_MANSION -> getMansionChests(worldSeed, blockX, blockZ, source);
            case DUNGEON -> getDungeonChests(worldSeed, blockX, blockZ);
            case END_CITY -> getEndCityChests(worldSeed, blockX, blockZ);
            case NETHER_FORTRESS -> getFortressChests(worldSeed, blockX, blockZ);
            case STRONGHOLD -> getStrongholdChests(worldSeed, blockX, blockZ);
            case BURIED_TREASURE -> getBuriedTreasureChests(worldSeed, blockX, blockZ, source);
            case PILLAGER_OUTPOST -> getOutpostChests(worldSeed, blockX, blockZ, source);
            default -> List.of();
        };
    }

    public static List<int[]> getChestPositions(StructureType type, long worldSeed,
                                                int blockX, int blockZ,
                                                SeedSourceUtils source) {
        List<ChestInfo> chests = getChests(type, worldSeed, blockX, blockZ, source);
        List<int[]> positions = new ArrayList<>(chests.size());
        for (ChestInfo c : chests) {
            positions.add(new int[]{c.blockX(), c.blockY(), c.blockZ()});
        }
        return positions;
    }

    private static WorldgenRandom createLootRng(long worldSeed, int blockX, int blockZ,
                                                PopulationSeedUtils.StructureSaltConfig config) {
        int chunkBlockX = blockX & ~15;
        int chunkBlockZ = blockZ & ~15;
        long populationSeed = PopulationSeedUtils.getPopulationSeed(worldSeed, chunkBlockX, chunkBlockZ);
        return PopulationSeedUtils.createLootRng(populationSeed, config);
    }

    private static WorldgenRandom createLootRngAtChest(long worldSeed, int chestX, int chestZ,
                                                       PopulationSeedUtils.StructureSaltConfig config) {
        int chunkBlockX = chestX & ~15;
        int chunkBlockZ = chestZ & ~15;
        long populationSeed = PopulationSeedUtils.getPopulationSeed(worldSeed, chunkBlockX, chunkBlockZ);
        return PopulationSeedUtils.createLootRng(populationSeed, config);
    }

    private static List<ChestInfo> getDesertPyramidChests(long worldSeed, int blockX, int blockZ,
                                                          SeedSourceUtils source) {
        int minBlockX = blockX & ~15;
        int minBlockZ = blockZ & ~15;

        var config = PopulationSeedUtils.getSaltConfig(StructureType.DESERT_PYRAMID);
        WorldgenRandom rng = createLootRng(worldSeed, blockX, blockZ, config);
        rng.nextInt(3);

        List<ChestInfo> chests = new ArrayList<>(4);
        int[][] positions = {{10, 8}, {12, 10}, {10, 12}, {8, 10}};
        for (int[] pos : positions) {
            chests.add(new ChestInfo(minBlockX + pos[0], 64, minBlockZ + pos[1],
                    rng.nextLong(), "chests/desert_pyramid"));
        }
        return chests;
    }

    private static List<ChestInfo> getIglooChests(long worldSeed, int blockX, int blockZ) {
        int minBlockX = blockX & ~15;
        int minBlockZ = blockZ & ~15;
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;

        WorldgenRandom rng = RandomUtils.chunkGenerateRandom(worldSeed, chunkX, chunkZ);
        int rot = rng.nextInt(4);
        boolean hasBasement = rng.nextDouble() < 0.5;
        if (!hasBasement) return List.of();

        int mirror = 0;
        int chestX, chestZ;
        switch ((rot << 1) | mirror) {
            case 0b00:
                chestX = minBlockX + 1;
                chestZ = minBlockZ + 4;
                break;
            case 0b01:
                chestX = minBlockX + 5;
                chestZ = minBlockZ + 6;
                break;
            case 0b10:
                chestX = minBlockX + 4;
                chestZ = minBlockZ + 3;
                break;
            case 0b11:
                chestX = minBlockX + 2;
                chestZ = minBlockZ + 7;
                break;
            default:
                chestX = minBlockX + 1;
                chestZ = minBlockZ + 4;
        }

        var config = PopulationSeedUtils.getSaltConfig(StructureType.IGLOO);
        WorldgenRandom lootRng = createLootRng(worldSeed, blockX, blockZ, config);
        lootRng.nextLong();
        long lootSeed = lootRng.nextLong();

        return List.of(new ChestInfo(chestX, 58, chestZ, lootSeed, "chests/igloo_chest"));
    }

    private static List<ChestInfo> getJungleTempleChests(long worldSeed, int blockX, int blockZ,
                                                         SeedSourceUtils source) {
        int minBlockX = blockX & ~15;
        int minBlockZ = blockZ & ~15;

        var config = PopulationSeedUtils.getSaltConfig(StructureType.JUNGLE_TEMPLE);
        WorldgenRandom rng = createLootRng(worldSeed, blockX, blockZ, config);

        int chestY = 64;
        if (source != null) {
            int sy = source.getSurfaceY(minBlockX + 8, minBlockZ + 8);
            if (sy != Integer.MIN_VALUE) chestY = sy - 2;
        }

        skipRngCalls(rng, 1511);

        List<ChestInfo> chests = new ArrayList<>(4);

        chests.add(new ChestInfo(minBlockX + 4, chestY, minBlockZ + 2,
                rng.nextLong(), "chests/jungle_temple_dispenser"));
        skipRngCalls(rng, 1513 - 1511 - 2);

        chests.add(new ChestInfo(minBlockX + 10, chestY, minBlockZ + 4,
                rng.nextLong(), "chests/jungle_temple_dispenser"));
        skipRngCalls(rng, 1515 - 1513 - 2);

        chests.add(new ChestInfo(minBlockX + 9, chestY, minBlockZ + 4,
                rng.nextLong(), "chests/jungle_temple"));
        skipRngCalls(rng, 1528 - 1515 - 2);

        chests.add(new ChestInfo(minBlockX + 8, chestY, minBlockZ + 11,
                rng.nextLong(), "chests/jungle_temple"));

        return chests;
    }

    private static List<ChestInfo> getOutpostChests(long worldSeed, int blockX, int blockZ,
                                                    SeedSourceUtils source) {
        int minBlockX = blockX & ~15;
        int minBlockZ = blockZ & ~15;

        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        WorldgenRandom rng = RandomUtils.chunkGenerateRandom(worldSeed, chunkX, chunkZ);
        rng.nextInt(4);

        var config = PopulationSeedUtils.getSaltConfig(StructureType.PILLAGER_OUTPOST);
        WorldgenRandom lootRng = createLootRng(worldSeed, blockX, blockZ, config);

        int chestX = minBlockX + 10;
        int chestZ = minBlockZ + 10;

        int chestY = 64;
        if (source != null) {
            int sy = source.getSurfaceY(chestX, chestZ);
            if (sy != Integer.MIN_VALUE) chestY = sy;
        }

        return List.of(new ChestInfo(chestX, chestY, chestZ,
                lootRng.nextLong(), "chests/pillager_outpost"));
    }

    private static List<ChestInfo> getShipwreckChests(long worldSeed, int blockX, int blockZ) {
        int minBlockX = blockX & ~15;
        int minBlockZ = blockZ & ~15;

        var config = PopulationSeedUtils.getSaltConfig(StructureType.SHIPWRECK);

        WorldgenRandom supplyRng = createLootRngAtChest(worldSeed, minBlockX + 4, minBlockZ + 8, config);
        long supplySeed = supplyRng.nextLong();

        WorldgenRandom mapRng = createLootRngAtChest(worldSeed, minBlockX + 5, minBlockZ + 18, config);
        mapRng.nextLong();
        long mapSeed = mapRng.nextLong();

        WorldgenRandom treasureRng = createLootRngAtChest(worldSeed, minBlockX + 6, minBlockZ + 24, config);
        treasureRng.nextLong();
        treasureRng.nextLong();
        long treasureSeed = treasureRng.nextLong();

        return List.of(
                new ChestInfo(minBlockX + 4, 38, minBlockZ + 8, supplySeed, "chests/shipwreck_supply"),
                new ChestInfo(minBlockX + 5, 38, minBlockZ + 18, mapSeed, "chests/shipwreck_map"),
                new ChestInfo(minBlockX + 6, 38, minBlockZ + 24, treasureSeed, "chests/shipwreck_treasure")
        );
    }

    private static List<ChestInfo> getBastionChests(long worldSeed, int blockX, int blockZ) {

        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        int type = RandomUtils.getBastionTypeIndex(worldSeed, chunkX, chunkZ);

        var config = PopulationSeedUtils.getSaltConfig(StructureType.BASTION_REMNANT);

        return switch (type) {
            case 2 -> {
                WorldgenRandom rng = createLootRngAtChest(worldSeed, blockX + 20, blockZ + 18, config);
                rng.nextLong();
                long seed1 = rng.nextLong();
                long seed2 = rng.nextLong();
                yield List.of(
                        new ChestInfo(blockX + 20, 40, blockZ + 18, seed1, "chests/bastion_treasure"),
                        new ChestInfo(blockX + 16, 40, blockZ + 14, seed2, "chests/bastion_treasure")
                );
            }
            case 0 -> {
                WorldgenRandom rng = createLootRngAtChest(worldSeed, blockX + 14, blockZ + 24, config);
                yield List.of(new ChestInfo(blockX + 14, 55, blockZ + 24, rng.nextLong(), "chests/bastion_other"));
            }
            case 1 -> {
                WorldgenRandom rng = createLootRngAtChest(worldSeed, blockX + 10, blockZ + 12, config);
                yield List.of(new ChestInfo(blockX + 10, 45, blockZ + 12, rng.nextLong(), "chests/bastion_other"));
            }
            default -> {
                WorldgenRandom rng = createLootRngAtChest(worldSeed, blockX + 24, blockZ + 20, config);
                yield List.of(new ChestInfo(blockX + 24, 50, blockZ + 20, rng.nextLong(), "chests/bastion_bridge"));
            }
        };
    }

    private static List<ChestInfo> getAncientCityChests(long worldSeed, int blockX, int blockZ) {

        var config = PopulationSeedUtils.getSaltConfig(StructureType.ANCIENT_CITY);
        int cx = blockX + 56, cz = blockZ + 56;

        int[][] chestCoords = {{-30, -44, -30}, {30, -44, 0}, {0, -44, 30},
                {-20, -44, 20}, {20, -44, -20}, {0, -44, 0}};

        List<ChestInfo> chests = new ArrayList<>(chestCoords.length);
        for (int[] pos : chestCoords) {
            int chestX = cx + pos[0], chestY = pos[1], chestZ = cz + pos[2];
            WorldgenRandom rng = createLootRngAtChest(worldSeed, chestX, chestZ, config);
            String table = rng.nextBoolean() ? "chests/ancient_city" : "chests/ancient_city_ice_box";
            chests.add(new ChestInfo(chestX, chestY, chestZ, rng.nextLong(), table));
        }
        return chests;
    }

    private static List<ChestInfo> getRuinedPortalChests(long worldSeed, int blockX, int blockZ,
                                                         SeedSourceUtils source) {
        var config = PopulationSeedUtils.getSaltConfig(StructureType.RUINED_PORTAL);
        WorldgenRandom rng = createLootRngAtChest(worldSeed, blockX + 4, blockZ + 4, config);
        int y = source != null ? source.getSurfaceY(blockX + 8, blockZ + 8) : 64;
        if (y == Integer.MIN_VALUE) y = 64;
        return List.of(new ChestInfo(blockX + 4, y - rng.nextInt(3), blockZ + 4,
                rng.nextLong(), "chests/ruined_portal"));
    }

    private static List<ChestInfo> getOceanRuinChests(long worldSeed, int blockX, int blockZ) {
        var config = PopulationSeedUtils.getSaltConfig(StructureType.OCEAN_RUIN);
        WorldgenRandom rng = createLootRngAtChest(worldSeed, blockX + 3, blockZ + 3, config);
        rng.nextBoolean();
        return List.of(new ChestInfo(blockX + 3, 38, blockZ + 3,
                rng.nextLong(), "chests/underwater_ruin_small"));
    }

    private static List<ChestInfo> getMansionChests(long worldSeed, int blockX, int blockZ,
                                                    SeedSourceUtils source) {
        var config = PopulationSeedUtils.getSaltConfig(StructureType.WOODLAND_MANSION);
        int y = source != null ? source.getSurfaceY(blockX + 8, blockZ + 8) : 64;
        if (y == Integer.MIN_VALUE) y = 64;

        int[][] roomChests = {{15, y + 8, 10}, {35, y + 8, 25}, {25, y + 16, 35}, {45, y + 8, 15}};
        List<ChestInfo> chests = new ArrayList<>(roomChests.length);
        for (int[] pos : roomChests) {
            WorldgenRandom rng = createLootRngAtChest(worldSeed, pos[0], pos[2], config);
            chests.add(new ChestInfo(pos[0], pos[1], pos[2], rng.nextLong(), "chests/woodland_mansion"));
        }
        return chests;
    }

    private static List<ChestInfo> getDungeonChests(long worldSeed, int blockX, int blockZ) {
        var config = PopulationSeedUtils.getSaltConfig(StructureType.DUNGEON);
        WorldgenRandom rng = createLootRngAtChest(worldSeed, blockX, blockZ, config);
        int nChests = rng.nextInt(2) + 1;
        List<ChestInfo> chests = new ArrayList<>(nChests);
        for (int i = 0; i < nChests; i++) {
            chests.add(new ChestInfo(blockX + rng.nextInt(5) - 2, -40 + rng.nextInt(5),
                    blockZ + rng.nextInt(5) - 2, rng.nextLong(), "chests/simple_dungeon"));
        }
        return chests;
    }

    private static List<ChestInfo> getEndCityChests(long worldSeed, int blockX, int blockZ) {
        var config = PopulationSeedUtils.getSaltConfig(StructureType.END_CITY);
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        if (!EndCitySimulationUtils.hasShip(worldSeed, chunkX, chunkZ)) return List.of();

        int cityOriginX = chunkX * 16 + 8;
        int cityOriginZ = chunkZ * 16 + 8;

        WorldgenRandom rng = createLootRngAtChest(worldSeed, cityOriginX - 1, cityOriginZ - 1, config);
        long seed1 = rng.nextLong();
        long seed2 = rng.nextLong();

        return List.of(
                new ChestInfo(cityOriginX - 1, 60, cityOriginZ - 1, seed1, "chests/end_city_treasure"),
                new ChestInfo(cityOriginX + 1, 60, cityOriginZ + 1, seed2, "chests/end_city_treasure")
        );
    }

    private static List<ChestInfo> getFortressChests(long worldSeed, int blockX, int blockZ) {
        var config = PopulationSeedUtils.getSaltConfig(StructureType.NETHER_FORTRESS);
        WorldgenRandom rng = createLootRngAtChest(worldSeed, blockX + 8, blockZ + 8, config);
        return List.of(new ChestInfo(blockX + 8, 60, blockZ + 8,
                rng.nextLong(), "chests/nether_bridge"));
    }

    private static List<ChestInfo> getStrongholdChests(long worldSeed, int blockX, int blockZ) {
        var config = PopulationSeedUtils.getSaltConfig(StructureType.STRONGHOLD);
        WorldgenRandom rng = createLootRngAtChest(worldSeed, blockX + 8, blockZ + 8, config);
        rng.nextLong();
        return List.of(new ChestInfo(blockX + 8, -40, blockZ + 8,
                rng.nextLong(), "chests/stronghold_corridor"));
    }

    private static List<ChestInfo> getBuriedTreasureChests(long worldSeed, int blockX, int blockZ,
                                                           SeedSourceUtils source) {
        var config = PopulationSeedUtils.getSaltConfig(StructureType.BURIED_TREASURE);
        WorldgenRandom rng = createLootRngAtChest(worldSeed, blockX, blockZ, config);

        int chestY = 64;
        if (source != null) {
            int sy = source.getSurfaceY(blockX, blockZ);
            if (sy != Integer.MIN_VALUE) chestY = sy - 1;
        }
        return List.of(new ChestInfo(blockX, chestY, blockZ,
                rng.nextLong(), "chests/buried_treasure"));
    }

    private static void skipRngCalls(WorldgenRandom rng, int count) {
        for (int i = 0; i < count; i++) {
            rng.nextInt();
        }
    }

    public record ChestInfo(int blockX, int blockY, int blockZ, long lootSeed, String lootTable) {
    }
}

