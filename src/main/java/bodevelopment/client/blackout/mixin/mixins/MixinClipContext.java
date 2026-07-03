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

import bodevelopment.client.blackout.interfaces.mixin.IClipContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClipContext.class)
@Internal
public class MixinClipContext implements IClipContext {
    @Mutable
    @Shadow
    @Final
    private ClipContext.Block block;
    @Mutable
    @Shadow
    @Final
    private ClipContext.Fluid fluid;
    @Mutable
    @Shadow
    @Final
    private CollisionContext collisionContext;
    @Mutable
    @Shadow
    @Final
    private Vec3 from;
    @Mutable
    @Shadow
    @Final
    private Vec3 to;

    @Override
    public void blackout_Client$set(Vec3 start, Vec3 end, ClipContext.Block shapeType, ClipContext.Fluid fluidHandling, Entity entity) {
        this.block = shapeType;
        this.fluid = fluidHandling;
        this.collisionContext = CollisionContext.of(entity);
        this.from = start;
        this.to = end;
    }

    @Override
    public void blackout_Client$set(Vec3 start, Vec3 end) {
        this.from = start;
        this.to = end;
    }

    @Override
    public void blackout_Client$set(ClipContext.Block shapeType, ClipContext.Fluid fluidHandling, Entity entity) {
        this.block = shapeType;
        this.fluid = fluidHandling;
        this.collisionContext = CollisionContext.of(entity);
    }

    @Override
    public void blackout_Client$setStart(Vec3 start) {
        this.from = start;
    }

    @Override
    public void blackout_Client$setEnd(Vec3 end) {
        this.to = end;
    }
}
