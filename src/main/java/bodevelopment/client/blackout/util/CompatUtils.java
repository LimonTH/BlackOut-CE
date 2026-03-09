package bodevelopment.client.blackout.util;

public class CompatUtils {

    public static boolean isBaritonePathing() {
        if (!isBaritonePresent()) {
            return false;
        }
        return BaritoneLazyLoader.isPathing();
    }

    private static boolean isBaritonePresent() {
        try {
            Class.forName("baritone.api.BaritoneAPI", false, CompatUtils.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static class BaritoneLazyLoader {
        private static final baritone.api.IBaritone BARITONE_INSTANCE;

        static {
            baritone.api.IBaritone inst = null;
            try {
                inst = baritone.api.BaritoneAPI.getProvider().getPrimaryBaritone();
            } catch (Throwable ignored) {}
            BARITONE_INSTANCE = inst;
        }

        private static boolean isPathing() {
            if (BARITONE_INSTANCE == null) return false;
            try {
                return BARITONE_INSTANCE.getPathingBehavior().isPathing();
            } catch (Throwable ignored) {
                return false;
            }
        }
    }
}