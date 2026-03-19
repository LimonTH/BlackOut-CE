package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.events.BlockStateEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public class MixinLevelChunk {
    @Inject(method = "setBlockState", at = @At("TAIL"), cancellable = true)
    private void onBlockState(BlockPos blockPos, BlockState blockState, int i, CallbackInfoReturnable<BlockState> cir) {
        BlockStateEvent event = BlockStateEvent.get(blockPos, blockState, cir.getReturnValue());
        if (BlackOut.EVENT_BUS.post(event).isCancelled()) {
            cir.cancel();
        }
    }
}
