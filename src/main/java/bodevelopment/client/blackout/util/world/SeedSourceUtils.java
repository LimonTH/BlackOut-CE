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

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.*;

import java.util.Set;

public class SeedSourceUtils {
    public static final int OPTIMAL_Y = 64;
    private static final LevelHeightAccessor HEIGHT_ACCESSOR = LevelHeightAccessor.create(-64, 384);
    private static final Set<ResourceKey<Biome>> STRONGHOLD_BIASED_TO = Set.of(
            Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES,
            Biomes.DESERT, Biomes.FOREST, Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST,
            Biomes.DARK_FOREST, Biomes.PALE_GARDEN, Biomes.OLD_GROWTH_BIRCH_FOREST,
            Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.TAIGA,
            Biomes.SNOWY_TAIGA, Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU,
            Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST,
            Biomes.WINDSWEPT_SAVANNA, Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE,
            Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS,
            Biomes.MEADOW, Biomes.GROVE, Biomes.SNOWY_SLOPES,
            Biomes.FROZEN_PEAKS, Biomes.JAGGED_PEAKS, Biomes.STONY_PEAKS,
            Biomes.MUSHROOM_FIELDS, Biomes.DRIPSTONE_CAVES, Biomes.LUSH_CAVES
    );
    private static HolderLookup.Provider vanillaRegistries;
    private final RandomState randomState;
    private final Climate.Sampler sampler;
    private final Climate.ParameterList<ResourceKey<Biome>> parameters;
    private final boolean isEnd;
    private final boolean isNether;
    private final TheEndBiomeSource endBiomeSource;
    private final NoiseBasedChunkGenerator chunkGenerator;

    public SeedSourceUtils(long seed, ResourceKey<Level> dimension) {
        this.isEnd = dimension == Level.END;
        this.isNether = dimension == Level.NETHER;

        HolderLookup.Provider registries = getRegistries();

        ResourceKey<NoiseGeneratorSettings> noiseSettingsKey = NoiseGeneratorSettings.OVERWORLD;
        if (dimension == Level.NETHER) {
            noiseSettingsKey = NoiseGeneratorSettings.NETHER;
        } else if (dimension == Level.END) {
            noiseSettingsKey = NoiseGeneratorSettings.END;
        }

        this.randomState = RandomState.create(registries, noiseSettingsKey, seed);
        this.sampler = randomState.sampler();

        HolderGetter<Biome> biomeRegistry = registries.lookupOrThrow(Registries.BIOME);
        HolderGetter<NoiseGeneratorSettings> noiseSettingsRegistry = registries.lookupOrThrow(Registries.NOISE_SETTINGS);

        if (this.isEnd) {
            this.endBiomeSource = TheEndBiomeSource.create(biomeRegistry);
            this.parameters = null;
            this.chunkGenerator = null;
        } else if (this.isNether) {
            this.endBiomeSource = null;
            MultiNoiseBiomeSourceParameterList.Preset preset = MultiNoiseBiomeSourceParameterList.Preset.NETHER;
            this.parameters = MultiNoiseBiomeSourceParameterList.knownPresets().get(preset);
            this.chunkGenerator = null;
        } else {
            this.endBiomeSource = null;
            MultiNoiseBiomeSourceParameterList.Preset preset = MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD;
            this.parameters = MultiNoiseBiomeSourceParameterList.knownPresets().get(preset);
            if (this.parameters != null) {
                ResourceKey<MultiNoiseBiomeSourceParameterList> overworldKey =
                        ResourceKey.create(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST,
                                ResourceLocation.withDefaultNamespace("overworld"));
                Holder<MultiNoiseBiomeSourceParameterList> paramListHolder =
                        registries.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                                .getOrThrow(overworldKey);
                MultiNoiseBiomeSource biomeSource = new MultiNoiseBiomeSource(
                        com.mojang.datafixers.util.Either.right(paramListHolder)
                );
                this.chunkGenerator = new NoiseBasedChunkGenerator(
                        biomeSource,
                        noiseSettingsRegistry.getOrThrow(NoiseGeneratorSettings.OVERWORLD)
                );
            } else {
                this.chunkGenerator = null;
            }
        }
    }

    private static HolderLookup.Provider getRegistries() {
        if (vanillaRegistries == null) {
            vanillaRegistries = VanillaRegistries.createLookup();
        }
        return vanillaRegistries;
    }

    public ResourceKey<Biome> getBiome(int blockX, int blockZ) {
        if (this.isEnd) return getEndBiome(blockX, OPTIMAL_Y, blockZ);
        return getBiomeAt(blockX, 319, blockZ);
    }

    public ResourceKey<Biome> getBiomeAt(int blockX, int blockY, int blockZ) {
        if (this.isEnd) return getEndBiome(blockX, blockY, blockZ);
        int quartX = QuartPos.fromBlock(blockX);
        int quartY = QuartPos.fromBlock(blockY);
        int quartZ = QuartPos.fromBlock(blockZ);
        Climate.TargetPoint target = this.sampler.sample(quartX, quartY, quartZ);
        return this.parameters.findValue(target);
    }

    public int getBiomeColor(int blockX, int blockZ) {
        return BiomeColorMap.getColor(getBiome(blockX, blockZ));
    }

