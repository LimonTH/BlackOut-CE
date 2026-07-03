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
import bodevelopment.client.blackout.annotations.PublicAPI;
import bodevelopment.client.blackout.enums.HoleType;
import bodevelopment.client.blackout.randomstuff.Hole;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

@PublicAPI
public class HoleUtils {
    public static Hole getHole(BlockPos pos) {
        return getHole(pos, true, true, true, 3, true);
    }

    public static Hole getHole(BlockPos pos, int depth) {
        return getHole(pos, depth, true);
    }

    public static Hole getHole(BlockPos pos, int depth, boolean floor) {
        return getHole(pos, true, true, true, depth, floor);
    }

    public static Hole getHole(BlockPos pos, boolean single, boolean doubles, boolean quad, int depth, boolean floor) {
        if (!isAir(pos, depth, floor)) {
            return new Hole(pos, HoleType.NotHole);
        }

        if (isBlock(pos.west()) && isBlock(pos.north())) {
            boolean xOpen = isAir(pos.east(), depth, floor) && isBlock(pos.east().north()) && isBlock(pos.east(2));
            boolean zOpen = isAir(pos.south(), depth, floor) && isBlock(pos.south().west()) && isBlock(pos.south(2));

            if (single && !xOpen && !zOpen && isBlock(pos.east()) && isBlock(pos.south())) {
                return new Hole(pos, HoleType.Single);
            } else if (quad && xOpen && zOpen
                    && isAir(pos.south().east(), depth, floor)
                    && isBlock(pos.east().east().south())
                    && isBlock(pos.south().south().east())) {
                return new Hole(pos, HoleType.Quad);
            } else if (doubles && xOpen && !zOpen && isBlock(pos.south()) && isBlock(pos.south().east())) {
                return new Hole(pos, HoleType.DoubleX);
            } else if (doubles && zOpen && !xOpen && isBlock(pos.east()) && isBlock(pos.south().east())) {
                return new Hole(pos, HoleType.DoubleZ);
            }
        }

        return new Hole(pos, HoleType.NotHole);
    }

    static boolean isBlock(BlockPos pos) {
        if (BlackOut.mc.level == null) return false;
        return BlockUtils.collidable(pos) && BlockUtils.hasCollision(pos) && BlockUtils.isSafe(pos);
    }

    static boolean isAir(BlockPos pos, int depth, boolean floor) {
        if (BlackOut.mc.level == null) return false;

        if (floor && !isBlock(pos.below())) return false;

        for (int i = 0; i < depth; i++) {
            BlockState state = BlackOut.mc.level.getBlockState(pos.above(i));
            if (!state.isAir() && !state.canBeReplaced() && !BlockUtils.replaceable(pos.above(i))) {
                return false;
            }
        }
        return true;
    }

    public static Hole currentHole(BlockPos pos) {
        for (int x = -1; x <= 0; x++) {
            for (int z = -1; z <= 0; z++) {
                Hole hole = getHole(pos.offset(x, 0, z), 1);
                if (hole.type != HoleType.NotHole) {
                    if (isPosInHole(pos, hole)) return hole;
                }
            }
        }
        return new Hole(pos, HoleType.NotHole);
    }

    private static boolean isPosInHole(BlockPos pos, Hole hole) {
        return switch (hole.type) {
            case Single -> pos.equals(hole.pos);
            case DoubleX -> pos.equals(hole.pos) || pos.equals(hole.pos.east());
            case DoubleZ -> pos.equals(hole.pos) || pos.equals(hole.pos.south());
            case Quad -> pos.getX() >= hole.pos.getX() && pos.getX() <= hole.pos.getX() + 1
                    && pos.getZ() >= hole.pos.getZ() && pos.getZ() <= hole.pos.getZ() + 1;
            default -> false;
        };
    }

    public static boolean inHole(BlockPos pos) {
        return currentHole(pos).type != HoleType.NotHole;
    }
}
