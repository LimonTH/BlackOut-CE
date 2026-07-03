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

import bodevelopment.client.blackout.module.modules.visual.world.Ambience;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
@Internal
public class MixinLevel {
    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void getRain(float delta, CallbackInfoReturnable<Float> cir) {
        Ambience ambience = Ambience.getInstance();
        if (ambience.enabled && ambience.modifyWeather.get()) {
            cir.setReturnValue(ambience.raining.get().floatValue());
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void getThunder(float delta, CallbackInfoReturnable<Float> cir) {
        Ambience ambience = Ambience.getInstance();
        if (ambience.enabled && ambience.modifyWeather.get()) {
            cir.setReturnValue(ambience.thunder.get().floatValue());
        }
    }
}
