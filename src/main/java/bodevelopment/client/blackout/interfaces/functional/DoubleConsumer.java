package bodevelopment.client.blackout.interfaces.functional;

@FunctionalInterface
public interface DoubleConsumer<T, E> {
    void accept(T target, E value);
}
