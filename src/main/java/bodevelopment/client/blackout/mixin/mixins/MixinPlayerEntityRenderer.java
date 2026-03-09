package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IUUIDHolder;
import bodevelopment.client.blackout.module.modules.visual.entities.PlayerModifier;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class MixinPlayerEntityRenderer {

    @Inject(
            method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("RETURN")
    )
    private void onUpdateRenderState(AbstractClientPlayerEntity entity, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        ((IUUIDHolder) state).blackout$setUUID(entity.getUuid());

        PlayerModifier modifier = PlayerModifier.getInstance();

        if (modifier.enabled) {
            if (modifier.forceSneak.get()) {
                state.isInSneakingPose = true;
            }
        }
    }
}
