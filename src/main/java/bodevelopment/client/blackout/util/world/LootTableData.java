package bodevelopment.client.blackout.util.world;

import bodevelopment.client.blackout.annotations.PublicAPI;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@PublicAPI
public final class LootTableData {
    private static final Map<String, CompiledLootTable> TABLES = new HashMap<>();

    static {
        register("chests/desert_pyramid", List.of(
                pool(2, 4, List.of(
                        entry("minecraft:diamond", 5, 1, 3),
                        entry("minecraft:iron_ingot", 15, 1, 5),
                        entry("minecraft:gold_ingot", 15, 2, 7),
                        entry("minecraft:emerald", 15, 1, 3),
                        entry("minecraft:bone", 25, 4, 6),
                        entry("minecraft:spider_eye", 25, 1, 3),
                        entry("minecraft:rotten_flesh", 25, 3, 7),
                        entry("minecraft:saddle", 20),
                        entry("minecraft:iron_horse_armor", 15),
                        entry("minecraft:golden_horse_armor", 10),
                        entry("minecraft:diamond_horse_armor", 5),
                        entry("minecraft:book", 20),
                        entry("minecraft:golden_apple", 20),
                        entry("minecraft:enchanted_golden_apple", 2),
                        emptyEntry(15)
                )),
                pool(4, 4, List.of(
                        entry("minecraft:bone", 10, 1, 8),
                        entry("minecraft:gunpowder", 10, 1, 8),
                        entry("minecraft:rotten_flesh", 10, 1, 8),
                        entry("minecraft:string", 10, 1, 8),
                        entry("minecraft:sand", 10, 1, 8)
                ))
        ));

        register("chests/ancient_city", List.of(
                pool(5, 10, List.of(
                        entry("minecraft:enchanted_golden_apple", 1),
                        entry("minecraft:music_disc_otherside", 1),
                        entry("minecraft:compass", 2, 1, 1),
                        entry("minecraft:sculk_catalyst", 2, 1, 2),
                        entry("minecraft:name_tag", 2),
                        entry("minecraft:book", 2),
                        entry("minecraft:leather_chestplate", 2),
                        entry("minecraft:leather_boots", 2),
                        entry("minecraft:leather_leggings", 2),
                        entry("minecraft:leather_helmet", 2),
                        entry("minecraft:diamond_leggings", 2),
                        entry("minecraft:diamond_chestplate", 2),
                        entry("minecraft:coal", 5, 3, 15),
                        entry("minecraft:bone", 5, 1, 4),
                        entry("minecraft:echo_shard", 4, 1, 3),
                        entry("minecraft:disc_fragment_5", 3, 1, 3),
                        entry("minecraft:enchanted_golden_apple", 1)
                ))
        ));

        register("chests/ancient_city_ice_box", List.of(
                pool(2, 6, List.of(
                        entry("minecraft:enchanted_golden_apple", 1),
                        entry("minecraft:suspicious_stew", 3),
                        entry("minecraft:golden_carrot", 4, 1, 4),
                        entry("minecraft:golden_apple", 3),
                        entry("minecraft:book", 1),
                        entry("minecraft:name_tag", 1)
                ))
        ));

        register("chests/bastion_treasure", List.of(
                pool(2, 6, List.of(
                        entry("minecraft:diamond_sword", 3),
                        entry("minecraft:diamond_chestplate", 3),
                        entry("minecraft:enchanted_golden_apple", 1),
                        entry("minecraft:ancient_debris", 2, 1, 2),
                        entry("minecraft:netherite_scrap", 2, 1, 2),
                        entry("minecraft:netherite_ingot", 1),
                        entry("minecraft:magma_cream", 3, 2, 5),
                        entry("minecraft:obsidian", 3, 2, 5),
                        entry("minecraft:crying_obsidian", 3, 1, 4),
                        entry("minecraft:gold_block", 3, 1, 2),
                        entry("minecraft:gold_ingot", 10, 4, 10),
                        entry("minecraft:iron_ingot", 10, 4, 10),
                        entry("minecraft:golden_sword", 3),
                        entry("minecraft:golden_chestplate", 3),
                        entry("minecraft:golden_helmet", 3),
                        entry("minecraft:golden_boots", 3),
                        entry("minecraft:golden_leggings", 3),
                        entry("minecraft:gilded_blackstone", 5, 1, 5),
                        entry("minecraft:chain", 5, 2, 10),
                        entry("minecraft:arrow", 5, 5, 25),
                        entry("minecraft:spectral_arrow", 5, 6, 12),
                        entry("minecraft:string", 3, 3, 8),
                        entry("minecraft:gold_nugget", 10, 2, 18),
                        emptyEntry(2)
                ))
        ));

        register("chests/bastion_other", List.of(
                pool(2, 4, List.of(
                        entry("minecraft:crossbow", 1),
                        entry("minecraft:book", 1),
                        entry("minecraft:enchanted_golden_apple", 1),
                        entry("minecraft:golden_apple", 2),
                        entry("minecraft:golden_sword", 1),
                        entry("minecraft:iron_sword", 3),
                        entry("minecraft:golden_chestplate", 1),
                        entry("minecraft:golden_boots", 1),
                        entry("minecraft:iron_chestplate", 2),
                        entry("minecraft:ancient_debris", 1, 1, 2),
                        entry("minecraft:netherite_scrap", 1),
                        entry("minecraft:gold_nugget", 20, 2, 15),
                        entry("minecraft:gold_ingot", 10, 1, 6),
                        entry("minecraft:iron_ingot", 5, 1, 4),
                        entry("minecraft:gilded_blackstone", 5, 1, 3),
                        entry("minecraft:crying_obsidian", 3, 1, 3),
                        entry("minecraft:magma_cream", 5, 1, 4),
                        entry("minecraft:string", 5, 3, 7),
                        entry("minecraft:arrow", 5, 5, 12),
                        entry("minecraft:spectral_arrow", 5, 5, 8),
                        emptyEntry(10)
                ))
        ));

        register("chests/ruined_portal", List.of(
                pool(4, 8, List.of(
                        entry("minecraft:enchanted_golden_apple", 1),
                        entry("minecraft:golden_apple", 3),
                        entry("minecraft:golden_sword", 6),
                        entry("minecraft:golden_axe", 6),
                        entry("minecraft:golden_pickaxe", 6),
                        entry("minecraft:golden_boots", 6),
                        entry("minecraft:golden_leggings", 6),
                        entry("minecraft:golden_chestplate", 6),
                        entry("minecraft:golden_helmet", 6),
                        entry("minecraft:gold_block", 3, 1, 1),
                        entry("minecraft:gold_ingot", 15, 1, 3),
                        entry("minecraft:gold_nugget", 30, 4, 12),
                        entry("minecraft:obsidian", 10, 1, 2),
                        entry("minecraft:flint_and_steel", 10),
                        entry("minecraft:fire_charge", 10, 1, 2),
                        entry("minecraft:arrow", 25, 2, 7),
                        entry("minecraft:bone", 25, 2, 7),
                        entry("minecraft:rotten_flesh", 25, 2, 7),
                        emptyEntry(20)
                ))
        ));

        register("chests/simple_dungeon", List.of(
                pool(1, 3, List.of(
                        entry("minecraft:saddle", 20),
                        entry("minecraft:book", 10),
                        entry("minecraft:enchanted_golden_apple", 1),
                        entry("minecraft:golden_apple", 10),
                        entry("minecraft:name_tag", 10),
                        entry("minecraft:iron_ingot", 10, 1, 4),
                        entry("minecraft:gold_ingot", 5, 1, 4),
                        entry("minecraft:bread", 10, 1, 4),
                        entry("minecraft:wheat", 10, 1, 4),
                        entry("minecraft:bucket", 5),
                        entry("minecraft:redstone", 5, 1, 4),
                        entry("minecraft:coal", 10, 1, 4),
                        entry("minecraft:melon_seeds", 10, 2, 4),
                        entry("minecraft:pumpkin_seeds", 10, 2, 4),
                        entry("minecraft:beetroot_seeds", 10, 2, 4),
                        emptyEntry(25)
                ))
        ));

        register("chests/abandoned_mineshaft", List.of(
                pool(1, 3, List.of(
                        entry("minecraft:enchanted_golden_apple", 1),
                        entry("minecraft:golden_apple", 5),
                        entry("minecraft:iron_ingot", 10, 1, 3),
                        entry("minecraft:gold_ingot", 5, 1, 3),
                        entry("minecraft:redstone", 5, 1, 5),
                        entry("minecraft:lapis_lazuli", 5, 1, 5),
                        entry("minecraft:diamond", 1),
                        entry("minecraft:coal", 10, 1, 4),
                        entry("minecraft:bread", 15, 1, 3),
                        entry("minecraft:melon_seeds", 10, 2, 4),
                        entry("minecraft:pumpkin_seeds", 10, 2, 4),
                        entry("minecraft:beetroot_seeds", 10, 2, 4),
                        entry("minecraft:name_tag", 5),
                        entry("minecraft:rail", 20, 4, 8),
                        entry("minecraft:powered_rail", 5, 1, 4),
                        entry("minecraft:detector_rail", 5, 1, 4),
                        entry("minecraft:activator_rail", 5, 1, 4),
                        emptyEntry(15)
                ))
        ));

        register("chests/woodland_mansion", List.of(
                pool(1, 3, List.of(
                        entry("minecraft:enchanted_golden_apple", 1),
                        entry("minecraft:golden_apple", 5),
                        entry("minecraft:book", 5),
                        entry("minecraft:name_tag", 6),
                        entry("minecraft:diamond", 3, 1, 3),
                        entry("minecraft:iron_ingot", 10, 1, 5),
                        entry("minecraft:gold_ingot", 10, 1, 3),
                        entry("minecraft:lead", 10),
                        entry("minecraft:chain", 10, 2, 10),
                        entry("minecraft:bread", 15, 1, 4),
                        entry("minecraft:wheat", 15, 1, 4),
                        entry("minecraft:bone", 10, 1, 8),
                        entry("minecraft:string", 10, 1, 8),
                        entry("minecraft:ender_pearl", 3),
                        emptyEntry(20)
                ))
        ));

        register("chests/stronghold_corridor", List.of(
                pool(1, 3, List.of(
                        entry("minecraft:ender_pearl", 10),
                        entry("minecraft:diamond", 3, 1, 3),
                        entry("minecraft:iron_ingot", 10, 1, 5),
                        entry("minecraft:gold_ingot", 5, 1, 3),
                        entry("minecraft:redstone", 5, 1, 5),
                        entry("minecraft:bread", 15, 1, 3),
                        entry("minecraft:apple", 15, 1, 3),
                        entry("minecraft:iron_pickaxe", 5),
                        entry("minecraft:iron_sword", 5),
                        entry("minecraft:iron_chestplate", 5),
                        entry("minecraft:iron_helmet", 5),
                        entry("minecraft:iron_leggings", 5),
                        entry("minecraft:iron_boots", 5),
                        entry("minecraft:golden_apple", 1),
                        entry("minecraft:saddle", 3),
                        entry("minecraft:iron_horse_armor", 1),
                        entry("minecraft:golden_horse_armor", 1),
                        entry("minecraft:diamond_horse_armor", 1),
                        entry("minecraft:book", 10),
                        emptyEntry(15)
                ))
        ));

        register("chests/stronghold_library", List.of(
                pool(1, 4, List.of(
                        entry("minecraft:book", 20),
                        entry("minecraft:book", 10),
                        entry("minecraft:book", 10),
                        entry("minecraft:paper", 20, 2, 7),
                        entry("minecraft:compass", 1),
                        emptyEntry(5)
                ))
        ));

        register("chests/end_city_treasure", List.of(
                pool(2, 6, List.of(
                        entry("minecraft:diamond", 5, 2, 7),
                        entry("minecraft:diamond_sword", 1),
                        entry("minecraft:diamond_chestplate", 1),
                        entry("minecraft:diamond_leggings", 1),
                        entry("minecraft:diamond_boots", 1),
                        entry("minecraft:diamond_helmet", 1),
                        entry("minecraft:iron_ingot", 10, 4, 8),
                        entry("minecraft:gold_ingot", 10, 2, 7),
                        entry("minecraft:emerald", 10, 2, 6),
                        entry("minecraft:beetroot_seeds", 10, 1, 10),
                        entry("minecraft:saddle", 3),
                        entry("minecraft:iron_horse_armor", 1),
                        entry("minecraft:golden_horse_armor", 1),
                        entry("minecraft:diamond_horse_armor", 1),
                        emptyEntry(15)
                ))
        ));

        register("chests/jungle_temple", List.of(
                pool(2, 6, List.of(
                        entry("minecraft:diamond", 3, 1, 3),
                        entry("minecraft:iron_ingot", 10, 1, 5),
                        entry("minecraft:gold_ingot", 15, 1, 3),
                        entry("minecraft:emerald", 5, 1, 3),
                        entry("minecraft:bone", 10, 4, 6),
                        entry("minecraft:rotten_flesh", 10, 3, 7),
                        entry("minecraft:saddle", 5),
                        entry("minecraft:iron_horse_armor", 3),
                        entry("minecraft:golden_horse_armor", 2),
                        entry("minecraft:diamond_horse_armor", 1),
                        entry("minecraft:book", 5),
                        entry("minecraft:golden_apple", 5),
                        emptyEntry(25)
                ))
        ));

        register("chests/igloo_chest", List.of(
                pool(2, 8, List.of(
                        entry("minecraft:apple", 15, 1, 3),
                        entry("minecraft:bread", 15, 1, 3),
                        entry("minecraft:golden_apple", 1),
                        entry("minecraft:coal", 15, 1, 4),
                        entry("minecraft:iron_ingot", 10, 1, 5),
                        entry("minecraft:gold_ingot", 5, 1, 3),
                        entry("minecraft:emerald", 5),
                        entry("minecraft:bone", 10, 1, 4),
                        entry("minecraft:rotten_flesh", 10, 1, 4),
                        entry("minecraft:string", 10, 1, 4),
                        entry("minecraft:arrow", 10, 1, 4),
                        emptyEntry(30)
                ))
        ));

        register("chests/nether_bridge", List.of(
                pool(2, 4, List.of(
                        entry("minecraft:diamond", 3, 1, 3),
                        entry("minecraft:iron_ingot", 10, 1, 5),
                        entry("minecraft:gold_ingot", 15, 1, 3),
                        entry("minecraft:golden_sword", 5),
                        entry("minecraft:golden_chestplate", 5),
                        entry("minecraft:flint_and_steel", 10),
                        entry("minecraft:nether_wart", 10, 3, 7),
                        entry("minecraft:string", 10, 1, 5),
                        entry("minecraft:glowstone_dust", 10, 1, 5),
                        entry("minecraft:blaze_rod", 5, 1, 2),
                        entry("minecraft:magma_cream", 5, 1, 3),
                        entry("minecraft:ender_pearl", 5, 1, 3),
                        entry("minecraft:book", 5),
                        emptyEntry(25)
                ))
        ));

        register("chests/buried_treasure", List.of(
                pool(1, 5, List.of(
                        entry("minecraft:iron_ingot", 20, 1, 4),
                        entry("minecraft:gold_ingot", 10, 1, 4),
                        entry("minecraft:diamond", 5, 1, 2),
                        entry("minecraft:emerald", 5, 1, 4),
                        entry("minecraft:book", 5)
                )),
                pool(1, 1, List.of(
                        entry("minecraft:heart_of_the_sea", 1)
                ))
        ));

        register("chests/shipwreck_treasure", List.of(
                pool(3, 6, List.of(
                        entry("minecraft:iron_ingot", 90, 1, 5),
                        entry("minecraft:gold_ingot", 10, 1, 5),
                        entry("minecraft:emerald", 40, 1, 5),
                        entry("minecraft:diamond", 5),
                        entry("minecraft:experience_bottle", 5)
                )),
                pool(2, 5, List.of(
                        entry("minecraft:iron_nugget", 50, 1, 10),
                        entry("minecraft:gold_nugget", 10, 1, 10),
                        entry("minecraft:lapis_lazuli", 20, 1, 10)
                )),
                pool(1, 1, List.of(
                        emptyEntry(5),
                        entry("minecraft:coast_armor_trim_smithing_template", 1, 2, 2)
                ))
        ));
    }

