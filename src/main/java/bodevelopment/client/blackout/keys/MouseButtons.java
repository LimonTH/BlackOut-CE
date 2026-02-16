package bodevelopment.client.blackout.keys;

import java.util.HashMap;
import java.util.Map;

public class MouseButtons {
    public static final boolean[] state = new boolean[50];
    private static final Map<Integer, String> names = new HashMap<>();

    static {
        names.put(0, "LEFT");
        names.put(1, "RIGHT");
        names.put(2, "MIDDLE");
    }

    public static String getKeyName(int key) {
        return names.computeIfAbsent(key, MouseButtons::getNameFromKey);
    }

    private static String getNameFromKey(int key) {
        return "mouse" + key;
    }

    public static boolean get(int key) {
        return state[key];
    }

    public static void set(int key, boolean s) {
        if (key >= 0) {
            state[key] = s;
        }
    }
}
