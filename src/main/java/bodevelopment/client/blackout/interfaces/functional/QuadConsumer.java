package bodevelopment.client.blackout.interfaces.functional;

@FunctionalInterface
public interface QuadConsumer<T1, T2, T3, T4> {
    void accept(T1 x, T2 y, T3 startAngle, T4 endAngle);
}
