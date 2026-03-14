package bodevelopment.client.blackout.mixin.mixins.sodium;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.occlusion.VisibilityEncoding", remap = false)
public class MixinSodiumVisibilityEncoding {
    @Inject(method = "getConnections(JI)I", at = @At("HEAD"), cancellable = true)
    private static void onGetConnectionsIncoming(long visibilityData, int incoming, CallbackInfoReturnable<Integer> cir) {
        if (XRay.getInstance().enabled) {
            cir.setReturnValue(63);
        }
    }

    @Inject(method = "getConnections(J)I", at = @At("HEAD"), cancellable = true)
    private static void onGetConnections(long visibilityData, CallbackInfoReturnable<Integer> cir) {
        if (XRay.getInstance().enabled) {
            cir.setReturnValue(63);
        }
    }
}