package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.Internal;
import bodevelopment.client.blackout.module.modules.misc.Streamer;
import bodevelopment.client.blackout.util.render.CapeRenderContext;
import bodevelopment.client.blackout.util.Capes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
@Internal
public class MixinCapeLayer {

    @Inject(method = "render*", at = @At("HEAD"))
    private void onRenderHead(PoseStack poseStack, MultiBufferSource vertexConsumerProvider, int i,
                              PlayerRenderState state, float f, float g, CallbackInfo ci) {
        if (state.id == BlackOut.mc.player.getId()) {
            Streamer streamer = Streamer.getInstance();
            if (streamer.enabled && streamer.skin.get()) {
                state.showCape = false;
                CapeRenderContext.clear();
                return;
            }
        }

        ResourceLocation cape = Capes.getCape(state);
        if (cape != null) {
            state.showCape = true;
            float[] dims = Capes.getDimensionsFor(cape);
            if (dims != null) {
                CapeRenderContext.set(cape, dims[0], dims[1]);
            } else {
                CapeRenderContext.set(cape);
            }
        } else {
            CapeRenderContext.clear();
        }
    }

    @Inject(method = "render*", at = @At("RETURN"))
    private void onRenderReturn(PoseStack poseStack, MultiBufferSource vertexConsumerProvider, int i,
                                PlayerRenderState state, float f, float g, CallbackInfo ci) {
        CapeRenderContext.clear();
    }
}
