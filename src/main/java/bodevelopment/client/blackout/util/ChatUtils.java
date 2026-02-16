package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.interfaces.mixin.IChatHud;
import net.minecraft.text.Text;

public class ChatUtils {
    public static void addMessage(Object object) {
        addMessage(object.toString());
    }

    public static void addMessage(String text, Object... objects) {
        addMessage(String.format(text, objects));
    }

    public static void addMessage(String text) {
        addMessage(Text.of(text));
    }

    public static void addMessage(String text, int id) {
        addMessage(Text.of(text), id);
    }

    public static void addMessage(Text text) {
        ((IChatHud) BlackOut.mc.inGameHud.getChatHud()).blackout_Client$addMessageToChat(text, -1);
    }

    public static void addMessage(Text text, int id) {
        ((IChatHud) BlackOut.mc.inGameHud.getChatHud()).blackout_Client$addMessageToChat(text, id);
    }

    public static void sendMessage(String text) {
        if (text.startsWith("/")) {
            BlackOut.mc.getNetworkHandler().sendChatCommand(text.substring(1));
        } else {
            BlackOut.mc.getNetworkHandler().sendChatMessage(text);
        }
    }
}
