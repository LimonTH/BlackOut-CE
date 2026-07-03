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

package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.Internal;
import com.google.common.reflect.ClassPath;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

@Internal
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
                    BOLogger.error("Class not found in " + packageName + " : ", e);
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
