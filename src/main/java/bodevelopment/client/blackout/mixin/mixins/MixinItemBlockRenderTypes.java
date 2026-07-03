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

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemBlockRenderTypes.class)
@Internal
public class MixinItemBlockRenderTypes {
    @Inject(method = "getChunkRenderType", at = @At("RETURN"), cancellable = true)
    private static void onGetChunkRenderType(BlockState blockState, CallbackInfoReturnable<RenderType> cir) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        if (xray.isTarget(blockState.getBlock())) return;

        final int opacity = xray.opacity.get();
        if (opacity > 0 && opacity < 255) {
            cir.setReturnValue(RenderType.translucent());
        }
    }

    @Inject(method = "getRenderLayer", at = @At("RETURN"), cancellable = true)
    private static void onGetFluidLayer(FluidState fluidState, CallbackInfoReturnable<RenderType> cir) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        if (xray.isTarget(fluidState.createLegacyBlock().getBlock())) return;

        final int opacity = xray.opacity.get();
        if (opacity > 0 && opacity < 255) {
            cir.setReturnValue(RenderType.translucent());
        }
    }
}