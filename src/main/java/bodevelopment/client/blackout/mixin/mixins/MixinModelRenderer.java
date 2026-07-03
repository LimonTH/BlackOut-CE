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
import bodevelopment.client.blackout.module.modules.visual.world.Brightness;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ModelBlockRenderer.class)
@Internal
public class MixinModelRenderer {
    @ModifyVariable(method = "renderModel", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int boostLight(int light) {
        if (Brightness.getInstance().enabled) {
            if (Brightness.getInstance().mode.get() == Brightness.Mode.Gamma) {
                return 15728880;
            }

            if (Brightness.getInstance().mode.get() == Brightness.Mode.Luminance) {
                int sky = (light >> 20) & 15;
                int block = (light >> 4) & 15;

                int lLevel = Brightness.getInstance().luminanceLevel.get();

                int newSky = Math.max(sky, lLevel);
                int newBlock = Math.max(block, lLevel);

                return (newSky << 20) | (newBlock << 4);
            }
        }
        return light;
    }
}