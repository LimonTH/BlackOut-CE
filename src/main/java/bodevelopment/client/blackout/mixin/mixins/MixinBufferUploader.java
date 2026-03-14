package bodevelopment.client.blackout.mixin.mixins;

import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BufferUploader.class)
public abstract class MixinBufferUploader {
    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private static void blackout$drawNullSafe(MeshData buffer, CallbackInfo ci) {
        if (buffer == null) {
            ci.cancel();
        }
    }

    @Inject(method = "drawWithShader", at = @At("HEAD"), cancellable = true)
    private static void blackout$drawGlobalNullSafe(MeshData buffer, CallbackInfo ci) {
        if (buffer == null) {
            ci.cancel();
        }
    }
}