    private ResourceKey<Biome> getEndBiome(int blockX, int blockY, int blockZ) {
        int quartX = QuartPos.fromBlock(blockX);
        int quartY = QuartPos.fromBlock(blockY);
        int quartZ = QuartPos.fromBlock(blockZ);
        Holder<Biome> holder = this.endBiomeSource.getNoiseBiome(quartX, quartY, quartZ, this.sampler);
        return holder.unwrapKey().orElse(Biomes.THE_END);
    }

    public int getFirstOccupiedHeight(int blockX, int blockZ) {
        if (!isEnd) return Integer.MIN_VALUE;
        var density = this.randomState.router().finalDensity();
        for (int y = 256; y >= 0; y--) {
            if (density.compute(new DensityFunction.SinglePointContext(blockX, y, blockZ)) > 0.0) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    public boolean hasTerrainAtOrAbove60(int blockX, int blockZ) {
        return getFirstOccupiedHeight(blockX, blockZ) >= 60;
    }

    public boolean hasSolidTerrainAtY60(int blockX, int blockZ) {
        return hasTerrainAtOrAbove60(blockX, blockZ);
    }

    public boolean hasSolidTerrainAboveSeaLevel(int blockX, int blockZ) {
        if (isNether || isEnd) return true;
        if (this.randomState == null) return true;
        try {
            var density = this.randomState.router().finalDensity();
            for (int y = 90; y >= 60; y--) {
                if (density.compute(new DensityFunction.SinglePointContext(blockX, y, blockZ)) > 0.0) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    public boolean hasAreaAboveSeaLevel(int centerX, int centerZ, int halfWidth) {
        int[][] points = {{0, 0}, {halfWidth, halfWidth}, {halfWidth, -halfWidth},
                {-halfWidth, halfWidth}, {-halfWidth, -halfWidth}};
        for (int[] pt : points) {
            if (hasSolidTerrainAboveSeaLevel(centerX + pt[0], centerZ + pt[1]))
                return true;
        }
        return false;
    }

    public int getSurfaceY(int blockX, int blockZ) {
        if (isNether || isEnd) return 64;
        if (this.chunkGenerator != null) {
            try {

                return this.chunkGenerator.getBaseHeight(blockX, blockZ,
                        Heightmap.Types.WORLD_SURFACE_WG, HEIGHT_ACCESSOR, this.randomState);
            } catch (Exception e) {

            }
        }
        if (this.randomState == null) return 64;
        try {
            var density = this.randomState.router().finalDensity();
            for (int y = 400; y >= -64; y--) {
                if (density.compute(new DensityFunction.SinglePointContext(blockX, y, blockZ)) > 0.0) {
                    return y;
                }
            }
            return Integer.MIN_VALUE;
        } catch (Exception e) {
            return 64;
        }
    }

    public int getDensitySurfaceY(int blockX, int blockZ) {
        if (isNether || isEnd) return 64;
        if (this.randomState == null) return 64;
        try {
            var density = this.randomState.router().finalDensity();
            int lo = -64, hi = 90;
            if (density.compute(new DensityFunction.SinglePointContext(blockX, lo, blockZ)) <= 0.0) {
                return Integer.MIN_VALUE;
            }
            while (lo < hi) {
                int mid = (lo + hi + 1) >>> 1;
                if (density.compute(new DensityFunction.SinglePointContext(blockX, mid, blockZ)) > 0.0) {
                    lo = mid;
                } else {
                    hi = mid - 1;
                }
            }
            return lo;
        } catch (Exception e) {
            return 64;
        }
    }

    public RandomState getRandomState() {
        return this.randomState;
    }

    public int getHeight(int blockX, int blockZ) {
        return BiomeColorMap.getHeight(getBiome(blockX, blockZ));
    }

    public long getBiomeSurfaceData(int blockX, int blockZ) {
        return BiomeColorMap.getSurfaceData(getBiome(blockX, blockZ));
    }

    public int[] findNearestStrongholdChunk(int chunkX, int chunkZ) {
        int blockX = (chunkX << 4) + 8;
        int blockZ = (chunkZ << 4) + 8;
        int quartX = QuartPos.fromBlock(blockX);
        int quartZ = QuartPos.fromBlock(blockZ);

        int biomeY = 319;
        int quartRadius = QuartPos.fromBlock(112);

        ResourceKey<Biome> rawBiome = getBiomeAt(blockX, biomeY, blockZ);
        if (STRONGHOLD_BIASED_TO.contains(rawBiome)) {
            return new int[]{chunkX, chunkZ};
        }

        for (int dq = 1; dq <= quartRadius; dq++) {
            for (int dqz = -dq; dqz <= dq; dqz++) {
                boolean edgeZ = Math.abs(dqz) == dq;
                for (int dqx = -dq; dqx <= dq; dqx++) {
                    boolean edgeX = Math.abs(dqx) == dq;
                    if (!edgeX && !edgeZ) continue;
                    ResourceKey<Biome> biome = getBiomeAt(
                            QuartPos.toBlock(quartX + dqx), biomeY, QuartPos.toBlock(quartZ + dqz)
                    );
                    if (STRONGHOLD_BIASED_TO.contains(biome)) {
                        int foundBlock = QuartPos.toBlock(quartX + dqx);
                        int foundBlockZ = QuartPos.toBlock(quartZ + dqz);
                        return new int[]{foundBlock >> 4, foundBlockZ >> 4};
                    }
                }
            }
        }
        return new int[]{chunkX, chunkZ};
    }
}

