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

package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.ThreadSafe;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.module.modules.misc.PingSpoof;
import net.minecraft.network.protocol.Packet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ThreadSafe
public class PingManager extends Manager {
    private final List<DelayedPacket> sending = Collections.synchronizedList(new ArrayList<>());

    public void update() {
        PingSpoof spoof = PingSpoof.getInstance();
        spoof.refresh();
        long ping = spoof.getPing();
        synchronized (this.sending) {
            this.sending.removeIf(d -> {
                if (System.currentTimeMillis() < d.time() + ping) {
                    return false;
                } else {
                    d.runnable().run();
                    return true;
                }
            });
        }
    }

    public boolean shouldDelay(Packet<?> packet) {
        PingSpoof spoof = PingSpoof.getInstance();
        return spoof.enabled && BlackOut.mc.player != null && spoof.shouldDelay(packet);
    }

    public void addSend(Runnable runnable) {
        this.sending.add(new DelayedPacket(runnable, System.currentTimeMillis()));
    }

    public void clear() {
        synchronized (this.sending) {
            this.sending.clear();
        }
    }

    private record DelayedPacket(Runnable runnable, long time) {
    }
}
