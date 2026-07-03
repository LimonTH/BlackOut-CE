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

import bodevelopment.client.blackout.interfaces.mixin.ILine;
import net.minecraft.client.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiMessage.Line.class)
@Internal
public class MixinLine implements ILine {
    @Unique
    private int id;
    @Unique
    private GuiMessage line;

    @Override
    public void blackout_Client$set(int id) {
        this.id = id;
    }

    @Override
    public boolean blackout_Client$idEquals(int id) {
        return this.id == id;
    }

    @Override
    public boolean blackout_Client$messageEquals(GuiMessage other) {
        if (this.line == null) {
            return false;
        }
        return this.line.equals(other);
    }

    @Override
    public void blackout_Client$setLine(GuiMessage line) {
        this.line = line;
    }
}
