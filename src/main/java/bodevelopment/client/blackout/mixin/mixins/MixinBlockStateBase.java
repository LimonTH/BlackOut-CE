package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class MixinBlockStateBase {
    @Inject(method = "isSolidRender", at = @At("HEAD"), cancellable = true)
    private void onIsSolidRender(CallbackInfoReturnable<Boolean> cir) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        cir.setReturnValue(false);
    }

    @Inject(method = "getLightBlock", at = @At("HEAD"), cancellable = true)
    private void onGetOpacity(CallbackInfoReturnable<Integer> cir) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        cir.setReturnValue(0);
    }

    @Inject(method = "getShadeBrightness", at = @At("HEAD"), cancellable = true)
    private void onGetAO(BlockGetter world, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        cir.setReturnValue(1.0f);
    }

    @Inject(method = "getLightEmission", at = @At("HEAD"), cancellable = true)
    private void onGetLuminance(CallbackInfoReturnable<Integer> cir) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        Block block = ((BlockBehaviour.BlockStateBase) (Object) this).getBlock();
        if (xray.isTarget(block)) {
            cir.setReturnValue(15);
        }
    }

    @Inject(method = "isCollisionShapeFullBlock", at = @At("HEAD"), cancellable = true)
    private void onIsFullCube(BlockGetter world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        cir.setReturnValue(false);
    }

    @Inject(method = "canOcclude", at = @At("HEAD"), cancellable = true)
    private void onIsOpaque(CallbackInfoReturnable<Boolean> cir) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        cir.setReturnValue(false);
    }

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void onGetRenderShape(CallbackInfoReturnable<RenderShape> cir) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        Block block = ((BlockBehaviour.BlockStateBase) (Object) this).getBlock();
        if (!xray.isTarget(block) && xray.opacity.get() <= 0) {
            cir.setReturnValue(RenderShape.INVISIBLE);
        }
    }
}