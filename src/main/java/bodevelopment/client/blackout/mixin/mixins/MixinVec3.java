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

import bodevelopment.client.blackout.interfaces.mixin.IVec3;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Vec3.class)
@Internal
public class MixinVec3 implements IVec3 {
    @Mutable
    @Shadow
    @Final
    public double x;
    @Mutable
    @Shadow
    @Final
    public double y;
    @Mutable
    @Shadow
    @Final
    public double z;

    @Override
    public void blackout_Client$set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void blackout_Client$setXZ(double x, double z) {
        this.x = x;
        this.z = z;
    }

    @Override
    public void blackout_Client$setX(double x) {
        this.x = x;
    }

    @Override
    public void blackout_Client$setY(double y) {
        this.y = y;
    }

    @Override
    public void blackout_Client$setZ(double z) {
        this.z = z;
    }
}
