package bodevelopment.client.blackout.util.world;

import bodevelopment.client.blackout.annotations.PublicAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

@PublicAPI
public final class TerrainUtils {
    private TerrainUtils() {
    }

    public static int findSurfaceY(DensityFunction density, int x, int z, int bottomY, int topY) {
        for (int y = topY; y >= bottomY; y--) {
            if (density.compute(new DensityFunction.SinglePointContext(x, y, z)) > 0.0) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    public static boolean hasTerrainNearSeaLevel(DensityFunction density, int x, int z) {

        for (int y = 90; y >= 60; y--) {
            if (density.compute(new DensityFunction.SinglePointContext(x, y, z)) > 0.0) {
                return true;
            }
        }
        return false;
    }

    public static int getAverageSurfaceYInArea(SeedSourceUtils source, Level level,
                                               int originX, int originZ, int width, int depth) {
        if (source == null) return Integer.MIN_VALUE;

        RandomState randomState = source.getRandomState();
        if (randomState != null) {
            var density = randomState.router().finalDensity();
            int sampleStep = Math.max(1, Math.min(width, depth) / 3);
            for (int dx = 0; dx < width; dx += sampleStep) {
                for (int dz = 0; dz < depth; dz += sampleStep) {
                    if (!hasTerrainNearSeaLevel(density, originX + dx, originZ + dz))
                        return Integer.MIN_VALUE;
                }
            }
            if (!hasTerrainNearSeaLevel(density, originX + width - 1, originZ + depth - 1))
                return Integer.MIN_VALUE;
        }

        long sum = 0;
        long count = 0;
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                int y = getExactHeight(source, level, originX + dx, originZ + dz);
                if (y == Integer.MIN_VALUE) return Integer.MIN_VALUE;
                sum += y;
                count++;
            }
        }
        return count == 0 ? Integer.MIN_VALUE : (int) (sum / count);
    }

    public static int getMinimumSurfaceYInArea(SeedSourceUtils source, Level level,
                                               int originX, int originZ, int width, int depth) {
        if (source == null) return Integer.MIN_VALUE;

        int stride = 4;
        int minY = Integer.MAX_VALUE;
        boolean found = false;
        for (int dx = 0; dx < width; dx += stride) {
            for (int dz = 0; dz < depth; dz += stride) {
                int y = getExactHeight(source, level, originX + dx, originZ + dz);
                if (y == Integer.MIN_VALUE) continue;
                if (y < minY) minY = y;
                found = true;
            }
        }

        int y = getExactHeight(source, level, originX + width - 1, originZ + depth - 1);
        if (y != Integer.MIN_VALUE && y < minY) minY = y;
        return found ? minY : Integer.MIN_VALUE;
    }

    public static int getExactHeight(SeedSourceUtils source, Level level, int blockX, int blockZ) {
        if (level != null) {
            int chunkX = blockX >> 4;
            int chunkZ = blockZ >> 4;
            if (level.getChunkSource().hasChunk(chunkX, chunkZ)) {
                try {
                    return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            new BlockPos(blockX, 64, blockZ)).getY();
                } catch (Exception ignored) {
                }
            }
        }

        if (source != null) {
            int y = source.getSurfaceY(blockX, blockZ);
            if (y != Integer.MIN_VALUE && y != 64) return y;
        }

        if (source != null && source.getRandomState() != null) {
            int y = findSurfaceY(source.getRandomState().router().finalDensity(), blockX, blockZ, -64, 90);
            if (y != Integer.MIN_VALUE) return y;
        }

        return 64;
    }
}

