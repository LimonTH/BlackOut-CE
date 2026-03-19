package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import bodevelopment.client.blackout.module.modules.visual.world.Ambience;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class MixinFogRenderer {
    @Inject(method = "computeFogColor", at = @At("RETURN"), cancellable = true)
    private void onGetFogColor(Camera camera, float tickDelta, ClientLevel world, int clampedViewDistance, float skyDarkness, boolean thickenFog, CallbackInfoReturnable<Vector4f> cir) {
        Ambience ambience = Ambience.getInstance();
        if (ambience.enabled && ambience.modifyFog.get() && ambience.thickFog.get() && !ambience.removeFog.get()) {
            BlackOutColor color = ambience.color.get();
            cir.setReturnValue(new Vector4f(color.red / 255.0F, color.green / 255.0F, color.blue / 255.0F, 1.0F));
        }
    }

    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private void onSetupFog(Camera camera, int viewDistance, boolean thickenFog, net.minecraft.client.DeltaTracker deltaTracker, float partialTick, ClientLevel level, CallbackInfoReturnable<Vector4f> cir) {
        Ambience ambience = Ambience.getInstance();
        NoRender noRender = NoRender.getInstance();
        if ((ambience != null && ambience.enabled && ambience.modifyFog.get()) || (noRender != null && noRender.enabled && noRender.fog.get())) {
            // Return default fog color with no modifications - fog will be effectively disabled
            cir.setReturnValue(new Vector4f(1.0F, 1.0F, 1.0F, 1.0F));
        }
    }
}
