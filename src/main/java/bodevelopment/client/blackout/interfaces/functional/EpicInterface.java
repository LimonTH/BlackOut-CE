package bodevelopment.client.blackout.interfaces.functional;

@FunctionalInterface
public interface EpicInterface<T, E> {
    E get(T item);
}
