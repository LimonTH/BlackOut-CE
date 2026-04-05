package bodevelopment.client.blackout.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

public class SeedBiomeSource {
    private static HolderLookup.Provider vanillaRegistries;

    private final RandomState randomState;
    private final Climate.Sampler sampler;
    private final Climate.ParameterList<ResourceKey<Biome>> parameters;
    private final boolean isEnd;

    public SeedBiomeSource(long seed, ResourceKey<Level> dimension) {
        this.isEnd = dimension == Level.END;

        if (vanillaRegistries == null) {
            vanillaRegistries = VanillaRegistries.createLookup();
        }
        ResourceKey<NoiseGeneratorSettings> noiseSettings = NoiseGeneratorSettings.OVERWORLD;
        if (dimension == Level.NETHER) {
            noiseSettings = NoiseGeneratorSettings.NETHER;
        } else if (dimension == Level.END) {
            noiseSettings = NoiseGeneratorSettings.END;
        }

        this.randomState = RandomState.create(vanillaRegistries, noiseSettings, seed);
        this.sampler = randomState.sampler();

        if (this.isEnd) {
            this.parameters = null;
        } else {
            MultiNoiseBiomeSourceParameterList.Preset preset = (dimension == Level.NETHER)
                    ? MultiNoiseBiomeSourceParameterList.Preset.NETHER
                    : MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD;
            this.parameters = MultiNoiseBiomeSourceParameterList.knownPresets().get(preset);
        }
    }

    public ResourceKey<Biome> getBiome(int blockX, int blockZ) {
        if (this.isEnd) return getEndBiome(blockX, blockZ);
        int maxY = 319;
        return getBiomeAt(blockX, maxY, blockZ);
    }

    public ResourceKey<Biome> getBiomeAt(int blockX, int blockY, int blockZ) {
        if (this.isEnd) return getEndBiome(blockX, blockZ);
        int quartX = QuartPos.fromBlock(blockX);
        int quartY = QuartPos.fromBlock(blockY);
        int quartZ = QuartPos.fromBlock(blockZ);
        Climate.TargetPoint target = this.sampler.sample(quartX, quartY, quartZ);
        return this.parameters.findValue(target);
    }

    public int getBiomeColor(int blockX, int blockZ) {
        return BiomeColorMap.getColor(getBiome(blockX, blockZ));
    }

    private ResourceKey<Biome> getEndBiome(int blockX, int blockZ) {
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        if ((long) chunkX * chunkX + (long) chunkZ * chunkZ <= 4096L) {
            return Biomes.THE_END;
        }

        DensityFunction.SinglePointContext ctx = new DensityFunction.SinglePointContext(blockX, 64, blockZ);
        double erosion = this.randomState.router().erosion().compute(ctx);

        if (erosion > 0.25) return Biomes.END_HIGHLANDS;
        if (erosion >= -0.0625) return Biomes.END_MIDLANDS;
        if (erosion < -0.21875) return Biomes.SMALL_END_ISLANDS;
        return Biomes.END_BARRENS;
    }

    public boolean hasSolidTerrainAt(int blockX, int blockY, int blockZ) {
        if (!isEnd) return true;

        DensityFunction.SinglePointContext ctx = new DensityFunction.SinglePointContext(blockX, blockY, blockZ);
        return this.randomState.router().finalDensity().compute(ctx) > 0;
    }

    public int getHeight(int blockX, int blockZ) {
        if (!isEnd) return 64;

        DensityFunction.SinglePointContext ctx = new DensityFunction.SinglePointContext(blockX, 64, blockZ);
        double erosion = this.randomState.router().erosion().compute(ctx);
        if (erosion > 0.25) {
            return 80 + (int)(erosion * 40);
        } else if (erosion >= -0.0625) {
            return 65 + (int)((erosion + 0.0625) * 60);
        } else if (erosion < -0.21875) {
            return 20 + (int)((erosion + 0.21875) * 80);
        } else {
            return 45 + (int)((erosion + 0.21875) * 60);
        }
    }
}