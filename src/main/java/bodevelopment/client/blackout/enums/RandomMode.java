package bodevelopment.client.blackout.enums;

import bodevelopment.client.blackout.interfaces.functional.SingleOut;

import java.util.concurrent.ThreadLocalRandom;

public enum RandomMode {
    Sin(() -> (Math.sin(System.currentTimeMillis() / 500.0) + 1.0) / 2.0),
    Random(ThreadLocalRandom.current()::nextDouble),
    Disabled(() -> 0.5);

    private final SingleOut<Double> random;

    RandomMode(SingleOut<Double> random) {
        this.random = random;
    }

    public double get() {
        return this.random.get();
    }
}
