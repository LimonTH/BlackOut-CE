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

package bodevelopment.client.blackout.mixin.mixins.sodium;

import bodevelopment.client.blackout.annotations.Internal;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.occlusion.VisibilityEncoding", remap = false)
@Internal
public class MixinSodiumVisibilityEncoding {
    @Inject(method = "getConnections(JI)I", at = @At("HEAD"), cancellable = true)
    private static void onGetConnectionsIncoming(long visibilityData, int incoming, CallbackInfoReturnable<Integer> cir) {
        if (XRay.getInstance().enabled) {
            cir.setReturnValue(63);
        }
    }

    @Inject(method = "getConnections(J)I", at = @At("HEAD"), cancellable = true)
    private static void onGetConnections(long visibilityData, CallbackInfoReturnable<Integer> cir) {
        if (XRay.getInstance().enabled) {
            cir.setReturnValue(63);
        }
    }
}