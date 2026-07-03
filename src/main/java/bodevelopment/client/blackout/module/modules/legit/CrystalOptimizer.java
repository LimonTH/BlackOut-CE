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

package bodevelopment.client.blackout.module.modules.legit;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.mixin.accessors.AccessorServerboundInteractPacket;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

public class CrystalOptimizer extends Module {
    private static CrystalOptimizer INSTANCE;

    public CrystalOptimizer() {
        super("Crystal Optimizer", "Improves combat fluidness by instantly removing crystal entities from the client-side world upon attack.", SubCategory.LEGIT, true);
        INSTANCE = this;
    }

    public static CrystalOptimizer getInstance() {
        return INSTANCE;
    }

    @Event
    public void onSent(PacketEvent.Sent event) {
        if (event.packet instanceof ServerboundInteractPacket packet
                && ((AccessorServerboundInteractPacket) packet).getType().getType() == ServerboundInteractPacket.ActionType.ATTACK
                && BlackOut.mc.level.getEntity(((AccessorServerboundInteractPacket) packet).getId()) instanceof EndCrystal entity) {
            BlackOut.mc.level.removeEntity(entity.getId(), Entity.RemovalReason.KILLED);
        }
    }
}
