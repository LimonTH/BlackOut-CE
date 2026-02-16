package bodevelopment.client.blackout.mixin.mixins;

import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BufferRenderer.class)
public abstract class MixinBufferRenderer {
    // TODO: РЕАЛИЗОВАНО!!! ; МИКСИН ДЛЯ ОБХОДА КРАША ПРИ ПЕРЕХОДЕ С 1.20.4 на 1.21.1 ВЫЗЫВАЕМОМ ПРИ BufferRenderer.draw() и т.п.
    /* stacktrace:
    Caused by: java.lang.NullPointerException: Cannot invoke "net.minecraft.class_9801.method_60822()" because "$$0" is null
    at knot//net.minecraft.class_286.method_43439(class_286.java:42)
    at knot//net.minecraft.class_286.method_43437(class_286.java:36)
     */

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
