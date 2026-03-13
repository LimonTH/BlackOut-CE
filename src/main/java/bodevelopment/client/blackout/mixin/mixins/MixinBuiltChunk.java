package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection")
public class MixinBuiltChunk {
    @Inject(method = "hasAllNeighbors", at = @At("HEAD"), cancellable = true)
    private void onShouldBuild(CallbackInfoReturnable<Boolean> cir) {
        if (XRay.getInstance().enabled) {
            cir.setReturnValue(true);
        }
    }
}