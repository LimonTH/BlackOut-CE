package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public class MixinInGameOverlayRenderer {
    @Inject(method = "renderInWallOverlay", at = @At("HEAD"), cancellable = true)
    private static void onWallOverlay(Sprite sprite, MatrixStack matrices, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender.enabled && noRender.wallOverlay.get()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true)
    private static void onWaterOverlay(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender.enabled && noRender.waterOverlay.get()) {
            ci.cancel();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }

    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void onFireOverlay(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender.enabled && noRender.fireOverlay.get()) {
            ci.cancel();
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.depthFunc(515);
        }
    }
}
