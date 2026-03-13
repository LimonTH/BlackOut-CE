package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderDispatcher.class)
public class MixinBlockRenderManager {
    @Inject(method = "renderBatched", at = @At("HEAD"), cancellable = true)
    private void onRenderBlock(BlockState state, BlockPos pos, BlockAndTintGetter world, PoseStack matrices, VertexConsumer vertexConsumer, boolean cull, RandomSource random, CallbackInfo ci) {
        XRay xray = XRay.getInstance();
        if (xray != null && xray.enabled) {
            if (!xray.isTarget(state.getBlock())) {
                ci.cancel();
            }
        }
    }
}