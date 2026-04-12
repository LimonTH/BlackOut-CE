package bodevelopment.client.blackout.util;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Set;

public class SeedBiomeSource {
    private static final Set<ResourceKey<Biome>> OCEAN_BIOMES = Set.of(
        Biomes.OCEAN, Biomes.DEEP_OCEAN,
        Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN,
        Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN,
        Biomes.DEEP_LUKEWARM_OCEAN,
        Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN
    );

    private static HolderLookup.Provider vanillaRegistries;

    private final RandomState randomState;
    private final Climate.Sampler sampler;
    private final Climate.ParameterList<ResourceKey<Biome>> parameters;
    private final boolean isEnd;
    private final boolean isNether;

    private final TheEndBiomeSource endBiomeSource;

    public static final int OPTIMAL_Y = 64;

    public SeedBiomeSource(long seed, ResourceKey<Level> dimension) {
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

        if (this.isEnd) {
            this.endBiomeSource = TheEndBiomeSource.create(biomeRegistry);
            this.parameters = null;
        } else {
            this.endBiomeSource = null;
            MultiNoiseBiomeSourceParameterList.Preset preset = (dimension == Level.NETHER)
                    ? MultiNoiseBiomeSourceParameterList.Preset.NETHER
                    : MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD;
            this.parameters = MultiNoiseBiomeSourceParameterList.knownPresets().get(preset);
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

    /**
     * Returns true if End terrain at (blockX, blockZ) has a solid block at Y=60.
     * Uses the vanilla finalDensity function via SinglePointContext — exact same check
     * as vanilla EndCityStructure.findGenerationPoint (getFirstOccupiedHeight >= 60).
     * Since all End noise functions are seed-independent (endIslands uses fixed seed 0L,
     * BASE_3D_NOISE_END is unseeded), this works correctly for any world seed.
     * TODO: ???. This is the original code, but individual cities are cut off correctly.
     */
    public boolean hasSolidTerrainAtY60(int blockX, int blockZ) {
        if (!isEnd) return false;
        double density = this.randomState.router().finalDensity()
                .compute(new DensityFunction.SinglePointContext(blockX, 60, blockZ));
        return density > 0.0;
    }

    public int getHeight(int blockX, int blockZ) {
        return BiomeColorMap.getHeight(getBiome(blockX, blockZ));
    }

    /** Returns color in low 32 bits, height in bits 32-63 (single biome lookup). */
    public long getBiomeSurfaceData(int blockX, int blockZ) {
        return BiomeColorMap.getSurfaceData(getBiome(blockX, blockZ));
    }

    /**
     * Mirrors vanilla ConcentricRingsStructurePlacement biome snapping:
     * searches within 112 blocks (28 quart units) for the nearest non-ocean chunk
     * and returns its chunk coordinates [chunkX, chunkZ].
     * If the theoretical position itself is valid, it is returned unchanged (fast path).
     */
    public int[] findNearestStrongholdChunk(int chunkX, int chunkZ) {
        int blockX = (chunkX << 4) + 8;
        int blockZ = (chunkZ << 4) + 8;
        int quartX = QuartPos.fromBlock(blockX);
        int quartZ = QuartPos.fromBlock(blockZ);
        int quartRadius = QuartPos.fromBlock(112); // 112 blocks = 28 quart units

        for (int dq = 0; dq <= quartRadius; dq++) {
            for (int dqz = -dq; dqz <= dq; dqz++) {
                boolean edgeZ = Math.abs(dqz) == dq;
                for (int dqx = -dq; dqx <= dq; dqx++) {
                    boolean edgeX = Math.abs(dqx) == dq;
                    if (!edgeX && !edgeZ) continue; // only check the border of each ring
                    ResourceKey<Biome> biome = getBiomeAt(
                        QuartPos.toBlock(quartX + dqx), OPTIMAL_Y, QuartPos.toBlock(quartZ + dqz)
                    );
                    if (!OCEAN_BIOMES.contains(biome)) {
                        int foundBlock = QuartPos.toBlock(quartX + dqx);
                        int foundBlockZ = QuartPos.toBlock(quartZ + dqz);
                        return new int[]{ foundBlock >> 4, foundBlockZ >> 4 };
                    }
                }
            }
        }
        return new int[]{ chunkX, chunkZ };
    }
}
