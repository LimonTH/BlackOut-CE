package bodevelopment.client.blackout.event;

import bodevelopment.client.blackout.annotations.Profile;
import bodevelopment.client.blackout.util.BOLogger;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventBus {
    public final Map<Class<?>, List<Listener>> listeners = new ConcurrentHashMap<>();

    /**
     * Subscribes all listener methods found in the provided object.
     * <p>
     * This method scans the object's class for methods annotated as listeners,
     * identifies the event type by the first parameter of the method,
     * and inserts them into the listener registry based on their priority.
     *
     * @param object The instance containing listener methods to be registered.
     * @param skip   The filter logic used to skip specific classes or methods.
     */
    public void subscribe(Object object, ISkip skip) {
        for (Listener listener : this.getListeners(new ArrayList<>(), object.getClass(), object, skip)) {
            Class<?> clazz = listener.method.getParameters()[0].getType();
            List<Listener> list = this.listeners.computeIfAbsent(clazz, k -> new ArrayList<>());
            list.add(this.getIndex(list, listener.priority()), listener);
        }
    }

    public void unsubscribe(Object object) {
        this.listeners.values().forEach(list -> list.removeIf(listener -> listener.object.equals(object)));
    }

    private List<Listener> getListeners(List<Listener> list, Class<?> clazz, Object object, ISkip skip) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Event.class)) {
                int priority = method.getAnnotation(Event.class).eventPriority();
                method.setAccessible(true);
                MethodHandle handle;
                try {
                    handle = MethodHandles.lookup().unreflect(method);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                list.add(this.getIndex(list, priority), new Listener(object, method, handle, skip, priority));
            }
        }

        if (clazz.getSuperclass() != null) {
            this.getListeners(list, clazz.getSuperclass(), object, skip);
        }

        return list;
    }

    public static volatile boolean profiling = false;
    public static final ConcurrentHashMap<String, long[]> profileData = new ConcurrentHashMap<>();

    public <T> T post(T object) {
        List<Listener> eventListeners = this.listeners.get(object.getClass());
        if (eventListeners != null) {
            for (Listener l : eventListeners) {
                try {
                    if (!l.skip.shouldSkip()) {
                        if (profiling && l.method.isAnnotationPresent(Profile.class)) {
                            long start = System.nanoTime();
                            l.handle.invoke(l.object, object);
                            long elapsed = System.nanoTime() - start;
                            String key = l.object.getClass().getSimpleName() + "#" + l.method.getName();
                            profileData.compute(key, (k, v) -> {
                                if (v == null) return new long[]{elapsed, 1L};
                                v[0] += elapsed;
                                v[1]++;
                                return v;
                            });
                        } else {
                            l.handle.invoke(l.object, object);
                        }
                    }
                } catch (Throwable e) {
                    BOLogger.error("Error dispatching event " + object.getClass().getSimpleName()
                            + " to listener " + l.object.getClass().getSimpleName()
                            + "#" + l.method.getName(), e);
                }
            }
        }

        return object;
    }

    /** Returns a formatted profile report, or null if profiling is disabled. */
    public static String getProfileReport() {
        if (!profiling || profileData.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("=== Profile Report (avg μs) ===\n");
        profileData.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .forEach(e -> {
                    long[] data = e.getValue();
                    double avgUs = (data[0] / (double) data[1]) / 1000.0;
                    sb.append(String.format("  %-50s %8.1f μs  (%d calls)%n",
                            e.getKey(), avgUs, data[1]));
                });
        return sb.toString();
    }

    /** Resets all accumulated profile data. */
    public static void resetProfileData() {
        profileData.clear();
    }

    private int getIndex(List<Listener> l, int priority) {
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).priority > priority) {
                return i;
            }
        }

        return l.size();
    }

    public record Listener(Object object, Method method, MethodHandle handle, ISkip skip, int priority) {
    }
}
