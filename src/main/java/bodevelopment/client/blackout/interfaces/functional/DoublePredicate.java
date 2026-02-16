package bodevelopment.client.blackout.interfaces.functional;

@FunctionalInterface
public interface DoublePredicate<T, E> {
    boolean test(T key, E value);
}
