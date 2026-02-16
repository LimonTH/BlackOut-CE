package bodevelopment.client.blackout.event.events;

public class MouseScrollEvent {
    private static final MouseScrollEvent INSTANCE = new MouseScrollEvent();
    public double horizontal = 0.0;
    public double vertical = 0.0;
    private boolean cancelled = false;

    public static MouseScrollEvent get(double horizontal, double vertical) {
        INSTANCE.horizontal = horizontal;
        INSTANCE.vertical = vertical;
        INSTANCE.cancelled = false;
        return INSTANCE;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }
}