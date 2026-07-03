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

package bodevelopment.client.blackout.util.world;

import bodevelopment.client.blackout.annotations.PublicAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;

@PublicAPI
public final class StrongholdSimulationUtils {
    private static final int MAX_RETRIES = 100;

    private StrongholdSimulationUtils() {
    }

    @PublicAPI
    public static BlockPos findPortalRoom(long worldSeed, int chunkX, int chunkZ) {
        for (int counter = 0; counter < MAX_RETRIES; counter++) {
            WorldgenRandom rng = new WorldgenRandom(new LegacyRandomSource(0L));
            rng.setLargeFeatureSeed(worldSeed + (long) counter, chunkX, chunkZ);

            StrongholdPieces.resetPieces();

            int blockX = (chunkX << 4) + 2;
            int blockZ = (chunkZ << 4) + 2;

            StructurePiecesBuilder builder = new StructurePiecesBuilder();
            StrongholdPieces.StartPiece start = new StrongholdPieces.StartPiece(rng, blockX, blockZ);
            builder.addPiece(start);

            start.addChildren(start, builder, rng);

            while (!start.pendingChildren.isEmpty()) {
                int idx = rng.nextInt(start.pendingChildren.size());
                StructurePiece piece = start.pendingChildren.remove(idx);
                piece.addChildren(start, builder, rng);
            }

            builder.moveBelowSeaLevel(63, -64, rng, 10);

            if (!builder.isEmpty() && start.portalRoomPiece != null) {

                return start.getLocatorPosition();
            }
        }

        return null;
    }

    @PublicAPI
    public static int[] findPortalRoomPos(long worldSeed, int chunkX, int chunkZ) {
        BlockPos pos = findPortalRoom(worldSeed, chunkX, chunkZ);
        return pos != null ? new int[]{pos.getX(), pos.getY(), pos.getZ()} : null;
    }
}

