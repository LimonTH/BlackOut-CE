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

package bodevelopment.client.blackout.module.modules.visual.world.SeedSearcher;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.awt.*;
import java.util.Set;

public enum StructureType {
    VILLAGE("Village", 34, 8, 10387312, false, 1.0F,
            new Color(0, 200, 0),
            biomes(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW, Biomes.DESERT, Biomes.SAVANNA,
                    Biomes.SNOWY_PLAINS, Biomes.TAIGA, Biomes.SNOWY_TAIGA),
            "textures/map/structures/village/village_normal.png"),

    DESERT_PYRAMID("Desert Pyramid", 32, 8, 14357617, false, 1.0F,
            new Color(220, 180, 50),
            biomes(Biomes.DESERT),
            "textures/map/structures/desert_temple.png"),

    JUNGLE_TEMPLE("Jungle Temple", 32, 8, 14357619, false, 1.0F,
            new Color(50, 180, 50),
            biomes(Biomes.JUNGLE, Biomes.BAMBOO_JUNGLE),
            "textures/map/structures/jungle_temple.png"),

    SWAMP_HUT("Swamp Hut", 32, 8, 14357620, false, 1.0F,
            new Color(100, 140, 60),
            biomes(Biomes.SWAMP),
            "textures/map/structures/witch_hut.png"),

    IGLOO("Igloo", 32, 8, 14357618, false, 1.0F,
            new Color(180, 220, 255),
            biomes(Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.SNOWY_SLOPES),
            "textures/map/structures/igloo/igloo_without_basement.png"),

    OCEAN_MONUMENT("Ocean Monument", 32, 5, 10387313, true, 1.0F,
            new Color(0, 150, 200),
            biomes(Biomes.DEEP_OCEAN, Biomes.DEEP_COLD_OCEAN,
                    Biomes.DEEP_FROZEN_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN),
            "textures/map/structures/monument.png"),

    WOODLAND_MANSION("Woodland Mansion", 80, 20, 10387319, true, 1.0F,
            new Color(140, 80, 40),
            biomes(Biomes.DARK_FOREST),
            "textures/map/structures/mansion.png"),

    PILLAGER_OUTPOST("Pillager Outpost", 32, 8, 165745296, false, 0.2F,
            new Color(160, 160, 160),
            biomes(Biomes.PLAINS, Biomes.MEADOW, Biomes.DESERT, Biomes.SAVANNA,
                    Biomes.SNOWY_PLAINS, Biomes.TAIGA, Biomes.SUNFLOWER_PLAINS,
                    Biomes.CHERRY_GROVE, Biomes.GROVE, Biomes.SNOWY_TAIGA),
            "textures/map/structures/outpost.png"),

    ANCIENT_CITY("Ancient City", 24, 8, 20083232, false, 1.0F,
            new Color(30, 50, 80),
            biomes(Biomes.DEEP_DARK),
            "textures/map/structures/ancient_city.png"),

    TRIAL_CHAMBERS("Trial Chambers", 34, 12, 94251327, false, 1.0F,
            new Color(200, 100, 0),
            null,
            "textures/map/structures/trial_chamber.png"),

    TRAIL_RUINS("Trail Ruins", 34, 8, 83469867, false, 1.0F,
            new Color(180, 130, 80),
            biomes(Biomes.TAIGA, Biomes.SNOWY_TAIGA,
                    Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA,
                    Biomes.JUNGLE, Biomes.OLD_GROWTH_BIRCH_FOREST),
            "textures/map/structures/trail_ruins.png"),

    RUINED_PORTAL("Ruined Portal", 40, 15, 34222645, false, 1.0F,
            new Color(160, 50, 200),
            null,
            "textures/map/structures/ruined_portal.png"),

    SHIPWRECK("Shipwreck", 24, 4, 165745295, false, 1.0F,
            new Color(100, 80, 60),
            biomes(Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.COLD_OCEAN,
                    Biomes.DEEP_COLD_OCEAN, Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN,
                    Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.WARM_OCEAN,
                    Biomes.BEACH, Biomes.SNOWY_BEACH),
            "textures/map/structures/shipwreck.png"),

    OCEAN_RUIN("Ocean Ruin", 20, 8, 14357621, false, 1.0F,
            new Color(60, 120, 160),
            biomes(Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.COLD_OCEAN,
                    Biomes.DEEP_COLD_OCEAN, Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN,
                    Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.WARM_OCEAN),
            "textures/map/structures/ocean_ruins/ocean_ruins_small.png"),

    DESERT_WELL("Desert Well", 0, 0, 40002, false, 0.001F,
            new Color(180, 140, 80),
            biomes(Biomes.DESERT),
            "textures/map/structures/desert_well.png"),

    MINESHAFT("Mineshaft", 0, 0, 0, false, 0.004F,
            new Color(160, 130, 90),
            null,
            "textures/map/structures/mineshaft.png"),

    BURIED_TREASURE("Buried Treasure", 0, 0, 0, false, 0.01F,
            new Color(255, 215, 0),
            biomes(Biomes.BEACH, Biomes.SNOWY_BEACH, Biomes.STONY_SHORE),
            "textures/map/structures/treasure.png"),

    STRONGHOLD("Stronghold", 0, 0, 0, false, 1.0F,
            new Color(255, 50, 50),
            null,
            "textures/map/structures/stronghold.png"),

