package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.world.Brightness;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightTexture.class)
public class MixinLightmapTextureManager {
    @Inject(method = "getBrightness*", at = @At("HEAD"), cancellable = true)
    private static void onGetBrightness(DimensionType type, int lightLevel, CallbackInfoReturnable<Float> info) {
        Brightness brightness = Brightness.getInstance();
        if (brightness.enabled && brightness.mode.get() == Brightness.Mode.Gamma) {
            info.setReturnValue(1.0F);
        }
    }

    @ModifyVariable(method = "updateLightTexture", at = @At(value = "STORE", ordinal = 0), index = 15)
    private float modifyLightmapBrightness(float value) {
        Brightness brightness = Brightness.getInstance();
        if (brightness.enabled && brightness.mode.get() == Brightness.Mode.Luminance) {
            float minLight = brightness.luminanceLevel.get() / 15.0F;
            return Math.max(value, minLight);
        }
        return value;
    }
}
