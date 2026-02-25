package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.class)
public class MixinAbstractBlock {
    @Inject(method = "isSideInvisible", at = @At("HEAD"), cancellable = true)
    private void onIsSideInvisible(BlockState state, BlockState stateFrom, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        XRay xray = XRay.getInstance();
        if (xray != null && xray.enabled) {
            boolean isTarget = xray.isTarget(state.getBlock());
            boolean isTargetFrom = xray.isTarget(stateFrom.getBlock());

            if (isTarget) {
                cir.setReturnValue(isTargetFrom);
            } else {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void onGetRenderType(BlockState state, CallbackInfoReturnable<BlockRenderType> cir) {
        XRay xray = XRay.getInstance();
        if (xray != null && xray.enabled) {
            if (!xray.isTarget(state.getBlock())) {
                cir.setReturnValue(BlockRenderType.INVISIBLE);
            }
        }
    }
}