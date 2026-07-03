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

package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class
NCPRaytracer {
    public static boolean raytrace(Vec3 from, Vec3 to, AABB box) {
        int lx = 0;
        int ly = 0;
        int lz = 0;

        for (double delta = 0.0; delta < 1.0; delta += 0.001F) {
            double x = Mth.lerp(from.x, to.x, delta);
            double y = Mth.lerp(from.y, to.y, delta);
            double z = Mth.lerp(from.z, to.z, delta);
            if (box.contains(x, y, z)) {
                return true;
            }

            int ix = (int) Math.floor(x);
            int iy = (int) Math.floor(y);
            int iz = (int) Math.floor(z);
            if (lx != ix || ly != iy || lz != iz) {
                BlockPos pos = new BlockPos(ix, iy, iz);
                if (validForCheck(pos, BlackOut.mc.level.getBlockState(pos))) {
                    return false;
                }
            }

            lx = ix;
            ly = iy;
            lz = iz;
        }

        return false;
    }

    public static boolean validForCheck(BlockPos pos, BlockState state) {
        if (!state.getCollisionShape(BlackOut.mc.level, pos).isEmpty()) {
            return true;
        } else if (state.getBlock() instanceof LiquidBlock) {
            return false;
        } else if (state.getBlock() instanceof StairBlock) {
            return false;
        } else {
            return !state.hasBlockEntity() && state.isCollisionShapeFullBlock(BlackOut.mc.level, pos);
        }
    }
}
