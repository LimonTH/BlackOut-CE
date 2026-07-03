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

package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.PlayerUtils;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

public class SafeWalk extends Module {
    private static SafeWalk INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Boolean> sneak = this.sgGeneral.booleanSetting("Shift Simulation", false, "Forces the player into a sneaking state when near a block edge to prevent falling.");

    public SafeWalk() {
        super("Safe Walk", "Prevents the player from walking off the edge of blocks, simulating the safety mechanics of sneaking.", SubCategory.MOVEMENT, true);
        INSTANCE = this;
    }

    public static boolean shouldSafeWalk() {
        return INSTANCE.active();
    }

    @Override
    public boolean shouldSkipListeners() {
        return !this.active();
    }

    @Event
    public void onTick(TickEvent.Post event) {
        if (PlayerUtils.isInGame() && this.sneak.get()) {
            Vec3 movement = BlackOut.mc.player.getDeltaMovement();
            Vec3 newMovement = BlackOut.mc.player.maybeBackOffFromEdge(movement, MoverType.SELF);
            if (!movement.equals(newMovement)) {
                BlackOut.mc.player.setShiftKeyDown(true);
                BlackOut.mc.options.keyShift.setDown(true);
            } else {
                BlackOut.mc.player.setShiftKeyDown(false);
                BlackOut.mc.options.keyShift.setDown(false);
            }
        }
    }

    private boolean active() {
        if (this.enabled) {
            return true;
        } else {
            Scaffold scaffold = Scaffold.getInstance();
            return scaffold.enabled && scaffold.safeWalk.get();
        }
    }
}
