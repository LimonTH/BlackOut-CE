package bodevelopment.client.blackout.module.modules.visual.world.SeedSearcher;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public record FoundStructure(
        StructureType type,
        int blockX,
        int blockY,
        int blockZ,
        String extraInfo,
        List<int[]> lootChestPositions,
        Set<String> foundItems,
        List<int[]> allCheckedPositions
) {
    public FoundStructure(StructureType type, int blockX, int blockZ, String extraInfo) {
        this(type, blockX, Integer.MIN_VALUE, blockZ, extraInfo,
                Collections.emptyList(), Collections.emptySet(), Collections.emptyList());
    }

    public FoundStructure(StructureType type, int blockX, int blockY, int blockZ, String extraInfo) {
        this(type, blockX, blockY, blockZ, extraInfo,
                Collections.emptyList(), Collections.emptySet(), Collections.emptyList());
    }

    public FoundStructure(StructureType type, int blockX, int blockY, int blockZ, String extraInfo,
                          List<int[]> lootChestPositions) {
        this(type, blockX, blockY, blockZ, extraInfo,
                lootChestPositions, Collections.emptySet(), Collections.emptyList());
    }

    public FoundStructure(StructureType type, int blockX, int blockY, int blockZ, String extraInfo,
                          List<int[]> lootChestPositions, Set<String> foundItems) {
        this(type, blockX, blockY, blockZ, extraInfo,
                lootChestPositions, foundItems, Collections.emptyList());
    }

    @Deprecated
    public List<int[]> appleChestPositions() {
        return lootChestPositions;
    }
}

