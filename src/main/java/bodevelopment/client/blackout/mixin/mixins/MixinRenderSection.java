package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public class MixinRenderSection {
    @Inject(method = "hasAllNeighbors", at = @At("HEAD"), cancellable = true)
    private void onHasAllNeighbors(CallbackInfoReturnable<Boolean> cir) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        cir.setReturnValue(true);
    }
}