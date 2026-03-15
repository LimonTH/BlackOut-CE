package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import bodevelopment.client.blackout.util.render.XRayVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiquidBlockRenderer.class)
public class MixinLiquidBlockRenderer {
    @Inject(method = "tesselate", at = @At("HEAD"), cancellable = true)
    private void onRenderFluid(BlockAndTintGetter world, BlockPos pos, VertexConsumer vertexConsumer, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        XRay xray = XRay.getInstance();
        if (xray != null && xray.enabled) {
            if (!xray.isTarget(blockState.getBlock())) {
                if (xray.opacity.get() <= 0) {
                    ci.cancel();
                }
            }
        }
    }

    @ModifyVariable(method = "tesselate", at = @At("HEAD"), argsOnly = true)
    private VertexConsumer modifyVertexConsumer(VertexConsumer original, BlockAndTintGetter world, BlockPos pos, VertexConsumer consumer, BlockState blockState, FluidState fluidState) {
        XRay xray = XRay.getInstance();
        if (xray != null && xray.enabled) {
            Block block = fluidState.createLegacyBlock().getBlock();

            if (!xray.isTarget(block) && xray.opacity.get() > 0) {
                return new XRayVertexConsumer(original, xray.opacity.get());
            }
        }
        return original;
    }
}