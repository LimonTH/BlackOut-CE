package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.entities.PlayerModifier;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerEntityRenderer.class)
public class MixinPlayerEntityRenderer {
    @Redirect(method = "setModelPose", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;isInSneakingPose()Z"))
    private boolean sneak(AbstractClientPlayerEntity instance) {
        PlayerModifier modifier = PlayerModifier.getInstance();
        return modifier.enabled && modifier.forceSneak.get() || instance.isInSneakingPose();
    }
}
