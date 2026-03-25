package bodevelopment.client.blackout.manager;

/**
 * Interface for managers that persist state to disk.
 * Implementors use deferred save: {@link #save()} marks dirty,
 * actual I/O happens on the next tick cycle when the cooldown expires.
 */
public interface Persistable {
    void save();
}
