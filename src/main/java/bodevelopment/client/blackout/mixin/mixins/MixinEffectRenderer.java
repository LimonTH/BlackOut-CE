package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class MixinEffectRenderer {
    @Inject(method = "renderTex", at = @At("HEAD"), cancellable = true)
    private static void onWallOverlay(TextureAtlasSprite textureAtlasSprite, PoseStack poseStack, MultiBufferSource multiBufferSource, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender.enabled && noRender.wallOverlay.get()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderWater", at = @At("HEAD"), cancellable = true)
    private static void onWaterOverlay(Minecraft minecraft, PoseStack poseStack, MultiBufferSource multiBufferSource, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender.enabled && noRender.waterOverlay.get()) {
            ci.cancel();
            GlStateManager._disableBlend();
        }
    }

    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private static void onFireOverlay(PoseStack poseStack, MultiBufferSource multiBufferSource, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender.enabled && noRender.fireOverlay.get()) {
            ci.cancel();
            GlStateManager._disableBlend();
            GlStateManager._depthMask(true);
            GlStateManager._depthFunc(515);
        }
    }
}
