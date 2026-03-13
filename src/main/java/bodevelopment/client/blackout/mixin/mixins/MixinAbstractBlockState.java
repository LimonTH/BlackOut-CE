package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class MixinAbstractBlockState {
    @Inject(method = "getLightBlock", at = @At("HEAD"), cancellable = true)
    private void onGetOpacity(CallbackInfoReturnable<Integer> cir) {
        if (XRay.getInstance().enabled) cir.setReturnValue(0);
    }

    @Inject(method = "getShadeBrightness", at = @At("HEAD"), cancellable = true)
    private void onGetAO(BlockGetter world, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        XRay xray = XRay.getInstance();
        if (xray != null && xray.enabled) {
            cir.setReturnValue(1.0f);
        }
    }

    @Inject(method = "getLightEmission", at = @At("HEAD"), cancellable = true)
    private void onGetLuminance(CallbackInfoReturnable<Integer> cir) {
        XRay xray = XRay.getInstance();
        if (xray != null && xray.enabled) {
            Block block = ((BlockBehaviour.BlockStateBase) (Object) this).getBlock();
            if (xray.isTarget(block)) {
                cir.setReturnValue(15);
            }
        }
    }

    @Inject(method = "isCollisionShapeFullBlock", at = @At("HEAD"), cancellable = true)
    private void onIsFullCube(BlockGetter world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        XRay xray = XRay.getInstance();
        if (xray != null && xray.enabled) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canOcclude", at = @At("HEAD"), cancellable = true)
    private void onIsOpaque(CallbackInfoReturnable<Boolean> cir) {
        XRay xray = XRay.getInstance();
        if (xray != null && xray.enabled) {
            cir.setReturnValue(false);
        }
    }
}