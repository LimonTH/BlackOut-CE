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

package bodevelopment.client.blackout.interfaces.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

public interface IClipContext {
    void blackout_Client$set(Vec3 startPos, Vec3 endPos, ClipContext.Block shapeType, ClipContext.Fluid fluidHandling, Entity entity);

    void blackout_Client$set(Vec3 startPos, Vec3 endPos);

    void blackout_Client$set(ClipContext.Block shapeType, ClipContext.Fluid fluidHandling, Entity entity);

    void blackout_Client$setStart(Vec3 startPos);

    void blackout_Client$setEnd(Vec3 endPos);
}
