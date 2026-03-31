package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.module.modules.movement.ElytraFly;

public class CompatUtils {
    private static final boolean BARITONE_PRESENT;

    static {
        boolean present;
        try {
            Class.forName("baritone.api.BaritoneAPI", false, CompatUtils.class.getClassLoader());
            present = true;
        } catch (ClassNotFoundException e) {
            present = false;
        }
        BARITONE_PRESENT = present;
    }

    public static boolean isBaritonePathing() {
        return BARITONE_PRESENT && BaritoneLazyLoader.isPathing();
    }

    public static boolean shouldBypassRotations() {
        return isBaritonePathing() && (BlackOut.mc.player == null || !BlackOut.mc.player.isFallFlying() || !ElytraFly.getInstance().enabled);
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
                return BARITONE_INSTANCE.getPathingBehavior().isPathing()
                        || BARITONE_INSTANCE.getPathingControlManager().mostRecentInControl().isPresent();
            } catch (Throwable ignored) {
                return false;
            }
        }
    }
}