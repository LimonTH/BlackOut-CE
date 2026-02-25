package bodevelopment.client.blackout.mixin.mixins;

import java.util.function.Supplier;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FabricBakedModel.class)
public interface MixinFabricBakedModel {
    @Inject(method = "emitBlockQuads", at = @At("HEAD"), cancellable = true)
    default void onEmitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context, CallbackInfo ci) {
        XRay xray = XRay.getInstance();
        if (xray != null && xray.enabled && state != null) {
            if (!xray.isTarget(state.getBlock())) {
                ci.cancel();
            }
        }
    }
}