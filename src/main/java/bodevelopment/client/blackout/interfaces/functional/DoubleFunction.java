package bodevelopment.client.blackout.interfaces.functional;

@FunctionalInterface
public interface DoubleFunction<T1, T2, T3> {
    T3 apply(T1 t, T2 u);
}
