package bodevelopment.client.blackout.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventBus {
    public final Map<Class<?>, List<Listener>> listeners = new ConcurrentHashMap<>();

    public void subscribe(Object object, ISkip skip) {
        for (Listener listener : this.getListeners(new ArrayList<>(), object.getClass(), object, skip)) {
            Class<?> clazz = listener.method.getParameters()[0].getType();
            this.listeners.computeIfAbsent(clazz, k -> new ArrayList<>()).add(listener);
        }
    }

    public void unsubscribe(Object object) {
        this.listeners.values().forEach(list -> list.removeIf(listener -> listener.object.equals(object)));
    }

    private List<Listener> getListeners(List<Listener> list, Class<?> clazz, Object object, ISkip skip) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Event.class)) {
                int priority = method.getAnnotation(Event.class).eventPriority();
                list.add(this.getIndex(list, priority), new Listener(object, method, skip, priority));
            }
        }

        if (clazz.getSuperclass() != null) {
            this.getListeners(list, clazz.getSuperclass(), object, skip);
        }

        return list;
    }

    public <T> T post(T object) {
        List<Listener> eventListeners = this.listeners.get(object.getClass());
        if (eventListeners != null) {
            for (Listener l : eventListeners) {
                try {
                    if (!l.skip.shouldSkip()) {
                        l.method.invoke(l.object, object);
                    }
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return object;
    }

    private int getIndex(List<Listener> l, int priority) {
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).priority > priority) {
                return i;
            }
        }

        return l.size();
    }

    public record Listener(Object object, Method method, ISkip skip, int priority) {
    }
}
