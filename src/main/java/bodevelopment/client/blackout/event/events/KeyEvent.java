package bodevelopment.client.blackout.event.events;

import bodevelopment.client.blackout.event.Cancellable;

public class KeyEvent extends Cancellable {
    private static final KeyEvent INSTANCE = new KeyEvent();
    public int key = 0;
    public boolean pressed = false;
    public boolean prev = false;

    public static KeyEvent get(int key, boolean pressed, boolean prev) {
        INSTANCE.key = key;
        INSTANCE.pressed = pressed;
        INSTANCE.prev = prev;
        return INSTANCE;
    }
}