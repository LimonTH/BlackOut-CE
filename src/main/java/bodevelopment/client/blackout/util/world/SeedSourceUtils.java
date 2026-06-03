package bodevelopment.client.blackout.util.world;

import bodevelopment.client.blackout.util.BiomeColorMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.Set;

public class SeedSourceUtils {
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
     * Returns the first occupied height (WORLD_SURFACE_WG) at the given block coordinates
     * using the End dimension's density functions.
     * Mirrors vanilla ChunkGenerator.getFirstOccupiedHeight for End terrain.
     * Scans from the max build height downward, finding the first Y where finalDensity > 0.
     * This is the exact same algorithm as vanilla's getFirstOccupiedHeight with WORLD_SURFACE_WG.
     *
     * @since End noise is seed-independent (endIslands uses fixed seed 0L), this works for any world seed.
     */
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

    /**
     * Returns true if the terrain surface at (blockX, blockZ) is at or above Y=60.
     * Uses proper height scanning (same as vanilla getFirstOccupiedHeight >= 60),
     * NOT just checking density at Y=60.
     */
    public boolean hasTerrainAtOrAbove60(int blockX, int blockZ) {
        return getFirstOccupiedHeight(blockX, blockZ) >= 60;
    }

    public boolean hasSolidTerrainAtY60(int blockX, int blockZ) {
        return hasTerrainAtOrAbove60(blockX, blockZ);
    }

    /**
     * Checks if there is solid terrain above sea level (Y >= 64) at the given block position
     * using the overworld noise density function. This mirrors vanilla's getLowestY check
     * used by SinglePieceStructure.findGenerationPoint.
     * <p>
     * Samples the final density at (blockX, 64, blockZ). If density > 0, the terrain exists
     * at or above Y=64, meaning the structure can generate on solid ground.
     * Returns true for Nether/End dimensions where this check doesn't apply.
     */
    public boolean hasSolidTerrainAboveSeaLevel(int blockX, int blockZ) {
        if (isNether || isEnd) return true;
        if (this.randomState == null) return true;
        try {
            var density = this.randomState.router().finalDensity();
            double d = density.compute(new DensityFunction.SinglePointContext(blockX, 64, blockZ));
            return d > 0.0;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Checks if a bounding box area has terrain above sea level.
     * Samples the center + 4 corners to catch edge cases near water bodies.
     */
    public boolean hasAreaAboveSeaLevel(int centerX, int centerZ, int halfWidth) {
        int[][] points = {{0, 0}, {halfWidth, halfWidth}, {halfWidth, -halfWidth},
                          {-halfWidth, halfWidth}, {-halfWidth, -halfWidth}};
        for (int[] pt : points) {
            if (hasSolidTerrainAboveSeaLevel(centerX + pt[0], centerZ + pt[1]))
                return true;
        }
        return false;
    }

    /**
     * Finds the surface Y (first solid block from top) at the given block position
     * using the overworld noise density function. Scans from Y=90 downward to Y=40.
     * Returns the highest Y where density > 0, or Integer.MIN_VALUE if no terrain found.
     * This is used to determine the exact chest Y for structures placed on the surface.
     */
    public int getSurfaceY(int blockX, int blockZ) {
        if (isNether || isEnd) return 64;
        if (this.randomState == null) return 64;
        try {
            var density = this.randomState.router().finalDensity();
            for (int y = 90; y >= 40; y--) {
                if (density.compute(new DensityFunction.SinglePointContext(blockX, y, blockZ)) > 0.0) {
                    return y;
                }
            }
            return Integer.MIN_VALUE;
        } catch (Exception e) {
            return 64;
        }
    }

    public int getHeight(int blockX, int blockZ) {
        return BiomeColorMap.getHeight(getBiome(blockX, blockZ));
    }

    /**
     * Returns color in low 32 bits, height in bits 32-63 (single biome lookup).
     */
    public long getBiomeSurfaceData(int blockX, int blockZ) {
        return BiomeColorMap.getSurfaceData(getBiome(blockX, blockZ));
    }

    /**
     * Mirrors vanilla ConcentricRingsStructurePlacement biome snapping:
     * searches within 112 blocks (28 quart units) for the nearest non-ocean chunk
     * and returns its chunk coordinates [chunkX, chunkZ].
     * If the theoretical position itself is valid, it is returned unchanged (fast path).
     */
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

    /**
     * Mirrors vanilla {@code ChunkGeneratorStructureState.trySnapToPreferredBiomes}.
     * Searches within 112 blocks (28 quart units) for a chunk whose center biome
     * is in the {@code #minecraft:stronghold_biased_to} tag.
     * Uses the exact same expansion search as vanilla (square rings in quart space).
     */
    public int[] findNearestStrongholdChunk(int chunkX, int chunkZ) {
        int blockX = (chunkX << 4) + 8;
        int blockZ = (chunkZ << 4) + 8;
        int quartX = QuartPos.fromBlock(blockX);
        int quartZ = QuartPos.fromBlock(blockZ);
        int quartRadius = QuartPos.fromBlock(112);

        ResourceKey<Biome> rawBiome = getBiomeAt(blockX, OPTIMAL_Y, blockZ);
        if (STRONGHOLD_BIASED_TO.contains(rawBiome)) {
            return new int[]{chunkX, chunkZ};
        }

        for (int dq = 0; dq <= quartRadius; dq++) {
            for (int dqz = -dq; dqz <= dq; dqz++) {
                boolean edgeZ = Math.abs(dqz) == dq;
                for (int dqx = -dq; dqx <= dq; dqx++) {
                    boolean edgeX = Math.abs(dqx) == dq;
                    if (!edgeX && !edgeZ) continue;
                    ResourceKey<Biome> biome = getBiomeAt(
                            QuartPos.toBlock(quartX + dqx), OPTIMAL_Y, QuartPos.toBlock(quartZ + dqz)
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
