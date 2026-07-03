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

package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.Experimental;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

@Experimental
public class XRay extends Module {
    private static XRay INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Integer> opacity = this.sgGeneral.intSetting("Opacity", 0, 0, 255, 1, "The alpha transparency level applied to non-target blocks during world rendering.").onChanged(v -> {
        if (this.enabled) {
            BlackOut.mc.levelRenderer.allChanged();
        }
    });
    public final Setting<List<Block>> targetBlocks = this.sgGeneral.blockListSetting("Blocks Filter Registry", "A definitive list of block types that will remain opaque and visible through terrain.",
            Blocks.ANCIENT_DEBRIS,
            Blocks.DIAMOND_ORE,
            Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.GOLD_ORE,
            Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.EMERALD_ORE,
            Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.IRON_ORE,
            Blocks.DEEPSLATE_IRON_ORE,
            Blocks.COPPER_ORE,
            Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.COAL_ORE,
            Blocks.DEEPSLATE_COAL_ORE,
            Blocks.REDSTONE_ORE,
            Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.LAPIS_ORE,
            Blocks.DEEPSLATE_LAPIS_ORE
    ).onChanged(v -> {
        if (this.enabled) {
            BlackOut.mc.levelRenderer.allChanged();
        }
    });

    public XRay() {
        super("Xray", "Modifies world rendering to isolate selected blocks by applying selective transparency to common terrain blocks.", SubCategory.MISC_VISUAL, true);
        INSTANCE = this;
    }

    public static XRay getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        BlackOut.mc.levelRenderer.allChanged();
    }

    @Override
    public void onDisable() {
        BlackOut.mc.levelRenderer.allChanged();
    }

    public boolean isTarget(Block block) {
        return targetBlocks.get().contains(block);
    }
}