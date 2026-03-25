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
        names.put(280, "CAPS");
        names.put(265, "ARROW UP");
        names.put(264, "ARROW DOWN");
        names.put(263, "ARROW LEFT");
        names.put(262, "ARROW RIGHT");
        names.put(340, "SHIFT");
        names.put(344, "RSHIFT");
        names.put(257, "ENTER");
        names.put(342, "LALT");
        names.put(346, "RALT");
        names.put(348, "MENU");
        names.put(258, "TAB");
        names.put(32,  "SPACE");
        names.put(259, "BACK");
        names.put(260, "INS");
        names.put(268, "HOME");
        names.put(269, "END");
        names.put(266, "PGUP");
        names.put(267, "PGDN");
        names.put(283, "PRTSC");
        names.put(284, "PAUSE");
        names.put(281, "SCRLK");
        names.put(320, "NUM 0");
        names.put(321, "NUM 1");
        names.put(322, "NUM 2");
        names.put(323, "NUM 3");
        names.put(324, "NUM 4");
        names.put(325, "NUM 5");
        names.put(326, "NUM 6");
        names.put(327, "NUM 7");
        names.put(328, "NUM 8");
        names.put(329, "NUM 9");
        names.put(330, "NUM .");
        names.put(331, "NUM /");
        names.put(332, "NUM *");
        names.put(333, "NUM -");
        names.put(334, "NUM +");
        names.put(335, "NUM ENT");
        names.put(336, "NUM =");
        names.put(282, "NUM LCK");
        names.put(343, "LWIN");
        names.put(347, "RWIN");

        for (int i = 0; i <= 24; i++) {
            names.put(290 + i, "F" + (i + 1));
        }
    }

    public static String getKeyName(int key) {
        return names.computeIfAbsent(key, Keys::getNameFromKey);
    }

    private static String getNameFromKey(int key) {
        String str = GLFW.glfwGetKeyName(key, 1);
        return str == null ? "" : str.toUpperCase();
    }

    public static boolean get(int key) {
        if (key < 0 || key >= state.length) return false;
        return state[key];
    }

    public static void set(int key, boolean s) {
        if (key >= 0 && key < state.length) {
            if (state[key] != s) {
                BlackOut.EVENT_BUS.post(KeyEvent.get(key, s, state[key]));
            }

            state[key] = s;
        }
    }
}
