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

