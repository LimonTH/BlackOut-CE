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

import bodevelopment.client.blackout.annotations.PublicAPI;
import bodevelopment.client.blackout.module.modules.visual.world.SeedSearcher.FoundStructure;
import bodevelopment.client.blackout.module.modules.visual.world.SeedSearcher.StructureType;
import bodevelopment.client.blackout.util.world.LootTableData.CompiledLootTable;
import bodevelopment.client.blackout.util.world.LootTableData.LootEntry;
import bodevelopment.client.blackout.util.world.LootTableData.LootPool;
import bodevelopment.client.blackout.util.world.StructurePieceSimulation.ChestInfo;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

import java.util.*;

@PublicAPI
public final class LootSimulationEngine {
    private LootSimulationEngine() {
    }

    @PublicAPI
    public static LootResult simulateForItem(FoundStructure structure, long worldSeed,
                                             SeedSourceUtils source, String targetItem) {
        return simulateForItems(structure, worldSeed, source, Set.of(targetItem));
    }

    @PublicAPI
    public static LootResult simulateForItems(FoundStructure structure, long worldSeed,
                                              SeedSourceUtils source, Set<String> targetItems) {
        if (targetItems == null || targetItems.isEmpty()) return LootResult.EMPTY;

        List<ChestInfo> chestInfos = StructurePieceSimulation.getChests(
                structure.type(), worldSeed, structure.blockX(), structure.blockZ(), source);

        if (chestInfos.isEmpty()) return LootResult.EMPTY;

        List<int[]> foundPositions = new ArrayList<>();
        Set<String> foundItems = new HashSet<>();
        List<int[]> allPositions = new ArrayList<>();
        List<Long> allSeeds = new ArrayList<>();

        for (ChestInfo chest : chestInfos) {

            RandomSource rng = new XoroshiroRandomSource(chest.lootSeed());

            String tablePath = chest.lootTable();
            CompiledLootTable table = LootTableData.getTable(tablePath);
            if (table == null) continue;

            boolean chestHasTarget = false;
            for (LootPool pool : table.pools()) {
                int rolls = rollCount(rng, pool.minRolls(), pool.maxRolls());
                for (int i = 0; i < rolls; i++) {
                    String rolledItem = rollEntry(rng, pool);
                    if (rolledItem == null) continue;
                    if (targetItems.contains(rolledItem)) {
                        chestHasTarget = true;
                        foundItems.add(rolledItem);
                    }
                }
            }

            int[] pos = new int[]{chest.blockX(), chest.blockY(), chest.blockZ(), chestHasTarget ? 1 : 0};
            allPositions.add(pos);
            allSeeds.add(chest.lootSeed());

            if (chestHasTarget) {
                foundPositions.add(new int[]{chest.blockX(), chest.blockY(), chest.blockZ()});
            }
        }

        return new LootResult(!foundPositions.isEmpty(), foundPositions, foundItems,
                chestInfos.size(), allPositions, allSeeds);
    }

    @PublicAPI
    public static LootResult simulateForApples(FoundStructure structure, long worldSeed,
                                               SeedSourceUtils source) {
        return simulateForItem(structure, worldSeed, source, "minecraft:enchanted_golden_apple");
    }

    @PublicAPI
    public static Set<String> simulateSingleChest(StructureType type, long worldSeed,
                                                  int chestX, int chestY, int chestZ) {
        PopulationSeedUtils.StructureSaltConfig config = PopulationSeedUtils.getSaltConfig(type);
        if (config == null) return Set.of();

        long populationSeed = PopulationSeedUtils.getPopulationSeed(worldSeed, chestX & ~15, chestZ & ~15);
        net.minecraft.world.level.levelgen.WorldgenRandom rng = PopulationSeedUtils.createLootRng(populationSeed, config);
        long lootSeed = rng.nextLong();

        RandomSource lootRng = new XoroshiroRandomSource(lootSeed);

        String tablePath = LootTableData.getLootTablePath(type);
        if (tablePath == null) return Set.of();
        CompiledLootTable table = LootTableData.getTable(tablePath);
        if (table == null) return Set.of();

        Set<String> items = new HashSet<>();
        for (LootPool pool : table.pools()) {
            int rolls = rollCount(lootRng, pool.minRolls(), pool.maxRolls());
            for (int i = 0; i < rolls; i++) {
                String item = rollEntry(lootRng, pool);
                if (item != null) items.add(item);
            }
        }
        return items;
    }

    private static int rollCount(RandomSource rng, int min, int max) {
        if (min >= max) return min;
        return min + rng.nextInt(max - min + 1);
    }

    private static String rollEntry(RandomSource rng, LootPool pool) {
        List<LootEntry> entries = pool.entries();
        int totalWeight = pool.totalWeight();
        if (totalWeight <= 0 || entries.isEmpty()) return null;
        int roll = rng.nextInt(totalWeight);
        int cumulative = 0;
        for (LootEntry entry : entries) {
            cumulative += entry.weight();
            if (roll < cumulative) return entry.itemId();
        }
        return null;
    }

    public record LootResult(boolean itemFound, List<int[]> chestPositions,
                             Set<String> foundItemIds, int totalChests,
                             List<int[]> allCheckedPositions,
                             List<Long> allCheckedSeeds) {
        public static final LootResult EMPTY = new LootResult(false, List.of(), Set.of(), 0, List.of(), List.of());

        public LootResult {
            chestPositions = Collections.unmodifiableList(chestPositions);
            foundItemIds = Collections.unmodifiableSet(foundItemIds);
            allCheckedPositions = Collections.unmodifiableList(allCheckedPositions);
            allCheckedSeeds = Collections.unmodifiableList(allCheckedSeeds);
        }
    }
}

