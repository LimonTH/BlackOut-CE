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

package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.NotificationsSettings;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.PlayerUtils;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;

import java.util.ArrayList;
import java.util.List;

public class StaffCheck extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Boolean> kb = this.sgGeneral.booleanSetting("Knockback Alert", true, "Notifies you if you receive purely vertical velocity, which is a common method used by staff to check for anti-knockback cheats.");
    private final Setting<Boolean> nameCheck = this.sgGeneral.booleanSetting("Staff Name Alert", false, "Cross-references joining players with a known list of staff members and administrators.");

    List<String> staff = new ArrayList<>();
    private long prevTime = System.currentTimeMillis();
    private boolean added = false;

    public StaffCheck() {
        super("Staff Check", "Monitors server activity for signs of administrative presence or manual 'vibe checks' like knockback testing.", SubCategory.MISC, true);
    }

    @Override
    public void onEnable() {
        if (!this.added) {
            this.added = true;
            this.staff.add("Limon_TH"); // LimonTH
            this.staff.add("Raksamies"); // KassuK
            this.staff.add("OLEPOSSU"); // luhpossu
        }
    }

    @Event
    public void onVelocity(PacketEvent.Receive.Pre event) {
        if (event.packet instanceof ClientboundSetEntityMotionPacket packet) {
            if (BlackOut.mc.player != null && BlackOut.mc.player.getId() != packet.getId()) {
                return;
            }

            this.checkVelocity((float) packet.getXa(), (float) packet.getZa(), (float) packet.getYa());
        }

        if (event.packet instanceof ClientboundExplodePacket packet) {
            if (packet.playerKnockback().isPresent()) {
                double velX = packet.playerKnockback().get().x;
                double velZ = packet.playerKnockback().get().z;
                double velY = packet.playerKnockback().get().y;
                this.checkVelocity(velX, velZ, velY);
            }
        }
    }

    private void checkVelocity(double x, double z, double y) {
        if (BlackOut.mc.player != null) {
            if (x == 0.0F && z == 0.0F && y > 0.0F && this.kb.get()) {
                Managers.NOTIFICATIONS
                        .addNotification("Suspicious Knockback taken at tick " + BlackOut.mc.player.tickCount, this.getDisplayName(), 2.0, NotificationsSettings.Type.Alert);
            }
        }
    }

    @Event
    public void onSend(PacketEvent.Receive.Pre event) {
        if (PlayerUtils.isInGame()) {
            if (this.nameCheck.get()) {
                if (event.packet instanceof ClientboundPlayerInfoUpdatePacket packet) {
                    List<ClientboundPlayerInfoUpdatePacket.Entry> entries = packet.newEntries();
                    entries.forEach(
                            entry -> {
                                if (entry.displayName() != null
                                        && this.staff.contains(entry.displayName().toFlatList().toString())
                                        && System.currentTimeMillis() - this.prevTime > 5000L) {
                                    Managers.NOTIFICATIONS.addNotification("Detected Staff", this.getDisplayName(), 2.0, NotificationsSettings.Type.Alert);
                                    this.prevTime = System.currentTimeMillis();
                                }
                            }
                    );
                }
            }
        }
    }
}
