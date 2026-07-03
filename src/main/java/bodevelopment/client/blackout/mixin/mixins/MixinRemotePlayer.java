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

import bodevelopment.client.blackout.annotations.Internal;

import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.combat.misc.NoInterpolation;
import bodevelopment.client.blackout.module.modules.combat.offensive.BackTrack;
import bodevelopment.client.blackout.randomstuff.Pair;
import bodevelopment.client.blackout.randomstuff.timers.TickTimerList;
import bodevelopment.client.blackout.util.BoxUtils;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RemotePlayer.class)
@Internal
public class MixinRemotePlayer {
    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/RemotePlayer;lerpPositionAndRotationStep(IDDDDD)V"))
    private void updatePos(RemotePlayer instance, int steps, double x, double y, double z, double yaw, double pit) {
        BackTrack backTrack = BackTrack.getInstance();
        Vec3 pos = instance.position();
        double[] realPos = this.realPos(instance, steps, x, y, z, yaw, pit);
        backTrack.realPositions.removeKey(instance);
        if (backTrack.enabled) {
            TickTimerList.TickTimer<Pair<RemotePlayer, AABB>> t = backTrack.spoofed
                    .get(timer -> timer.value.getA().equals(instance) && timer.ticks > 3);
            if (t != null) {
                backTrack.realPositions.add(instance, new Vec3(realPos[0], realPos[1], realPos[2]), 1.0);
                this.setPosition(instance, BoxUtils.feet(t.value.getB()), pos);
                return;
            }
        }

        this.setPosition(instance, new Vec3(realPos[0], realPos[1], realPos[2]), pos);
        instance.setYRot((float) realPos[3]);
        instance.setXRot((float) realPos[4]);
    }

    @Unique
    private void setPosition(RemotePlayer instance, Vec3 pos, Vec3 prev) {
        instance.setPos(pos);
        Managers.EXTRAPOLATION.tick(instance, pos.subtract(prev));
    }

    @Unique
    private double[] realPos(RemotePlayer instance, int steps, double x, double y, double z, double yaw, double pit) {
        NoInterpolation noInterpolation = NoInterpolation.getInstance();
        if (!noInterpolation.enabled) {
            double d = 1.0 / steps;
            double e = Mth.lerp(d, instance.getX(), x);
            double f = Mth.lerp(d, instance.getY(), y);
            double g = Mth.lerp(d, instance.getZ(), z);
            float h = (float) Mth.rotLerp(d, instance.getYRot(), yaw);
            float i = (float) Mth.lerp(d, instance.getXRot(), pit);
            return new double[]{e, f, g, h, i};
        } else {
            return new double[]{x, y, z, yaw, pit};
        }
    }
}
