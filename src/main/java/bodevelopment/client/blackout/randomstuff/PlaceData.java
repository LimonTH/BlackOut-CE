package bodevelopment.client.blackout.randomstuff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record PlaceData(BlockPos pos, Direction dir, boolean valid, boolean sneak) {
}
