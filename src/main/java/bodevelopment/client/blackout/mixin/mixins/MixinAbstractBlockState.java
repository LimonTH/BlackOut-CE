package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public class MixinAbstractBlockState {
    @Inject(method = "getOpacity", at = @At("HEAD"), cancellable = true)
    private void onGetOpacity(CallbackInfoReturnable<Integer> cir) {
        if (XRay.getInstance().enabled) cir.setReturnValue(0);
    }

    @Inject(method = "getAmbientOcclusionLightLevel", at = @At("HEAD"), cancellable = true)
    private void onGetAO(BlockView world, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        XRay xray = XRay.getInstance();
        if (xray != null && xray.enabled) {
            cir.setReturnValue(1.0f);
        }
    }

    @Inject(method = "getLuminance", at = @At("HEAD"), cancellable = true)
    private void onGetLuminance(CallbackInfoReturnable<Integer> cir) {
        XRay xray = XRay.getInstance();
        if (xray != null && xray.enabled) {
            Block block = ((AbstractBlock.AbstractBlockState) (Object) this).getBlock();
            if (xray.isTarget(block)) {
                cir.setReturnValue(15);
            }
        }
    }

    @Inject(method = "isFullCube", at = @At("HEAD"), cancellable = true)
    private void onIsFullCube(BlockView world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        XRay xray = XRay.getInstance();
        if (xray != null && xray.enabled) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isOpaque", at = @At("HEAD"), cancellable = true)
    private void onIsOpaque(CallbackInfoReturnable<Boolean> cir) {
        XRay xray = XRay.getInstance();
        if (xray != null && xray.enabled) {
            cir.setReturnValue(false);
        }
    }
}