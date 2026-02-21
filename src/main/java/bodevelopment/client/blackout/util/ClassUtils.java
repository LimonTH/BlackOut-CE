package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import com.google.common.reflect.ClassPath;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

public class ClassUtils {
    private static Constructor<?> constructor = null;

    public static void init() {
        for (Constructor<?> ctr : BlackOut.class.getDeclaredConstructors()) {
            constructor = ctr;
            if (ctr.getGenericParameterTypes().length == 0) {
                break;
            }
        }

        constructor.setAccessible(true);
    }

    public static void forEachClass(Consumer<? super Class<?>> consumer, String packageName, ClassLoader loader) {
        try {
            ClassLoader cl = loader != null ? loader : BlackOut.class.getClassLoader();

            ClassPath.from(cl).getTopLevelClassesRecursive(packageName).forEach(info -> {
                try {
                    consumer.accept(Class.forName(info.getName(), true, cl));
                } catch (ClassNotFoundException e) {
                    BOLogger.error("Класс не найден в " + packageName + " : ", e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T instance(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
