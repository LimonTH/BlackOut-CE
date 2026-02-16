package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.world.Brightness;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightmapTextureManager.class)
public class MixinLightmapTextureManager {
    @Inject(method = "getBrightness", at = @At("HEAD"), cancellable = true)
    private static void getBrightnessLevel(DimensionType type, int lightLevel, CallbackInfoReturnable<Float> cir) {
        Brightness brightness = Brightness.getInstance();
        if (brightness.enabled && brightness.mode.get() == Brightness.Mode.Gamma) {
            cir.setReturnValue(1.0E7F);
        }
    }
}
