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

package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.PublicAPI;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.BlockStateEvent;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.module.modules.misc.Simulation;
import bodevelopment.client.blackout.randomstuff.timers.TimerMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

@PublicAPI
public class BlockManager extends Manager {
    private final TimerMap<BlockPos, SpoofedBlock> timers = new TimerMap<>(true);

    public void set(BlockPos pos, Block type, boolean damage, boolean placing) {
        this.timers.add(pos, new SpoofedBlock(type, damage, placing), 1.0);
    }

    public void reset(BlockPos pos) {
        if (this.timers.containsKey(pos)) {
            this.timers.removeKey(pos);
        }
    }

    public BlockState damageState(BlockPos pos) {
        if (Simulation.getInstance().blocks() && this.timers.containsKey(pos)) {
            SpoofedBlock block = this.timers.get(pos);
            if (block != null && block.damage()) {
                return block.type().defaultBlockState();
            }
        }

        return BlackOut.mc.level.getBlockState(pos);
    }

    public BlockState blockState(BlockPos pos) {
        if (Simulation.getInstance().blocks() && this.timers.containsKey(pos)) {
            SpoofedBlock block = this.timers.get(pos);
            if (block != null && block.placing()) {
                return block.type().defaultBlockState();
            }
        }

        return BlackOut.mc.level.getBlockState(pos);
    }

    @Override
    public void init() {
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
    }

    @Event
    public void onBlock(BlockStateEvent event) {
        this.reset(event.pos);
    }

    private record SpoofedBlock(Block type, boolean damage, boolean placing) {
    }
}
