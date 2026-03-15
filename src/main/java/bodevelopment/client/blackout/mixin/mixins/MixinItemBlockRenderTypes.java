package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemBlockRenderTypes.class)
public class MixinItemBlockRenderTypes {
    @Inject(method = "getChunkRenderType", at = @At("RETURN"), cancellable = true)
    private static void onGetChunkRenderType(BlockState blockState, CallbackInfoReturnable<RenderType> cir) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        if (!xray.isTarget(blockState.getBlock()) && xray.opacity.get() > 0) {
            cir.setReturnValue(RenderType.translucent());
        }
    }

    @Inject(method = "getRenderLayer", at = @At("RETURN"), cancellable = true)
    private static void onGetFluidLayer(FluidState fluidState, CallbackInfoReturnable<RenderType> cir) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        if (!xray.isTarget(fluidState.createLegacyBlock().getBlock()) && xray.opacity.get() > 0) {
            cir.setReturnValue(RenderType.translucent());
        }
    }
}