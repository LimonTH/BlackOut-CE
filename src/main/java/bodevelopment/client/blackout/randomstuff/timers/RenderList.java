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

import net.minecraft.util.Mth;

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
                            Mth.clamp((double) (now - item.startTime) / duration, 0.0, 1.0);

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
