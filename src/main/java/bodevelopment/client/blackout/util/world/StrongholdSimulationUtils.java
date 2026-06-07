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

