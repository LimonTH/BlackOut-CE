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

package bodevelopment.client.blackout.mixin.mixins.fabricapi;

import bodevelopment.client.blackout.annotations.Internal;
import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;
import java.util.function.Supplier;

@Mixin(FabricBakedModel.class)
@Internal
public interface MixinFabricBakedModel {
    @Inject(method = "emitBlockQuads", at = @At("HEAD"), cancellable = true)
    default void onEmitBlockQuadsHead(
            QuadEmitter emitter,
            BlockAndTintGetter blockView,
            BlockState state,
            BlockPos pos,
            Supplier<RandomSource> randomSupplier,
            Predicate<@Nullable Direction> cullTest,
            CallbackInfo ci
    ) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled || state == null) return;
        if (xray.isTarget(state.getBlock())) return;

        final int opacity = xray.opacity.get();
        if (opacity <= 0) {
            ci.cancel();
            return;
        }
        if (opacity >= 255) return;

        emitter.pushTransform(quad -> {
            for (int v = 0; v < 4; v++) {
                final int color = quad.color(v);
                quad.color(v, (color & 0x00FFFFFF) | (opacity << 24));
                quad.lightmap(v, 15728880);
            }
            return true;
        });
    }

    @Inject(method = "emitBlockQuads", at = @At("RETURN"))
    default void onEmitBlockQuadsReturn(
            QuadEmitter emitter,
            BlockAndTintGetter blockView,
            BlockState state,
            BlockPos pos,
            Supplier<RandomSource> randomSupplier,
            Predicate<@Nullable Direction> cullTest,
            CallbackInfo ci
    ) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled || state == null) return;
        if (xray.isTarget(state.getBlock())) return;

        final int opacity = xray.opacity.get();
        if (opacity <= 0 || opacity >= 255) return;

        emitter.popTransform();
    }
}
