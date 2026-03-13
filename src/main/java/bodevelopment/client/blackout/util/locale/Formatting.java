package bodevelopment.client.blackout.util.locale;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class Formatting {
    public static MutableComponent translate(String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static String string(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }
}