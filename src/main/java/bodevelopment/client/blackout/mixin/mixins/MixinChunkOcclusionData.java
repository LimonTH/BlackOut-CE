package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkOcclusionData.class)
public class MixinChunkOcclusionData {

    @Inject(method = "isVisibleThrough", at = @At("HEAD"), cancellable = true)
    private void onIsVisibleThrough(net.minecraft.util.math.Direction from, net.minecraft.util.math.Direction to, CallbackInfoReturnable<Boolean> cir) {
        if (XRay.getInstance().enabled) {
            cir.setReturnValue(true);
        }
    }
}