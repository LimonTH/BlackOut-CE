package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.interfaces.mixin.IChatHud;
import net.minecraft.network.chat.Component;

public class ChatUtils {
    public static void addMessage(Object object) {
        addMessage(object.toString());
    }

    public static void addMessage(String text, Object... objects) {
        addMessage(String.format(text, objects));
    }

    public static void addMessage(String text) {
        addMessage(Component.nullToEmpty(text));
    }

    public static void addMessage(String text, int id) {
        addMessage(Component.nullToEmpty(text), id);
    }

    public static void addMessage(Component text) {
        ((IChatHud) BlackOut.mc.gui.getChat()).blackout_Client$addMessageToChat(text, -1);
    }

    public static void addMessage(Component text, int id) {
        ((IChatHud) BlackOut.mc.gui.getChat()).blackout_Client$addMessageToChat(text, id);
    }

    public static void sendMessage(String text) {
        if (text.startsWith("/")) {
            BlackOut.mc.getConnection().sendCommand(text.substring(1));
        } else {
            BlackOut.mc.getConnection().sendChat(text);
        }
    }
}
