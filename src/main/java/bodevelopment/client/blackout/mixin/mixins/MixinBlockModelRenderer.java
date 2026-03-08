package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.world.Brightness;
import net.minecraft.client.render.block.BlockModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(BlockModelRenderer.class)
public class MixinBlockModelRenderer {
    @ModifyVariable(method = "render*", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int boostLight(int light) {
        if (Brightness.getInstance().enabled) {
            if (Brightness.getInstance().mode.get() == Brightness.Mode.Gamma) {
                return 15728880;
            }

            if (Brightness.getInstance().mode.get() == Brightness.Mode.Luminance) {
                return Math.max(light, Brightness.luminanceValue);
            }
        }

        return light;
    }
}