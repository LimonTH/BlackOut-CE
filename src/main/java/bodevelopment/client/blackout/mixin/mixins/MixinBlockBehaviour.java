package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public class MixinBlockBehaviour {
    @Inject(method = "skipRendering", at = @At("HEAD"), cancellable = true)
    private void onSkipRendering(BlockState state, BlockState adjacentState, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;

        boolean isTarget = xray.isTarget(state.getBlock());
        boolean isAdjacentTarget = xray.isTarget(adjacentState.getBlock());

        if (isTarget) {
            cir.setReturnValue(isAdjacentTarget);
        } else {
            cir.setReturnValue(xray.opacity.get() <= 0);
        }
    }

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void onGetRenderType(BlockState state, CallbackInfoReturnable<RenderShape> cir) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        if (!xray.isTarget(state.getBlock()) && xray.opacity.get() <= 0) {
            cir.setReturnValue(RenderShape.INVISIBLE);
        }
    }
}