/*
 * Blackout Client (CE) - A cutting-edge, feature-rich cheat client for Minecraft.
 * A modernized continuation of the original Blackout project by OLEPOSSU & KassuK.
 * Copyright (C) 2026  LimonTH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.Internal;
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
@Internal
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
