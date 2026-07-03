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

package bodevelopment.client.blackout.randomstuff;

import bodevelopment.client.blackout.enums.HoleType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class Hole {
    public static final BlockPos[] POSITIONS = new BlockPos[0];
    public final BlockPos pos;
    public final HoleType type;
    public final BlockPos[] positions;
    public final Vec3 middle;

    public Hole(BlockPos pos, HoleType type) {
        this.pos = pos;
        this.type = type;
        switch (type) {
            case Single:
                this.positions = new BlockPos[]{pos};
                this.middle = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                break;
            case DoubleX:
                this.positions = new BlockPos[]{pos, pos.offset(1, 0, 0)};
                this.middle = new Vec3(pos.getX() + 1, pos.getY(), pos.getZ() + 0.5);
                break;
            case DoubleZ:
                this.positions = new BlockPos[]{pos, pos.offset(0, 0, 1)};
                this.middle = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 1);
                break;
            case Quad:
                this.positions = new BlockPos[]{pos, pos.offset(1, 0, 0), pos.offset(0, 0, 1), pos.offset(1, 0, 1)};
                this.middle = new Vec3(pos.getX() + 1, pos.getY(), pos.getZ() + 1);
                break;
            default:
                this.positions = POSITIONS;
                this.middle = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        }
    }
}
