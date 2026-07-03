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

import bodevelopment.client.blackout.interfaces.mixin.IAbstractContainerScreen;
import bodevelopment.client.blackout.module.modules.visual.misc.ShulkerViewer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
@Internal
public abstract class MixinAbstractContainerScreen implements IAbstractContainerScreen {
    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    @Override
    public Slot blackout_Client$getFocusedSlot() {
        return this.hoveredSlot;
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    private void onDrawTooltipPre(GuiGraphics context, int mouseX, int mouseY, CallbackInfo ci) {
        ShulkerViewer module = ShulkerViewer.getInstance();
        if (module != null && module.enabled && module.isHoveringShulker()) {
            module.renderOnTop(context, mouseX, mouseY);
            ci.cancel();
        }
    }
}
