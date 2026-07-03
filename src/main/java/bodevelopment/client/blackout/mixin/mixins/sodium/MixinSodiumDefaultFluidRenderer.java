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
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer", remap = false)
@Internal
public class MixinSodiumDefaultFluidRenderer {
    @Unique
    private static final ThreadLocal<Integer> XRAY_FLUID_ALPHA = ThreadLocal.withInitial(() -> -1);
    @Final
    @Shadow
    private QuadLightData quadLightData;
    @Final
    @Shadow
    private float[] brightness;
    @Final
    @Shadow
    private int[] quadColors;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onRenderHead(
            LevelSlice level,
            BlockState blockState,
            FluidState fluidState,
            BlockPos blockPos,
            BlockPos offset,
            TranslucentGeometryCollector collector,
            ChunkModelBuilder meshBuilder,
            Material material,
            ColorProvider<FluidState> colorProvider,
            TextureAtlasSprite[] sprites,
            CallbackInfo ci
    ) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) {
            XRAY_FLUID_ALPHA.set(-1);
            return;
        }
        if (xray.isTarget(blockState.getBlock())) {
            XRAY_FLUID_ALPHA.set(-1);
            return;
        }

        final int opacity = xray.opacity.get();
        if (opacity <= 0) {
            ci.cancel();
            return;
        }
        XRAY_FLUID_ALPHA.set(opacity < 255 ? opacity : -1);
    }

    @Inject(method = "render", at = @At("RETURN"), remap = false, require = 0)
    private void onRenderReturn(CallbackInfo ci) {
        XRAY_FLUID_ALPHA.set(-1);
    }

    @Inject(method = "updateQuad", at = @At("RETURN"), remap = false, require = 0)
    private void onUpdateQuadReturn(CallbackInfo ci) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;

        Arrays.fill(this.brightness, 1.0f);
        for (int i = 0; i < 4; i++) {
            this.quadLightData.lm[i] = 0xF000F0;
            this.quadLightData.br[i] = 1.0f;
        }

        final int alpha = XRAY_FLUID_ALPHA.get();
        if (alpha < 0) return;

        final int alphaShifted = alpha << 24;
        for (int i = 0; i < 4; i++) {
            this.quadColors[i] = (this.quadColors[i] & 0x00FFFFFF) | alphaShifted;
        }
    }
}