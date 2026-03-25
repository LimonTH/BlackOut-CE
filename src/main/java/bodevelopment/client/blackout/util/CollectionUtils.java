package bodevelopment.client.blackout.util;

import java.util.ArrayList;
import java.util.List;

public class CollectionUtils {
    public static <T> void limitSize(List<T> list, int maxSize) {
        if (list.size() > maxSize) {
            list.subList(maxSize, list.size()).clear();
        }
    }

    public static <T> List<T> reversed(List<T> list) {
        List<T> result = new ArrayList<>();
        list.forEach(result::addFirst);
        return result;
    }

    public static <T> boolean contains(T[] array, T object) {
        for (T t : array) {
            if (t.equals(object)) {
                return true;
            }
        }
        return false;
    }
}
