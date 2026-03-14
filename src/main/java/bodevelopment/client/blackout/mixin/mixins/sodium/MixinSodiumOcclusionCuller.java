package bodevelopment.client.blackout.mixin.mixins.sodium;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller", remap = false)
public class MixinSodiumOcclusionCuller {
    @Inject(method = "isSectionVisible", at = @At("HEAD"), cancellable = true, require = 0)
    private static void onIsSectionVisible(CallbackInfoReturnable<Boolean> cir) {
        if (XRay.getInstance().enabled) {
            cir.setReturnValue(true);
        }
    }
}