    private LootTableData() {
    }

    @PublicAPI
    public static CompiledLootTable getTable(String path) {

        String normalized = path.startsWith("minecraft:") ? path.substring(10) : path;
        return TABLES.get(normalized);
    }

    @PublicAPI
    public static String getLootTablePath(
            bodevelopment.client.blackout.module.modules.visual.world.SeedSearcher.StructureType type) {
        return switch (type) {
            case DESERT_PYRAMID -> "chests/desert_pyramid";
            case ANCIENT_CITY -> "chests/ancient_city";
            case BASTION_REMNANT -> "chests/bastion_treasure";
            case RUINED_PORTAL -> "chests/ruined_portal";
            case DUNGEON -> "chests/simple_dungeon";
            case MINESHAFT -> "chests/abandoned_mineshaft";
            case WOODLAND_MANSION -> "chests/woodland_mansion";
            case STRONGHOLD -> "chests/stronghold_corridor";
            case END_CITY -> "chests/end_city_treasure";
            case JUNGLE_TEMPLE -> "chests/jungle_temple";
            case IGLOO -> "chests/igloo_chest";
            case NETHER_FORTRESS -> "chests/nether_bridge";
            case BURIED_TREASURE -> "chests/buried_treasure";
            case SHIPWRECK -> "chests/shipwreck_treasure";
            default -> null;
        };
    }

