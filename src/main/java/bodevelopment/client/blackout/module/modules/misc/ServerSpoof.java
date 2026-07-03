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
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.PlayerUtils;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;

import java.util.UUID;

public class ServerSpoof extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Double> delay = this.sgGeneral.doubleSetting("Packet Delay", 2.0, 0.0, 10.0, 0.1, "The interval in seconds between each sequential spoofed status packet (Accepted, Downloaded, Loaded).");

    private final ServerboundResourcePackPacket.Action[] statuses = new ServerboundResourcePackPacket.Action[]{ServerboundResourcePackPacket.Action.ACCEPTED, ServerboundResourcePackPacket.Action.DOWNLOADED, ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED};
    private UUID id;
    private long time = -1L;
    private int progress = 0;

    public ServerSpoof() {
        super("Server Spoof", "Automatically spoofs resource pack download and installation packets to bypass mandatory server pack requirements without downloading them.", SubCategory.MISC, true);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (PlayerUtils.isInGame() && BlackOut.mc.player.tickCount >= 20 && this.time >= 0L) {
            if (System.currentTimeMillis() > this.time + this.delay.get() * 1000.0) {
                this.sendPacket(new ServerboundResourcePackPacket(this.id, this.statuses[this.progress]));
                if (this.progress > 1) {
                    this.time = -1L;
                } else {
                    this.time = System.currentTimeMillis();
                }

                this.progress++;
            }
        }
    }

    @Event
    public void onReceive(PacketEvent.Receive.Post event) {
        if (event.packet instanceof ClientboundResourcePackPushPacket packet) {
            event.setCancelled(true);
            this.id = packet.id();
            this.time = System.currentTimeMillis();
            this.progress = 0;
        }
    }
}
