package bodevelopment.client.blackout.event.events;

/**
 * Fired when the player changes dimensions (Nether ↔ Overworld, etc.).
 * Modules that cache position-dependent state should reset their data when
 * this event is received.
 *
 * <p>Usage: {@code BlackOut.EVENT_BUS.post(DimensionChangeEvent.get())}
 */
public class DimensionChangeEvent {
    private static final DimensionChangeEvent INSTANCE = new DimensionChangeEvent();

    public static DimensionChangeEvent get() {
        return INSTANCE;
    }
}
