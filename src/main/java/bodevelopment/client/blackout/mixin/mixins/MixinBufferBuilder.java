package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.mixin.accessors.AccessorBufferBuilder;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder {
    @Inject(method = "buildOrThrow", at = @At("HEAD"), cancellable = true)
    private void blackout$safeEnd(CallbackInfoReturnable<MeshData> cir) {
        if (!((AccessorBufferBuilder) this).isBuilding() || ((AccessorBufferBuilder) this).getVertexCount() == 0) {
            cir.setReturnValue(null);
        }
    }
}

