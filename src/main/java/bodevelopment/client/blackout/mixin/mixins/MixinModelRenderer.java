package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.world.Brightness;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ModelBlockRenderer.class)
public class MixinModelRenderer {
    @ModifyVariable(method = "renderModel", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int boostLight(int light) {
        if (Brightness.getInstance().enabled) {
            if (Brightness.getInstance().mode.get() == Brightness.Mode.Gamma) {
                return 15728880;
            }

            if (Brightness.getInstance().mode.get() == Brightness.Mode.Luminance) {
                int sky = (light >> 20) & 15;
                int block = (light >> 4) & 15;

                int lLevel = Brightness.getInstance().luminanceLevel.get();

                int newSky = Math.max(sky, lLevel);
                int newBlock = Math.max(block, lLevel);

                return (newSky << 20) | (newBlock << 4);
            }
        }
        return light;
    }
}