package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.module.modules.misc.Streamer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class MixinAbstractClientPlayerEntity {
    @Shadow
    @Nullable
    protected abstract PlayerListEntry getPlayerListEntry();

    @Redirect(
            method = "getSkinTextures",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getPlayerListEntry()Lnet/minecraft/client/network/PlayerListEntry;"
            )
    )
    private PlayerListEntry getEntry(AbstractClientPlayerEntity instance) {
        if (instance == BlackOut.mc.player) {
            Streamer streamer = Streamer.getInstance();
            if (streamer.enabled && streamer.skin.get()) {
                return null;
            }
        }

        return this.getPlayerListEntry();
    }
}
