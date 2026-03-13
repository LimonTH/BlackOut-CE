package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class MixinBlockEntityRenderDispatcher {
    @Inject(method = "render*", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity> void onRender(E blockEntity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, CallbackInfo ci) {
        XRay xray = XRay.getInstance();
        if (xray != null && xray.enabled) {
            if (!xray.isTarget(blockEntity.getBlockState().getBlock())) {
                ci.cancel();
            }
        }
    }
}