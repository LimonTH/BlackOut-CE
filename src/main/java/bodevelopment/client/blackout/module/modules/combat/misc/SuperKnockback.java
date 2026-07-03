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

package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.mixin.accessors.AccessorServerboundInteractPacket;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.LivingEntity;

public class SuperKnockback extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Boolean> check = this.sgGeneral.booleanSetting("Movement Check", true, "Only triggers packets if you have active velocity to prevent unnatural server-side behavior.");

    public SuperKnockback() {
        super("Super Knockback", "Manipulates sprinting packets to maximize knockback on every hit.", SubCategory.MISC_COMBAT, true);
    }

    @Event
    public void onSend(PacketEvent.Send event) {
        if (BlackOut.mc.player != null) {
            if (!this.check.get() || BlackOut.mc.player.getDeltaMovement().x() != 0.0 && BlackOut.mc.player.getDeltaMovement().z() != 0.0) {
                if (event.packet instanceof AccessorServerboundInteractPacket packet
                        && packet.getType().getType() == ServerboundInteractPacket.ActionType.ATTACK
                        && BlackOut.mc.level.getEntity(packet.getId()) instanceof LivingEntity) {
                    if (!BlackOut.mc.player.isSprinting()) {
                        this.start();
                    }

                    this.stop();
                    this.start();
                }
            }
        }
    }

    private void stop() {
        this.sendPacket(new ServerboundPlayerCommandPacket(BlackOut.mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
    }

    private void start() {
        this.sendPacket(new ServerboundPlayerCommandPacket(BlackOut.mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
    }
}
