package bodevelopment.client.blackout.randomstuff;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public record PlaceData(BlockPos pos, Direction dir, boolean valid, boolean sneak) {
}
