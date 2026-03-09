package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.module.modules.misc.Streamer;
import bodevelopment.client.blackout.util.Capes;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeFeatureRenderer.class)
public class MixinCapeFeatureRenderer {

    @Inject(method = "render*", at = @At("HEAD"))
    private void onRenderHead(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, PlayerEntityRenderState state, float f, float g, CallbackInfo ci) {
        if (state.id == BlackOut.mc.player.getId()) {
            Streamer streamer = Streamer.getInstance();
            if (streamer.enabled && streamer.skin.get()) {
                state.capeVisible = false;
                return;
            }
        }

        if (Capes.getCape(state) != null) {
            state.capeVisible = true;
        }
    }

    @Redirect(
            method = "render*",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/SkinTextures;capeTexture()Lnet/minecraft/util/Identifier;"
            )
    )
    private Identifier redirectCapeTexture(SkinTextures instance, MatrixStack matrices, VertexConsumerProvider vcp, int light, PlayerEntityRenderState state) {
        Identifier custom = Capes.getCape(state);
        return custom != null ? custom : instance.capeTexture();
    }
}
