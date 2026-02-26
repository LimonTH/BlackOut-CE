package bodevelopment.client.blackout.randomstuff.timers;

import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.interfaces.functional.DoublePredicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class TimerMap<E, T> {
    public static final List<TimerMap<?, ?>> updating = new ArrayList<>();
    public final Map<E, Timer<T>> timers = new ConcurrentHashMap<>();

    public TimerMap(boolean autoUpdate) {
        if (autoUpdate) {
            updating.add(this);
        }
    }

    @Event
    public void onRender(RenderEvent.World.Pre event) {
        this.update();
    }

    public void add(E key, T value, double time) {
        this.timers.remove(key);
        this.timers.put(key, new Timer<>(value, time));
    }

    public void update() {
        this.remove((key, value) -> System.currentTimeMillis() > value.endTime);
    }

    public T get(E key) {
        var entry = timers.get(key);
        if (entry == null) {
            return null;
        }
        return entry.value;
    }

    public void clear() {
        this.timers.clear();
    }

    public T removeKey(E key) {
        Timer<T> value = this.timers.remove(key);
        return value == null ? null : value.value;
    }

    public T remove(DoublePredicate<E, Timer<T>> predicate) {
        for (Entry<E, Timer<T>> entry : this.timers.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue())) {
                this.timers.remove(entry.getKey());
                return entry.getValue().value;
            }
        }

        return null;
    }

    public boolean contains(DoublePredicate<E, Timer<T>> predicate) {
        for (Entry<E, Timer<T>> entry : this.timers.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue())) {
                return true;
            }
        }

        return false;
    }

    public boolean containsKey(E key) {
        return this.timers.containsKey(key);
    }

    public boolean containsValue(T value) {
        for (Entry<E, Timer<T>> entry : this.timers.entrySet()) {
            if (entry.getValue().value == value) {
                return true;
            }
        }

        return false;
    }

    public static class Timer<T> {
        public final T value;
        public final long endTime;
        public final double time;

        public Timer(T value, double time) {
            this.value = value;
            this.endTime = System.currentTimeMillis() + Math.round(time * 1000.0);
            this.time = time;
        }
    }
}
