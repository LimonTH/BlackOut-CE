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
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.NotificationsSettings;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.BlockUtils;
import bodevelopment.client.blackout.util.PlayerUtils;

public class AntiVoid extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Mode> mode = this.sgGeneral.enumSetting("Mode", Mode.Motion, "The method used to prevent falling into the void.");
    private final Setting<Double> d = this.sgGeneral.doubleSetting("Activation Distance", 2.5, 0.0, 10.0, 0.5, "The fall distance required before the anti-void logic triggers.");
    private final Setting<Boolean> voidCheck = this.sgGeneral.booleanSetting("Void Check", true, "Only activates if there are no solid blocks directly beneath the player.");

    private double prevOG = 0.0;

    public AntiVoid() {
        super("Anti Void", "Prevents the player from falling into the void by manipulating vertical movement or position.", SubCategory.MOVEMENT, true);
    }

    @Override
    public String getInfo() {
        return this.mode.get().name();
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        if (PlayerUtils.isInGame()) {
            if (BlackOut.mc.player.onGround()) {
                this.prevOG = BlackOut.mc.player.getY();
            }

            if (!(BlackOut.mc.player.fallDistance < this.d.get())) {
                if (!this.voidCheck.get() || this.aboveVoid()) {
                    switch (this.mode.get()) {
                        case Motion:
                            event.setY(this, 0.42);
                            break;
                        case Freeze:
                            event.setY(this, 0.0);
                            break;
                        case Position:
                            BlackOut.mc
                                    .player
                                    .setPos(BlackOut.mc.player.getX(), BlackOut.mc.player.getY() + 1.0, BlackOut.mc.player.getZ());
                    }

                    BlackOut.mc.player.fallDistance = 0.0F;
                    Managers.NOTIFICATIONS.addNotification("Attempted to save you from the void!", this.getDisplayName(), 2.0, NotificationsSettings.Type.Info);
                }
            }
        }
    }

    public boolean aboveVoid() {
        for (int i = 1; i < 30.0 - Math.ceil(this.prevOG) + BlackOut.mc.player.getBlockY(); i++) {
            if (BlockUtils.collidable(BlackOut.mc.player.blockPosition().below(i))) {
                return false;
            }
        }

        return true;
    }

    public enum Mode {
        Motion,
        Freeze,
        Position
    }
}
