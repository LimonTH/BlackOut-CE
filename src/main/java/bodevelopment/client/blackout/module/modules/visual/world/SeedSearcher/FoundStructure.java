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

