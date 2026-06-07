package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.util.render.CapeRenderContext;
import bodevelopment.client.blackout.util.render.consumers.CapeVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.tr7zw.waveycapes.VanillaCapeRenderer", remap = false)
public class MixinVanillaCapeRenderer {

    @Inject(method = "getVertexConsumer", at = @At("RETURN"), cancellable = true, remap = false)
    private void wrapVertexConsumer(CallbackInfoReturnable<VertexConsumer> cir) {
        float[] dims = CapeRenderContext.getDimensions();
        if (dims == null) return;

        VertexConsumer original = cir.getReturnValue();
        if (original == null) return;

        cir.setReturnValue(new CapeVertexConsumer(original, dims[0], dims[1]));
    }
}
