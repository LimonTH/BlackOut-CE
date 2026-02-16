package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BOLogger {
    private static final Logger LOGGER = LogManager.getLogger(BlackOut.NAME);
    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void info(Text text) {
        LOGGER.info(text.getString());
    }

    public static void warn(String message) {
        LOGGER.warn(message);
    }

    public static void warn(Text text) {
        LOGGER.warn(text);
    }

    public static void error(String message) {
        LOGGER.error(message);
    }

    public static void error(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
    }

    public static void error(Throwable throwable) {
        LOGGER.error(throwable);
    }

    public static void debug(String message) {
        LOGGER.debug(message);
    }
}