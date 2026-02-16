package bodevelopment.client.blackout.randomstuff.timers;

public class StackingRenderList<T> extends RenderList<T> {
    protected StackingRenderList() {
    }

    @Override
    public void add(T value, double time) {
        this.timers.add(new Timer<>(value, time));
    }
}
