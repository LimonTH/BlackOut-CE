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
import bodevelopment.client.blackout.annotations.PublicAPI;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.module.modules.client.NotificationsSettings;
import bodevelopment.client.blackout.util.ScreenUtils;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@PublicAPI
public class NotificationManager extends Manager {
    /**
     * Merge duplicate notifications within this window instead of dropping them.
     */
    private static final long DEDUP_WINDOW_MS = 500L;
    private final List<Notification> notifications = Collections.synchronizedList(new ArrayList<>());
    private final PoseStack stack = new PoseStack();
    private float y = 0.0F;

    @Override
    public void init() {
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
    }

    @Event
    public void onRender2D(RenderEvent.Hud.Pre event) {
        if (BlackOut.mc.screen == null && BlackOut.mc.level != null && BlackOut.mc.player != null) {
            this.y = 100.0F;
            ScreenUtils.beginPixelSpace(this.stack);
            synchronized (this.notifications) {
                this.notifications.removeIf(notification -> {
                    if (System.currentTimeMillis() > notification.startTime + notification.time) {
                        return true;
                    } else {
                        this.y = this.y + NotificationsSettings.getInstance().render(this.stack, notification, this.y);
                        return false;
                    }
                });
            }

            ScreenUtils.endPixelSpace(this.stack);
        }
    }

    public void addNotification(String text, String bigText, double time, NotificationsSettings.Type type) {
        if (!NotificationsSettings.getInstance().hudNotifications.get()) return;

        long now = System.currentTimeMillis();
        String dedupKey = bigText + "|" + text;

        synchronized (this.notifications) {
            // Merge with existing identical notification still on screen
            for (Notification n : this.notifications) {
                if (dedupKey.equals(n.dedupKey) && (now - n.startTime) < DEDUP_WINDOW_MS) {
                    n.counter++;
                    n.startTime = now; // reset timer so merged notification stays visible
                    return;
                }
            }
        }

        this.notifications.addFirst(new Notification(text, bigText, time, type, dedupKey));
    }

    public static class Notification {
        public final String text;
        public final String bigText;
        public final NotificationsSettings.Type type;
        public final long time;
        public long startTime;
        public int counter = 1;
        final String dedupKey;

        Notification(String text, String bigText, double time, NotificationsSettings.Type type, String dedupKey) {
            this.text = text;
            this.bigText = bigText;
            this.type = type;
            this.startTime = System.currentTimeMillis();
            this.time = Math.round(time * 1000.0);
            this.dedupKey = dedupKey;
        }
    }
}
