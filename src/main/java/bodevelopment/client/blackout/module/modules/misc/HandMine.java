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

import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.util.BlockUtils;
import bodevelopment.client.blackout.util.InvUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public class HandMine extends Module {
    private static HandMine INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<SwitchMode> switchMode = this.sgGeneral.enumSetting("Swap Method", SwitchMode.InvSwitch, "The mechanism used to switch to the optimal tool.");
    private final Setting<Boolean> allowInventory = this.sgGeneral.booleanSetting("Scan Inventory", false, "Allows the module to utilize tools located in the player's inventory, not just the hotbar.", () -> this.switchMode.get().inventory);
    private final Setting<Double> speed = this.sgGeneral.doubleSetting("Mining Speed", 1.0, 0.0, 2.0, 0.02, "A multiplier applied to the mining speed of the selected tool.");

    public HandMine() {
        super("Hand Mine", "Swaps to the most effective tool during the final stage of breaking a block to maximize efficiency.", SubCategory.MISC, false);
        INSTANCE = this;
    }

    public static HandMine getInstance() {
        return INSTANCE;
    }

    public void onEnd(BlockPos pos, Runnable packet) {
        FindResult best = this.bestSlot(pos);
        if (best.wasFound()) {
            if (this.miningDelta(pos, best.stack()) < this.miningDelta(pos, Managers.PACKET.getStack())) {
                packet.run();
            } else {
                this.switchMode.get().swapInstantly(best.slot());
                packet.run();
                this.switchMode.get().swapBackInstantly();
            }
        }
    }

    public void onInstant(BlockPos pos, Runnable packet) {
        FindResult best = this.bestSlot(pos);
        if (best.wasFound()) {
            if (this.miningDelta(pos, Managers.PACKET.getStack()) >= 1.0) {
                packet.run();
            } else {
                this.switchMode.get().swapInstantly(best.slot());
                packet.run();
                this.switchMode.get().swapBackInstantly();
            }
        }
    }

    public float getDelta(BlockPos pos, float vanilla) {
        FindResult best = this.bestSlot(pos);
        return !best.wasFound() ? vanilla : (float) Math.max(this.miningDelta(pos, best.stack()), this.miningDelta(pos, Managers.PACKET.getStack()));
    }

    private FindResult bestSlot(BlockPos pos) {
        return InvUtils.findBest(true, this.switchMode.get().inventory && this.allowInventory.get(), stack -> this.miningDelta(pos, stack));
    }

    private double miningDelta(BlockPos pos, ItemStack stack) {
        return BlockUtils.getBlockBreakingDelta(pos, stack) * this.speed.get();
    }
}
