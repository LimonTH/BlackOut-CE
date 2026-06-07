package bodevelopment.client.blackout.module.modules.visual.world.SeedSearcher;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.util.world.SeedSourceUtils;
import bodevelopment.client.blackout.util.world.StructureAlgorithms;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Predicate;

public final class StructureFinders {
    private StructureFinders() {
    }

    public static void findInArea(List<FoundStructure> results, long worldSeed, SeedSourceUtils source,
                                  ResourceKey<Level> dim, int centerX, int centerZ, int radius, boolean limitRadius,
                                  Predicate<StructureType> enabledCheck) {
        StructureAlgorithms.findInArea(results, worldSeed, source, BlackOut.mc.level,
                dim, centerX, centerZ, radius, limitRadius, enabledCheck);
    }

    static int getPyramidRandomOffset(long worldSeed, int blockX, int blockZ) {
        return bodevelopment.client.blackout.util.world.RandomUtils.getPyramidRandomOffset(worldSeed, blockX, blockZ);
    }
}

