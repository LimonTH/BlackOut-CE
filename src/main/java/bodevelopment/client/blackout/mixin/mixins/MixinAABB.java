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
import bodevelopment.client.blackout.interfaces.mixin.IAABB;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AABB.class)
@Internal
public class MixinAABB implements IAABB {
    @Mutable @Shadow @Final public double minX;
    @Mutable @Shadow @Final public double minY;
    @Mutable @Shadow @Final public double minZ;
    @Mutable @Shadow @Final public double maxX;
    @Mutable @Shadow @Final public double maxY;
    @Mutable @Shadow @Final public double maxZ;

    @Override
    public void blackout_Client$set(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    @Override public void blackout_Client$setMinX(double minX) { this.minX = minX; }
    @Override public void blackout_Client$setMinY(double minY) { this.minY = minY; }
    @Override public void blackout_Client$setMinZ(double minZ) { this.minZ = minZ; }
    @Override public void blackout_Client$setMaxX(double maxX) { this.maxX = maxX; }
    @Override public void blackout_Client$setMaxY(double maxY) { this.maxY = maxY; }
    @Override public void blackout_Client$setMaxZ(double maxZ) { this.maxZ = maxZ; }
}
