package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import bodevelopment.client.blackout.util.render.WireframeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerItemInHandLayer.class)
public class MixinPlayerItemInHandLayer {
    @Inject(method = "renderArmWithItem*", at = @At("HEAD"), cancellable = true)
    private void onRenderItem(
            PlayerRenderState entityRenderState,
            BakedModel model,
            ItemStack stack,
            ItemDisplayContext transformationMode,
            HumanoidArm arm,
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            int light,
            CallbackInfo ci
    ) {
        if (WireframeRenderer.hidden || NoRender.getInstance().ignoreHand(arm)) {
            ci.cancel();
        }
    }
}
