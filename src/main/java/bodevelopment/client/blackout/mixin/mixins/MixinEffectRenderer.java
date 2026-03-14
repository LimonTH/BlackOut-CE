package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class MixinEffectRenderer {
    @Inject(method = "renderTex", at = @At("HEAD"), cancellable = true)
    private static void onWallOverlay(TextureAtlasSprite sprite, PoseStack matrices, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender.enabled && noRender.wallOverlay.get()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderWater", at = @At("HEAD"), cancellable = true)
    private static void onWaterOverlay(Minecraft client, PoseStack matrices, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender.enabled && noRender.waterOverlay.get()) {
            ci.cancel();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }

    @Inject(method = "renderFire", at = @At("HEAD"), cancellable = true)
    private static void onFireOverlay(Minecraft client, PoseStack matrices, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender.enabled && noRender.fireOverlay.get()) {
            ci.cancel();
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.depthFunc(515);
        }
    }
}
