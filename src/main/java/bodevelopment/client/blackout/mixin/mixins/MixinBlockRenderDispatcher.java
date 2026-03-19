package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BlockRenderDispatcher.class)
public class MixinBlockRenderDispatcher {
    @Inject(method = "renderBatched", at = @At("HEAD"), cancellable = true)
    private void onRenderBatched(BlockState blockState, BlockPos blockPos, BlockAndTintGetter blockAndTintGetter, PoseStack poseStack, VertexConsumer vertexConsumer, boolean bl, List<BlockModelPart> list, CallbackInfo ci) {
        XRay xray = XRay.getInstance();
        if (xray == null) return;

        if (xray.enabled && !xray.isTarget(blockState.getBlock())) {
            if (xray.opacity.get() <= 0) {
                ci.cancel();
            }
        }
    }
}