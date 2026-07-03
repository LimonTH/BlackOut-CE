/*
 * Blackout Client (CE) - A cutting-edge, feature-rich cheat client for Minecraft.
 * A modernized continuation of the original Blackout project by OLEPOSSU & KassuK.
 * Copyright (C) 2026  LimonTH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

package bodevelopment.client.blackout.randomstuff.timers;

import bodevelopment.client.blackout.annotations.ThreadSafe;

import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

@ThreadSafe
public class TimerList<T> {
    public static final List<TimerList<?>> updating = new ArrayList<>();
    private final List<Timer<T>> timers = Collections.synchronizedList(new ArrayList<>());

    public TimerList(boolean autoUpdate) {
        if (autoUpdate) {
            updating.add(this);
        }
    }

    @Event
    public void onRender(RenderEvent.World.Pre event) {
        this.update();
    }

    public void add(T value, double time) {
        this.timers.add(new Timer<>(value, time));
    }

    public void addAll(Collection<? extends T> collection, double time) {
        collection.forEach(item -> this.add(item, time));
    }

    public void update() {
        synchronized (this.timers) {
            this.timers.removeIf(item -> System.currentTimeMillis() > item.endTime);
        }
    }

    public void clear() {
        this.timers.clear();
    }

    public void forEach(Consumer<? super Timer<T>> consumer) {
        synchronized (this.timers) {
            this.timers.forEach(consumer);
        }
    }

    public Map<T, Double> getMap() {
        Map<T, Double> map = new HashMap<>();
        synchronized (this.timers) {
            for (Timer<T> timer : this.timers) {
                map.put(timer.value, timer.time);
            }

            return map;
        }
    }

    public long getEndTime(T object) {
        synchronized (this.timers) {
            for (Timer<T> timer : this.timers) {
                if (timer.value.equals(object)) {
                    return timer.endTime;
                }
            }

            return -1L;
        }
    }

    public List<Timer<T>> getTimers() {
        return this.timers;
    }

    public List<T> getList() {
        List<T> l = new ArrayList<>();
        synchronized (this.timers) {
            for (Timer<T> timer : this.timers) {
                l.add(timer.value);
            }

            return l;
        }
    }

    public T remove(Predicate<? super Timer<T>> predicate) {
        synchronized (this.timers) {
            for (Timer<T> timer : this.timers) {
                if (predicate.test(timer)) {
                    this.timers.remove(timer);
                    return timer.value;
                }
            }

            return null;
        }
    }

    public boolean removeAll(Predicate<? super Timer<T>> predicate) {
        synchronized (this.timers) {
            return this.timers.removeIf(predicate);
        }
    }

    public boolean contains(T value) {
        synchronized (this.timers) {
            for (Timer<T> timer : this.timers) {
                if (timer.value.equals(value)) {
                    return true;
                }
            }

            return false;
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

    public void replace(T value, double time) {
        this.remove(timer -> timer.value.equals(value));
        this.add(value, time);
    }

    public static class Timer<T> {
        public final T value;
        public final long startTime;
        public final long endTime;
        public final double time;

        public Timer(T value, double time) {
            this.value = value;
            this.startTime = System.currentTimeMillis();
            this.endTime = System.currentTimeMillis() + Math.round(time * 1000.0);
            this.time = time;
        }
    }
}
