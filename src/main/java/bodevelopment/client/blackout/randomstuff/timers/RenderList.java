package bodevelopment.client.blackout.randomstuff.timers;

import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class RenderList<T> {
    protected final List<Timer<T>> timers = Collections.synchronizedList(new ArrayList<>());

    protected RenderList() {
    }

    public static <E> RenderList<E> getList(boolean stacking) {
        return stacking ? new StackingRenderList<>() : new RenderList<>();

    }

    public void add(T value, double time) {
        this.timers.removeIf(timer -> timer.value.equals(value));
        this.timers.add(new Timer<>(value, time));
    }

    public void update(RenderConsumer<T> consumer) {
        synchronized (this.timers) {
            long now = System.currentTimeMillis();
            this.timers.removeIf(item -> {
                if (now >= item.endTime) {
                    return true;
                } else {
                    long duration = item.endTime - item.startTime;

                    double progress = (duration <= 0) ? 1.0 :
                            MathHelper.clamp((double) (now - item.startTime) / duration, 0.0, 1.0);

                    consumer.accept(
                            item.value,
                            (now - item.startTime) / 1000.0,
                            progress
                    );
                    return false;
                }
            });
        }
    }

    public void remove(T t) {
        synchronized (this.timers) {
            this.timers.removeIf(item -> item.value.equals(t));
        }
    }

    public void remove(Predicate<Timer<T>> predicate) {
        synchronized (this.timers) {
            this.timers.removeIf(predicate);
        }
    }

    public boolean contains(Predicate<Timer<T>> predicate) {
        synchronized (this.timers) {
            for (Timer<T> timer : this.timers) {
                if (predicate.test(timer)) {
                    return true;
                }
            }

            return false;
        }
    }

    public void clear() {
        this.timers.clear();
    }

    @FunctionalInterface
    public interface RenderConsumer<E> {
        void accept(E element, double time, double progress);
    }

    public static class Timer<T> {
        public final T value;
        public final long startTime;
        public final long endTime;

        public Timer(T value, double time) {
            this.value = value;
            this.startTime = System.currentTimeMillis();
            this.endTime = System.currentTimeMillis() + Math.round(time * 1000.0);
        }
    }
}
