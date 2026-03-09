package bodevelopment.client.blackout.mixin.mixins;

import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BufferRenderer.class)
public abstract class MixinBufferRenderer {
    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private static void blackout$drawNullSafe(BuiltBuffer buffer, CallbackInfo ci) {
        if (buffer == null) {
            ci.cancel();
        }
    }

    @Inject(method = "drawWithGlobalProgram", at = @At("HEAD"), cancellable = true)
    private static void blackout$drawGlobalNullSafe(BuiltBuffer buffer, CallbackInfo ci) {
        if (buffer == null) {
            ci.cancel();
        }
    }
}
