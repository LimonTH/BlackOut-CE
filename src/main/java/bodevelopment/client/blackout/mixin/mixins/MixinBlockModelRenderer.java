package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import net.minecraft.client.render.block.BlockModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(BlockModelRenderer.class)
public class MixinBlockModelRenderer {
    @ModifyVariable(method = "render*", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int boostLight(int light) {
        XRay xray = XRay.getInstance();
        return (xray != null && xray.enabled) ? 15728880 : light;
    }
}