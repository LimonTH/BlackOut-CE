package bodevelopment.client.blackout.randomstuff.timers;

import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TickTimerList<T> {
    public static final List<TickTimerList<?>> updating = new ArrayList<>();
    public final List<TickTimer<T>> timers = new ArrayList<>();

    public TickTimerList(boolean autoUpdate) {
        if (autoUpdate) {
            updating.add(this);
        }
    }

    @Event
    public void onRender(RenderEvent.World.Pre event) {
        this.update();
    }

    public void add(T value, int ticks) {
        this.timers.add(new TickTimer<>(value, ticks));
    }

    public void forEach(Consumer<? super T> consumer) {
        this.timers.forEach(timer -> consumer.accept(timer.value));
    }

    public void update() {
        this.timers.removeIf(item -> item.ticks-- <= 0);
    }

    public void clear() {
        this.timers.clear();
    }

    public Map<T, Integer> getMap() {
        Map<T, Integer> map = new HashMap<>();

        for (TickTimer<T> timer : this.timers) {
            map.put(timer.value, timer.ticks);
        }

        return map;
    }

    public List<T> getList() {
        List<T> l = new ArrayList<>();

        for (TickTimer<T> timer : this.timers) {
            l.add(timer.value);
        }

        return l;
    }

    public int getTicksLeft(T object) {
        for (TickTimer<T> timer : this.timers) {
            if (timer.value.equals(object)) {
                return timer.ticks;
            }
        }

        return -1;
    }

    public void remove(TickTimer<T> timer) {
        this.timers.remove(timer);
    }

    public T remove(Predicate<? super TickTimer<T>> predicate) {
        for (TickTimer<T> timer : this.timers) {
            if (predicate.test(timer)) {
                this.timers.remove(timer);
                return timer.value;
            }
        }

        return null;
    }

    public TickTimer<T> get(Predicate<TickTimer<T>> predicate) {
        for (TickTimer<T> timer : this.timers) {
            if (predicate.test(timer)) {
                return timer;
            }
        }

        return null;
    }

    public boolean contains(Predicate<TickTimer<T>> predicate) {
        for (TickTimer<T> timer : this.timers) {
            if (predicate.test(timer)) {
                return true;
            }
        }

        return false;
    }

    public boolean contains(T value) {
        return this.contains(timer -> timer.value.equals(value));
    }

    public static class TickTimer<T> {
        public final T value;
        public int ticks;

        public TickTimer(T value, int ticks) {
            this.value = value;
            this.ticks = ticks;
        }
    }
}
