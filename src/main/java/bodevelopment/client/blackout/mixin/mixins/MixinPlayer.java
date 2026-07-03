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
import bodevelopment.client.blackout.module.modules.misc.AntiPose;
import bodevelopment.client.blackout.module.modules.movement.SafeWalk;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
@Internal
public abstract class MixinPlayer {
    @Shadow
    protected abstract boolean canPlayerFitWithinBlocksAndEntitiesWhen(Pose pose);

    @Shadow
    public abstract boolean isStayingOnGroundSurface();

    @Redirect(method = "maybeBackOffFromEdge", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isStayingOnGroundSurface()Z"))
    private boolean sneakingThing(Player instance) {
        if ((Object) this == BlackOut.mc.player && SafeWalk.shouldSafeWalk()) {
            return true;
        }
        return this.isStayingOnGroundSurface();
    }

    @Redirect(
            method = "updatePlayerPose",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;canPlayerFitWithinBlocksAndEntitiesWhen(Lnet/minecraft/world/entity/Pose;)Z", ordinal = 1)
    )
    private boolean canEnterPose(Player instance, Pose pose) {
        return instance == BlackOut.mc.player && AntiPose.getInstance().enabled || this.canPlayerFitWithinBlocksAndEntitiesWhen(pose);
    }
}
