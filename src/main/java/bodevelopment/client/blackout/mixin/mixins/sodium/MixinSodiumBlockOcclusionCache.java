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

package bodevelopment.client.blackout.mixin.mixins.sodium;

import bodevelopment.client.blackout.annotations.Internal;
import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache", remap = false)
@Internal
public class MixinSodiumBlockOcclusionCache {
    @Inject(
            method = "shouldDrawSide",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void onShouldDrawSide(
            BlockState selfState,
            BlockGetter view,
            BlockPos selfPos,
            Direction facing,
            CallbackInfoReturnable<Boolean> cir
    ) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        if (xray.isTarget(selfState.getBlock())) {
            cir.setReturnValue(true);
        }
    }
}