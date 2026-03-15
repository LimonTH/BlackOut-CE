package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class MixinBlock {
    @Inject(method = "shouldRenderFace", at = @At("HEAD"), cancellable = true)
    private static void onShouldRenderFace(BlockState blockState, BlockState blockState2, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        if (xray.isTarget(blockState.getBlock())) {
            cir.setReturnValue(true);
        }
    }
}