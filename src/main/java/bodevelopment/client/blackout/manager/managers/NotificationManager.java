package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.util.render.Render2DUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationManager extends Manager {
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
            this.stack.pushPose();
            Render2DUtils.unGuiScale(this.stack);
            synchronized (this.notifications) {
                this.notifications.removeIf(notification -> {
                    if (System.currentTimeMillis() > notification.startTime + notification.time) {
                        return true;
                    } else {
                        this.y = this.y + Notifications.getInstance().render(this.stack, notification, this.y);
                        return false;
                    }
                });
            }

            this.stack.popPose();
        }
    }

    public void addNotification(String text, String bigText, double time, Notifications.Type type) {
        if (Notifications.getInstance().hudNotifications.get()) {
            this.notifications.addFirst(new Notification(text, bigText, time, type));
        }
    }

    public static class Notification {
        public final String text;
        public final String bigText;
        public final Notifications.Type type;
        public final long startTime;
        public final long time;

        public Notification(String text, String bigText, double time, Notifications.Type type) {
            this.text = text;
            this.bigText = bigText;
            this.type = type;
            this.startTime = System.currentTimeMillis();
            this.time = Math.round(time * 1000.0);
        }
    }
}
