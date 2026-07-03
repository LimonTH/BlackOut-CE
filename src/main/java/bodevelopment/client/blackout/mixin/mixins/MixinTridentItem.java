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

import bodevelopment.client.blackout.module.modules.movement.FastRiptide;
import net.minecraft.world.item.TridentItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TridentItem.class)
@Internal
public abstract class MixinTridentItem {
    @ModifyVariable(method = "releaseUsing", at = @At("HEAD"), index = 4, argsOnly = true)
    private int preUse(int value) {
        FastRiptide fastRiptide = FastRiptide.getInstance();
        if (!fastRiptide.enabled) {
            return value;
        } else if (System.currentTimeMillis() - fastRiptide.prevRiptide < fastRiptide.cooldown.get() * 1000.0) {
            return Integer.MAX_VALUE;
        } else {
            fastRiptide.prevRiptide = System.currentTimeMillis();
            return 0;
        }
    }
}
