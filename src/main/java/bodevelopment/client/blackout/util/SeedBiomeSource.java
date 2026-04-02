package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public class SeedBiomeSource {
    private static HolderLookup.Provider vanillaRegistries;

    // MultiNoise Overworld / Nether
    private final Climate.Sampler sampler;
    private final Climate.ParameterList<ResourceKey<Biome>> parameters;

    // End
    private final boolean isEnd;
    private SimplexNoise endNoise;

    public SeedBiomeSource(long seed, ResourceKey<Level> dimension) {
        this.isEnd = dimension == Level.END;

        if (this.isEnd) {
            this.endNoise = new SimplexNoise(new WorldgenRandom(new LegacyRandomSource(seed)));
            this.sampler = null;
            this.parameters = null;
            return;
        }

        if (vanillaRegistries == null) {
            vanillaRegistries = VanillaRegistries.createLookup();
        }

        ResourceKey<NoiseGeneratorSettings> noiseSettings = NoiseGeneratorSettings.OVERWORLD;
        MultiNoiseBiomeSourceParameterList.Preset preset = MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD;

        if (dimension == Level.NETHER) {
            noiseSettings = NoiseGeneratorSettings.NETHER;
            preset = MultiNoiseBiomeSourceParameterList.Preset.NETHER;
        }

        RandomState randomState = RandomState.create(vanillaRegistries, noiseSettings, seed);
        this.sampler = randomState.sampler();

        var presets = MultiNoiseBiomeSourceParameterList.knownPresets();
        this.parameters = presets.get(preset);
    }

    public ResourceKey<Biome> getBiome(int blockX, int blockZ) {
        if (this.isEnd) return getEndBiome(blockX, blockZ);
        int maxY = BlackOut.mc.level != null ? BlackOut.mc.level.getMaxY() : 319;
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

    public int getHeight(int blockX, int blockZ) {
        if (!this.isEnd) return 64;

        int quartX = QuartPos.fromBlock(blockX);
        int quartZ = QuartPos.fromBlock(blockZ);

        if ((long) quartX * quartX + (long) quartZ * quartZ <= 4096L) return 80;

        float noiseHeight = getEndIslandHeight(quartX, quartZ);

        if (noiseHeight > 0) {
            return (int) (60 + (noiseHeight / 4.0f));
        }
        return 0;
    }

    private ResourceKey<Biome> getEndBiome(int blockX, int blockZ) {
        int quartX = QuartPos.fromBlock(blockX);
        int quartZ = QuartPos.fromBlock(blockZ);

        if ((long) quartX * quartX + (long) quartZ * quartZ <= 4096L) {
            return Biomes.THE_END;
        }

        float height = getEndIslandHeight(quartX, quartZ);

        if (height > 40.0F) return Biomes.END_HIGHLANDS;
        if (height >= 0.0F) return Biomes.END_MIDLANDS;
        if (height < -20.0F) return Biomes.SMALL_END_ISLANDS;
        return Biomes.END_BARRENS;
    }

    private float getEndIslandHeight(int x, int z) {
        int i = x / 2;
        int j = z / 2;
        int k = x % 2;
        int l = z % 2;

        float f = 100.0F - Mth.sqrt((float)(x * x + z * z)) * 8.0F;
        f = Mth.clamp(f, -100.0F, 80.0F);

        for (int m = -12; m <= 12; ++m) {
            for (int n = -12; n <= 12; ++n) {
                long o = i + m;
                long p = j + n;
                if (o * o + p * p > 4096L && this.endNoise.getValue((double)o, (double)p) < -0.8999999761581421) {
                    float g = (Mth.abs((float)o) * 3439.0F + Mth.abs((float)p) * 147.0F) % 13.0F + 9.0F;
                    float h = (float)(k - m * 2);
                    float q = (float)(l - n * 2);
                    float r = 100.0F - Mth.sqrt(h * h + q * q) * g;
                    r = Mth.clamp(r, -100.0F, 80.0F);
                    f = Math.max(f, r);
                }
            }
        }
        return f;
    }
}
