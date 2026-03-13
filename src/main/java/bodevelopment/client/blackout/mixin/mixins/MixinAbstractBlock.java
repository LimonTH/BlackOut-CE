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
public class MixinAbstractBlock {
    @Inject(method = "skipRendering", at = @At("HEAD"), cancellable = true)
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

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void onGetRenderType(BlockState state, CallbackInfoReturnable<RenderShape> cir) {
        XRay xray = XRay.getInstance();
        if (xray != null && xray.enabled) {
            if (!xray.isTarget(state.getBlock())) {
                cir.setReturnValue(RenderShape.INVISIBLE);
            }
        }
    }
}