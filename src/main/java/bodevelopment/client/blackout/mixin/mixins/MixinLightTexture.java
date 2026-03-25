package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.world.Brightness;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightTexture.class)
public class MixinLightTexture {
    @Inject(method = "getBrightness(Lnet/minecraft/world/level/dimension/DimensionType;I)F", at = @At("HEAD"), cancellable = true)
    private static void onGetBrightness(DimensionType type, int lightLevel, CallbackInfoReturnable<Float> info) {
        Brightness brightness = Brightness.getInstance();
        if (brightness != null && brightness.enabled && brightness.mode.get() == Brightness.Mode.Gamma) {
            info.setReturnValue(1.0F);
        }
    }

    @Inject(method = "getBrightness(FI)F", at = @At("HEAD"), cancellable = true)
    private static void onGetBrightnessAmbient(float ambientLight, int lightLevel, CallbackInfoReturnable<Float> info) {
        Brightness brightness = Brightness.getInstance();
        if (brightness != null && brightness.enabled && brightness.mode.get() == Brightness.Mode.Gamma) {
            info.setReturnValue(1.0F);
        }
    }

    /**
     * In 1.21.4, the lightmap is rendered via a shader (CoreShaders.LIGHTMAP).
     * We inject just before the quad is drawn and override the shader uniforms
     * to force full brightness (Gamma) or minimum light level (Luminance).
     */
    @Inject(
            method = "updateLightTexture",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/pipeline/TextureTarget;bindWrite(Z)V"
            )
    )
    private void onUpdateLightTexture(float tickDelta, CallbackInfo ci) {
        Brightness brightness = Brightness.getInstance();
        if (brightness == null || !brightness.enabled) return;

        CompiledShaderProgram shader = RenderSystem.getShader();
        if (shader == null) return;

        switch (brightness.mode.get()) {
            case Gamma -> {
                shader.safeGetUniform("BrightnessFactor").set(100.0F);
                shader.safeGetUniform("NightVisionFactor").set(1.0F);
            }
            case Luminance -> {
                float minLight = brightness.luminanceLevel.get() / 15.0F;
                shader.safeGetUniform("AmbientLightFactor").set(minLight * 0.5F);
                shader.safeGetUniform("BrightnessFactor").set(minLight);
            }
        }
    }
}
