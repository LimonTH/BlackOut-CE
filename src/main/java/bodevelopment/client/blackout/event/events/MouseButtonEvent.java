package bodevelopment.client.blackout.event.events;

import bodevelopment.client.blackout.event.Cancellable;

public class MouseButtonEvent extends Cancellable {
    private static final MouseButtonEvent INSTANCE = new MouseButtonEvent(0, false);
    public int button;
    public boolean pressed;

    public MouseButtonEvent(int button, boolean pressed) {
        this.button = button;
        this.pressed = pressed;
    }

    public static MouseButtonEvent get(int button, boolean pressed) {
        INSTANCE.button = button;
        INSTANCE.pressed = pressed;
        return INSTANCE;
    }
}