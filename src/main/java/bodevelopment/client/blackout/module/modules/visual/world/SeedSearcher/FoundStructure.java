package bodevelopment.client.blackout.module.modules.visual.world.SeedSearcher;

import java.util.Collections;
import java.util.List;

public record FoundStructure(StructureType type, int blockX, int blockZ, String extraInfo, List<int[]> appleChestPositions) {
    public FoundStructure(StructureType type, int blockX, int blockZ, String extraInfo) {
        this(type, blockX, blockZ, extraInfo, Collections.emptyList());
    }
}
