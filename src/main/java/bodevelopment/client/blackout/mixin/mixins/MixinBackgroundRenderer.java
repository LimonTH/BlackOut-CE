package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.world.Ambience;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class MixinBackgroundRenderer {
    @Inject(method = "computeFogColor", at = @At("RETURN"), cancellable = true)
    private static void onGetFogColor(Camera camera, float tickDelta, ClientLevel world, int clampedViewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir) {
        Ambience ambience = Ambience.getInstance();
        if (ambience.enabled && ambience.modifyFog.get() && ambience.thickFog.get() && !ambience.removeFog.get()) {
            BlackOutColor color = ambience.color.get();
            cir.setReturnValue(new Vector4f(color.red / 255.0F, color.green / 255.0F, color.blue / 255.0F, 1.0F));
        }
    }

    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private static void onApplyFog(Camera camera, FogRenderer.FogMode fogType, Vector4f color, float viewDistance, boolean thickenFog, float tickDelta, CallbackInfoReturnable<FogParameters> cir) {
        Ambience ambience = Ambience.getInstance();
        if (ambience != null && ambience.enabled && ambience.modifyFog(fogType == FogRenderer.FogMode.FOG_TERRAIN)) {
            cir.setReturnValue(FogParameters.NO_FOG);
        }
    }
}
