package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import bodevelopment.client.blackout.util.render.consumers.XRayVertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderDispatcher.class)
public class MixinBlockRenderDispatcher {
    @Inject(method = "renderBatched", at = @At("HEAD"), cancellable = true)
    private void onRenderBatched(BlockState state, BlockPos pos, BlockAndTintGetter world, PoseStack matrices, VertexConsumer vertexConsumer, boolean cull, RandomSource random, CallbackInfo ci) {
        XRay xray = XRay.getInstance();
        if (xray == null) return;

        if (xray.enabled && !xray.isTarget(state.getBlock())) {
            if (xray.opacity.get() <= 0) {
                ci.cancel();
            }
        }
    }

    @ModifyVariable(method = "renderBatched", at = @At("HEAD"), argsOnly = true)
    private VertexConsumer modifyVertexConsumer(
            VertexConsumer vertexConsumer,
            BlockState blockState,
            BlockPos blockPos,
            BlockAndTintGetter world,
            PoseStack poseStack,
            VertexConsumer originalConsumer,
            boolean bl,
            RandomSource randomSource
    ) {
        XRay xray = XRay.getInstance();
        if (xray != null && xray.enabled && !xray.isTarget(blockState.getBlock())) {
            if (xray.opacity.get() > 0) {
                return new XRayVertexConsumer(vertexConsumer, xray.opacity.get());
            }
        }
        return vertexConsumer;
    }
}