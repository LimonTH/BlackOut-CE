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

package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.util.BlockUtils;
import bodevelopment.client.blackout.util.InvUtils;
import bodevelopment.client.blackout.util.PlayerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class AutoTool extends Module {
    public AutoTool() {
        super("Auto Tool", "Automatically selects the most efficient tool from the hotbar based on the block currently being broken.", SubCategory.MISC, true);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (PlayerUtils.isInGame()) {
            if (BlackOut.mc.gameMode.isDestroying()) {
                BlockPos pos = BlackOut.mc.gameMode.destroyBlockPos;
                if (pos != null) {
                    FindResult best = this.bestSlot(pos);
                    if (best.wasFound()) {
                        if (!(this.miningDelta(pos, best.stack()) <= this.miningDelta(pos, Managers.PACKET.getStack()))) {
                            SwitchMode.Normal.swap(best.slot());
                        }
                    }
                }
            }
        }
    }

    private FindResult bestSlot(BlockPos pos) {
        return InvUtils.findBest(true, false, stack -> this.miningDelta(pos, stack));
    }

    private double miningDelta(BlockPos pos, ItemStack stack) {
        double delta = BlockUtils.getBlockBreakingDelta(pos, stack);

        if (stack.getItem() instanceof SwordItem && BlackOut.mc.level != null) {
            BlockState state = BlackOut.mc.level.getBlockState(pos);

            if (state.is(BlockTags.SWORD_EFFICIENT) || state.is(Blocks.BAMBOO) || state.is(Blocks.BAMBOO_SAPLING)) {
                delta *= 1.5;
            }
        }

        return delta;
    }
}
