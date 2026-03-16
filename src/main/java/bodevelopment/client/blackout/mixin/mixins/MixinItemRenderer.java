package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer {
    @Inject(method = "getFoilBuffer", at = @At("HEAD"), cancellable = true)
    private static void onGetFoilBuffer(MultiBufferSource multiBufferSource, RenderType renderType, boolean bl, boolean hasFoil, CallbackInfoReturnable<VertexConsumer> cir) {
        NoRender noRender = NoRender.getInstance();

        if (noRender != null && noRender.enabled && noRender.enchantGlint.get()) {
            cir.setReturnValue(multiBufferSource.getBuffer(renderType));
        }
    }
}