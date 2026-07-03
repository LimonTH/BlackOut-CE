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

import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.util.Mth;

import java.util.concurrent.ThreadLocalRandom;

public class PingSpoof extends Module {
    private static PingSpoof INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<SpoofMode> mode = this.sgGeneral.enumSetting("Latency Mode", SpoofMode.Fake, "Fake: Only modifies the ping value displayed in the tab list. Real: Delays packets to physically increase latency.");
    private final Setting<Integer> jitterInterval = this.sgGeneral.intSetting("Refresh Rate", 5, 0, 20, 1, "The interval in ticks at which the jitter value is recalculated for Real mode.", () -> this.mode.get() == SpoofMode.Real);
    private final Setting<Integer> extra = this.sgGeneral.intSetting("Additional Ping", 50, 0, 1000, 10, "The base amount of extra latency added to your connection in milliseconds.");
    private final Setting<Integer> jitter = this.sgGeneral.intSetting("Jitter Magnitude", 5, 0, 1000, 10, "The maximum random variance added to the extra ping to simulate a natural connection.");

    private static final int MAX_SAFE_DELAY_MS = 15000;
    private boolean warnedCap = false;

    private int ji = 0;
    private long nextJ = 0L;

    public PingSpoof() {
        super("Ping Spoof", "Artificially inflates your latency to the server for visual effect or specific bypasses.", SubCategory.MISC, true);
        INSTANCE = this;
    }

    public static PingSpoof getInstance() {
        return INSTANCE;
    }

    @Override
    public void onDisable() {
        Managers.PING.clear();
    }

    @Override
    public String getInfo() {
        return this.mode.get().name() + " " + this.extra.get() + " " + this.ji;
    }

    public void refresh() {
        if (System.currentTimeMillis() > this.nextJ) {
            this.ji = (int) Math.round(ThreadLocalRandom.current().nextDouble() * this.jitter.get());
            this.nextJ = System.currentTimeMillis()
                    + Math.round(Mth.lerp(ThreadLocalRandom.current().nextDouble(), this.jitterInterval.get() / 2.0F, this.jitterInterval.get() * 1.5) * 50.0);
        }
    }

    public boolean shouldDelay(Packet<?> packet) {
        return packet instanceof ServerboundPongPacket || packet instanceof ServerboundKeepAlivePacket;
    }

    public int getPing() {
        int raw = this.mode.get() == SpoofMode.Fake ? this.extra.get() : this.extra.get() + this.ji;
        if (raw > MAX_SAFE_DELAY_MS) {
            if (!warnedCap) {
                Managers.NOTIFICATIONS.addNotification("PingSpoof",
                        "Delay capped at " + MAX_SAFE_DELAY_MS / 1000 + "s to prevent timeout disconnects",
                        5.0, bodevelopment.client.blackout.module.modules.client.NotificationsSettings.Type.Alert);
                warnedCap = true;
            }
            return MAX_SAFE_DELAY_MS;
        }
        warnedCap = false;
        return raw;
    }

    public enum SpoofMode {
        Fake,
        Real
    }
}
