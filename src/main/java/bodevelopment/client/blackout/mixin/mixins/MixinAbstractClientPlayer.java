package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.module.modules.misc.Streamer;
import bodevelopment.client.blackout.module.modules.misc.Zoomify;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class MixinAbstractClientPlayer {
    @Shadow
    @Nullable
    protected abstract PlayerInfo getPlayerInfo();

    @Redirect(
            method = "getSkin",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/AbstractClientPlayer;getPlayerInfo()Lnet/minecraft/client/multiplayer/PlayerInfo;"
            )
    )
    private PlayerInfo getEntry(AbstractClientPlayer instance) {
        if (instance == BlackOut.mc.player) {
            Streamer streamer = Streamer.getInstance();
            if (streamer.enabled && streamer.skin.get()) {
                return null;
            }
        }

        return this.getPlayerInfo();
    }

    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
    private void onGetFovMultiplier(CallbackInfoReturnable<Float> cir) {
        Zoomify zoom = Zoomify.getInstance();

        if (zoom != null && zoom.enabled) {
            cir.setReturnValue(zoom.getZoomFactor(cir.getReturnValue()));
        }
    }
}
