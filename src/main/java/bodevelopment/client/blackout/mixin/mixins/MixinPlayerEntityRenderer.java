package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.interfaces.mixin.IUUIDHolder;
import bodevelopment.client.blackout.module.modules.visual.entities.PlayerModifier;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public class MixinPlayerEntityRenderer {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V",
            at = @At("RETURN")
    )
    private void onUpdateRenderState(AbstractClientPlayer entity, PlayerRenderState state, float tickDelta, CallbackInfo ci) {
        ((IUUIDHolder) state).blackout$setUUID(entity.getUUID());

        PlayerModifier modifier = PlayerModifier.getInstance();

        if (modifier.enabled) {
            if (modifier.forceSneak.get()) {
                state.isCrouching = true;
            }
        }
    }
}
