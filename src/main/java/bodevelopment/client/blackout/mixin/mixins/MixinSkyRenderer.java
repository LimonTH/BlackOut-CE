package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FogParameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.SkyRenderer")
public class MixinSkyRenderer {
    @Inject(method = "renderSkyDisc", at = @At("HEAD"), cancellable = true)
    private void onRenderSkyDisc(float r, float g, float b, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.enabled && noRender.skybox.get()) ci.cancel();
    }

    @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"), cancellable = true)
    private void onRenderSunMoonAndStars(
            PoseStack poseStack,
            com.mojang.blaze3d.vertex.Tesselator tesselator,
            float timeOfDay,
            int moonPhase,
            float rainLevel,
            float starBrightness,
            FogParameters fogParameters,
            CallbackInfo ci
    ) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.enabled && noRender.skybox.get()) ci.cancel();
    }

    @Inject(method = "renderEndSky", at = @At("HEAD"), cancellable = true)
    private void onRenderEndSky(PoseStack poseStack, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.enabled && noRender.skybox.get()) ci.cancel();
    }
}