package bodevelopment.client.blackout.util;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;

public class CompatUtils {
    private static IBaritone baritone;

    static {
        try {
            baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        } catch (Throwable ignored) {}
    }

    public static boolean isBaritonePathing() {
        try {
            return baritone.getPathingBehavior().isPathing();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
