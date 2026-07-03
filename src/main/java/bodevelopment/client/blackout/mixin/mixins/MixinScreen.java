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

import bodevelopment.client.blackout.gui.menu.MainMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
@Internal
public abstract class MixinScreen {
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!((Object) this instanceof TitleScreen)) {
            float fade = MainMenu.globalFade;
            if (fade < 1.0F) {
                MainMenu.globalFade = Math.min(1.0F, MainMenu.globalFade + 0.015F);
                RenderSystem.setShaderColor(fade, fade, fade, 1.0F);
            }
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRenderReturn(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}