    NETHER_FORTRESS("Nether Fortress", 27, 4, 30084232, false, 1.0F,
            new Color(200, 50, 50),
            biomes(Biomes.NETHER_WASTES, Biomes.SOUL_SAND_VALLEY,
                    Biomes.WARPED_FOREST, Biomes.CRIMSON_FOREST, Biomes.BASALT_DELTAS),
            "textures/map/structures/nether_fortress.png"),

    BASTION_REMNANT("Bastion Remnant", 27, 4, 30084232, false, 1.0F,
            new Color(50, 50, 50),
            biomes(Biomes.NETHER_WASTES, Biomes.SOUL_SAND_VALLEY,
                    Biomes.WARPED_FOREST, Biomes.CRIMSON_FOREST),
            "textures/map/structures/bastion/bastion_treasure.png"),

    NETHER_FOSSIL("Nether Fossil", 2, 1, 14357921, true, 1.0F,
            new Color(180, 160, 110),
            biomes(Biomes.SOUL_SAND_VALLEY),
            "textures/map/structures/fossil.png"),

    END_CITY("End City", 20, 11, 10387313, true, 1.0F,
            new Color(200, 150, 255),
            biomes(Biomes.END_HIGHLANDS),
            "textures/map/structures/end_city/end_city_without_ship.png"),

    END_GATEWAY("End Gateway", 0, 0, 0, false, 1.0F,
            new Color(180, 100, 220),
            null,
            "textures/map/structures/end_gateway.png"),

    LUSH_CAVES("Lush Caves", 0, 0, 0, false, 1.0F,
            new Color(50, 140, 30),
            biomes(Biomes.LUSH_CAVES),
            "textures/map/structures/cave.png"),

    DRIPSTONE_CAVES("Dripstone Caves", 0, 0, 0, false, 1.0F,
            new Color(140, 106, 70),
            biomes(Biomes.DRIPSTONE_CAVES),
            "textures/map/structures/cave.png"),

    DUNGEON("Dungeon", 0, 0, 0, false, 1.0F,
            new Color(140, 100, 60),
            biomes(Biomes.LUSH_CAVES, Biomes.DRIPSTONE_CAVES, Biomes.DEEP_DARK),
            "textures/map/structures/dungeon/dungeon_skeleton.png"),

    GEODE("Geode", 0, 0, 0, false, 1.0F,
            new Color(120, 60, 200),
            biomes(Biomes.LUSH_CAVES),
            "textures/map/structures/geode.png"),

    LAVA_POOL_SURFACE("Lava Pool (Surface)", 0, 0, 0, false, 1.0F,
            new Color(200, 80, 20),
            null,
            "textures/map/structures/lava_pool/lava_pool_lake.png"),

    LAVA_POOL_CAVE("Lava Pool (Cave)", 0, 0, 0, false, 1.0F,
            new Color(200, 50, 10),
            biomes(Biomes.LUSH_CAVES, Biomes.DRIPSTONE_CAVES, Biomes.DEEP_DARK),
            "textures/map/structures/lava_pool/lava_pool_cave.png"),

    RAVINE("Ravine", 0, 0, 0, false, 1.0F,
            new Color(100, 80, 60),
            null,
            "textures/map/structures/ravine.png"),

    SPAWN("World Spawn", 0, 0, 0, false, 1.0F,
            new Color(255, 255, 255),
            null,
            "textures/map/spawn_point.png");

    public final String displayName;
    public final int spacing;
    public final int separation;
    public final int salt;
    public final boolean triangular;
    public final float frequency;
    public final Color mapColor;
    public final Set<ResourceKey<Biome>> validBiomes;
    public final String iconPath;

    StructureType(String displayName, int spacing, int separation, int salt,
                  boolean triangular, float frequency, Color mapColor,
                  Set<ResourceKey<Biome>> validBiomes, String iconPath) {
        this.displayName = displayName;
        this.spacing = spacing;
        this.separation = separation;
        this.salt = salt;
        this.triangular = triangular;
        this.frequency = frequency;
        this.mapColor = mapColor;
        this.validBiomes = validBiomes;
        this.iconPath = iconPath;
    }

    @SafeVarargs
    private static Set<ResourceKey<Biome>> biomes(ResourceKey<Biome>... keys) {
        return Set.of(keys);
    }

    public boolean isCaveBiome() {
        return this == LUSH_CAVES || this == DRIPSTONE_CAVES;
    }

    public boolean isFeature() {
        return this == DUNGEON || this == GEODE || this == LAVA_POOL_SURFACE
                || this == LAVA_POOL_CAVE || this == RAVINE;
    }

    public boolean isAlwaysVisible() {
        return this == STRONGHOLD || this == ANCIENT_CITY || this == WOODLAND_MANSION
                || this == END_CITY || this == NETHER_FORTRESS || this == BASTION_REMNANT
                || this == END_GATEWAY || isFeature() || this == GEODE;
    }

    public boolean isMinor() {
        return this == SHIPWRECK || this == OCEAN_RUIN || this == RUINED_PORTAL
                || this == TRAIL_RUINS || this == SWAMP_HUT || this == IGLOO
                || isCaveBiome();
    }
}