    @PublicAPI
    public static boolean canHaveEnchantedApple(
            bodevelopment.client.blackout.module.modules.visual.world.SeedSearcher.StructureType type) {
        return switch (type) {
            case DESERT_PYRAMID, ANCIENT_CITY, BASTION_REMNANT,
                 RUINED_PORTAL, DUNGEON, MINESHAFT, WOODLAND_MANSION -> true;
            default -> false;
        };
    }

    private static void register(String path, List<LootPool> pools) {
        TABLES.put(path, new CompiledLootTable("minecraft:" + path, Collections.unmodifiableList(pools)));
    }

    private static LootEntry entry(String item, int weight) {
        return new LootEntry(item, weight, 1, 1);
    }

    private static LootEntry entry(String item, int weight, int min, int max) {
        return new LootEntry(item, weight, min, max);
    }

    private static LootEntry emptyEntry(int weight) {
        return new LootEntry("minecraft:empty", weight, 0, 0);
    }

    private static LootPool pool(int minRolls, int maxRolls, List<LootEntry> entries) {
        return new LootPool(minRolls, maxRolls, Collections.unmodifiableList(entries));
    }

    public record LootEntry(String itemId, int weight, int minCount, int maxCount) {
        public LootEntry(String itemId, int weight) {
            this(itemId, weight, 1, 1);
        }
    }

    public record LootPool(int minRolls, int maxRolls, List<LootEntry> entries) {
        public int totalWeight() {
            int sum = 0;
            for (LootEntry e : entries) sum += e.weight;
            return sum;
        }
    }

    public record CompiledLootTable(String lootTablePath, List<LootPool> pools) {
    }
}

