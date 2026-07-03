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
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

@PublicAPI
public final class PopulationSeedUtils {
    private static final StructureSaltConfig SS_BURIED_TREASURE_1194 = new StructureSaltConfig(3, 0);
    private static final StructureSaltConfig SS_BASTION_1194 = new StructureSaltConfig(4, 0);
    private static final StructureSaltConfig SS_DESERT_PYRAMID_1192 = new StructureSaltConfig(4, 1);
    private static final StructureSaltConfig SS_IGLOO_1192 = new StructureSaltConfig(4, 3);
    private static final StructureSaltConfig SS_JUNGLE_PYRAMID_1194 = new StructureSaltConfig(4, 4);
    private static final StructureSaltConfig SS_PILLAGER_OUTPOST_1194 = new StructureSaltConfig(4, 9);
    private static final StructureSaltConfig SS_SHIPWRECK_1194 = new StructureSaltConfig(4, 17);
    private static final StructureSaltConfig SS_SHIPWRECK_BEACHED_1194 = new StructureSaltConfig(4, 18);
    private static final StructureSaltConfig SS_RUINED_PORTAL_1194 = new StructureSaltConfig(4, 10);
    private static final StructureSaltConfig SS_RUINED_PORTAL_DESERT_1194 = new StructureSaltConfig(4, 11);
    private static final StructureSaltConfig SS_RUINED_PORTAL_JUNGLE_1194 = new StructureSaltConfig(4, 12);
    private static final StructureSaltConfig SS_RUINED_PORTAL_SWAMP_1194 = new StructureSaltConfig(4, 16);
    private static final StructureSaltConfig SS_RUINED_PORTAL_MOUNTAIN_1194 = new StructureSaltConfig(4, 13);
    private static final StructureSaltConfig SS_RUINED_PORTAL_OCEAN_1194 = new StructureSaltConfig(4, 15);
    private static final StructureSaltConfig SS_RUINED_PORTAL_NETHER_1194 = new StructureSaltConfig(4, 14);
    private static final StructureSaltConfig SS_STRONGHOLD_1194 = new StructureSaltConfig(4, 19);
    private static final StructureSaltConfig SS_MINESHAFT_1194 = new StructureSaltConfig(3, 1);
    private static final StructureSaltConfig SS_MINESHAFT_MESA_1194 = new StructureSaltConfig(3, 2);
    private static final StructureSaltConfig SS_FORTRESS_1194 = new StructureSaltConfig(7, 1);
    private static final StructureSaltConfig SS_END_CITY_1194 = new StructureSaltConfig(4, 2);
    private static final StructureSaltConfig SS_ANCIENT_CITY = new StructureSaltConfig(4, 2);
    private static final StructureSaltConfig SS_MANSION = new StructureSaltConfig(4, 2);

    private PopulationSeedUtils() {
    }

    @PublicAPI
    public static long getPopulationSeed(long worldSeed, int chunkBlockX, int chunkBlockZ) {
        WorldgenRandom rng = createXoroshiroRng();
        rng.setSeed(worldSeed);
        long a = rng.nextLong() | 1L;
        long b = rng.nextLong() | 1L;
        return (long) chunkBlockX * a + (long) chunkBlockZ * b ^ worldSeed;
    }

    @PublicAPI
    public static WorldgenRandom createLootRng(long populationSeed, StructureSaltConfig config) {

        WorldgenRandom rng = createXoroshiroRng();
        rng.setSeed(populationSeed + (long) config.decoratorIndex() + 10000L * (long) config.generationStep());
        return rng;
    }

    @PublicAPI
    public static WorldgenRandom createXoroshiroRng() {
        return new WorldgenRandom(new XoroshiroRandomSource(0L));
    }

    @PublicAPI
    public static WorldgenRandom createLegacyRng() {
        return new WorldgenRandom(new LegacyRandomSource(0L));
    }

    @PublicAPI
    public static StructureSaltConfig getSaltConfig(
            bodevelopment.client.blackout.module.modules.visual.world.SeedSearcher.StructureType type) {
        return switch (type) {
            case DESERT_PYRAMID -> SS_DESERT_PYRAMID_1192;
            case JUNGLE_TEMPLE -> SS_JUNGLE_PYRAMID_1194;
            case IGLOO -> SS_IGLOO_1192;
            case BASTION_REMNANT -> SS_BASTION_1194;
            case RUINED_PORTAL -> SS_RUINED_PORTAL_1194;
            case STRONGHOLD -> SS_STRONGHOLD_1194;
            case MINESHAFT -> SS_MINESHAFT_1194;
            case NETHER_FORTRESS -> SS_FORTRESS_1194;
            case END_CITY -> SS_END_CITY_1194;
            case BURIED_TREASURE -> SS_BURIED_TREASURE_1194;
            case SHIPWRECK -> SS_SHIPWRECK_1194;
            case OCEAN_RUIN -> SS_SHIPWRECK_1194;
            case PILLAGER_OUTPOST -> SS_PILLAGER_OUTPOST_1194;
            case ANCIENT_CITY -> SS_ANCIENT_CITY;
            case WOODLAND_MANSION -> SS_MANSION;
            default -> null;
        };
    }

    public record StructureSaltConfig(int generationStep, int decoratorIndex) {
    }
}

