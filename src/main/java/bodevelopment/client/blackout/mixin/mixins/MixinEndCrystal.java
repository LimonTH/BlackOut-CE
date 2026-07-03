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

import bodevelopment.client.blackout.interfaces.mixin.IEndCrystal;
import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndCrystal.class)
@Internal
public class MixinEndCrystal implements IEndCrystal {
    @Unique
    private final long spawnTime = System.currentTimeMillis();
    @Unique
    private boolean isOwn = false;

    @Override
    public long blackout_Client$getSpawnTime() {
        return this.spawnTime;
    }

    @Override
    public boolean blackout_Client$isOwn() {
        return this.isOwn;
    }

    @Override
    public void blackout_Client$markOwn() {
        this.isOwn = true;
    }

    @Inject(method = "showsBottom", at = @At("HEAD"), cancellable = true)
    private void cancelBottom(CallbackInfoReturnable<Boolean> cir) {
        NoRender noRender = NoRender.getInstance();
        if (noRender.enabled && noRender.crystalBase.get()) {
            cir.setReturnValue(false);
            return;
        }

        // Всегда прячем платформу для кристаллов, поставленных нами
        if (this.isOwn) {
            cir.setReturnValue(false);
        }
    }
}
