package bodevelopment.client.blackout.event.events;

import bodevelopment.client.blackout.event.Cancellable;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class BlockStateEvent extends Cancellable {
    private static final BlockStateEvent INSTANCE = new BlockStateEvent();
    public BlockPos pos = null;
    public BlockState state = null;
    public BlockState previousState = null;

    public static BlockStateEvent get(BlockPos pos, BlockState state, BlockState previousState) {
        INSTANCE.pos = pos;
        INSTANCE.state = state;
        INSTANCE.previousState = previousState;
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }
}
