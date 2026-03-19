package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.module.modules.misc.Streamer;
import bodevelopment.client.blackout.util.Capes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
public class MixinCapeLayer {
    @Inject(method = "render*", at = @At("HEAD"))
    private void onRenderHead(PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i, PlayerRenderState state, float f, float g, CallbackInfo ci) {
        if (state.id == BlackOut.mc.player.getId()) {
            Streamer streamer = Streamer.getInstance();
            if (streamer.enabled && streamer.skin.get()) {
                state.showCape = false;
                return;
            }
        }

        if (Capes.getCape(state) != null) {
            state.showCape = true;
        }
    }

    @Redirect(
            method = "render*",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/PlayerSkin;capeTexture()Lnet/minecraft/resources/ResourceLocation;"
            )
    )
    private ResourceLocation redirectCapeTexture(PlayerSkin instance, PoseStack matrices, MultiBufferSource vcp, int light, PlayerRenderState state) {
        ResourceLocation custom = Capes.getCape(state);
        return custom != null ? custom : instance.capeTexture();
    }
}
