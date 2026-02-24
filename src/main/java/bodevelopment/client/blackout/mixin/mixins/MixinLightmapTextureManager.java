package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.world.Brightness;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LightmapTextureManager.class)
public class MixinLightmapTextureManager {
    @ModifyVariable(
            method = "update",
            at = @At(value = "STORE", ordinal = 1),
            ordinal = 2
    )
    private float onUpdate(float value) {
        Brightness brightness = Brightness.getInstance();
        if (brightness != null && brightness.enabled && brightness.mode.get() == Brightness.Mode.Gamma) {
            return 1.0f;
        }
        return value;
    }
}
