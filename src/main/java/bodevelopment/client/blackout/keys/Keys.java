package bodevelopment.client.blackout.keys;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.events.KeyEvent;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class Keys {
    public static final boolean[] state = new boolean[1000];
    private static final Map<Integer, String> names = new HashMap<>();

    static {
        names.put(261, "DEL");
        names.put(341, "CTRL");
        names.put(345, "RCTRL");
        names.put(256, "ESC");
        names.put(265, "ARROW UP");
        names.put(264, "ARROW DOWN");
        names.put(263, "ARROW LEFT");
        names.put(262, "ARROW RIGHT");
        names.put(340, "SHIFT");
        names.put(344, "RSHIFT");
    }

    public static String getKeyName(int key) {
        return names.computeIfAbsent(key, Keys::getNameFromKey);
    }

    private static String getNameFromKey(int key) {
        String str = GLFW.glfwGetKeyName(key, 1);
        return str == null ? "" : str.toUpperCase();
    }

    public static boolean get(int key) {
        return state[key];
    }

    public static void set(int key, boolean s) {
        if (key >= 0) {
            if (state[key] != s) {
                BlackOut.EVENT_BUS.post(KeyEvent.get(key, s, state[key]));
            }

            state[key] = s;
        }
    }
}
