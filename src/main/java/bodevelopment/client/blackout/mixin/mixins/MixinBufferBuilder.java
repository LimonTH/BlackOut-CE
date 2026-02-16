package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.mixin.accessors.AccessorBufferBuilder;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder {
    // TODO: РЕАЛИЗОВАНО!!! ; МИКСИН ДЛЯ ОБХОДА КРАША ПРИ ПЕРЕХОДЕ С 1.20.4 на 1.21.1 ВЫЗЫВАЕМОМ ПРИ BufferBuilder.end() и т.п.
    /* stacktrace:
    Caused by: java.lang.IllegalStateException: BufferBuilder was empty
    at knot//net.minecraft.class_287.method_60800(class_287.java:69)
     */
    @Inject(method = "end", at = @At("HEAD"), cancellable = true)
    private void blackout$safeEnd(CallbackInfoReturnable<BuiltBuffer> cir) {
        // Проверяем, есть ли в билдере вообще хоть какие-то данные
        // В 1.21.1 поле 'building' или 'vertexCount' отвечает за это.
        // Если билдер пуст, просто возвращаем null вместо того, чтобы падать.
        if (!((AccessorBufferBuilder) this).isBuilding() || ((AccessorBufferBuilder) this).getVertexCount() == 0) {
            cir.setReturnValue(null);
        }
    }
}

