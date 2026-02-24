package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.events.BlockStateEvent;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldChunk.class)
public class MixinWorldChunk {
    @Inject(method = "setBlockState", at = @At("TAIL"), cancellable = true)
    private void onBlockState(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir) {
        BlockStateEvent event = BlockStateEvent.get(pos, state, cir.getReturnValue());
        if (BlackOut.EVENT_BUS.post(event).isCancelled()) {
            cir.cancel();
        }
    }
}
