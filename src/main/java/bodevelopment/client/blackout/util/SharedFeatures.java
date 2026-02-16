package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.module.modules.combat.defensive.Clip;
import bodevelopment.client.blackout.module.modules.combat.offensive.Auto32K;
import bodevelopment.client.blackout.module.modules.misc.FastProjectile;
import bodevelopment.client.blackout.module.modules.misc.Manager;
import bodevelopment.client.blackout.module.modules.misc.Stealer;
import bodevelopment.client.blackout.module.modules.movement.PhaseWalk;

public class SharedFeatures {
    private static Clip clip;
    private static Stealer stealer;
    private static PhaseWalk phaseWalk;
    private static Auto32K auto32K;
    private static FastProjectile fastProjectile;
    private static Manager manager;

    public static void init() {
        clip = Clip.getInstance();
        stealer = Stealer.getInstance();
        phaseWalk = PhaseWalk.getInstance();
        auto32K = Auto32K.getInstance();
        fastProjectile = FastProjectile.getInstance();
        manager = Manager.getInstance();
    }

    public static boolean shouldPauseRotations() {
        if (fastProjectile.enabled && fastProjectile.ticksLeft >= 0) {
            return true;
        } else if (phaseWalk.enabled && phaseWalk.shouldStopRotation()) {
            return true;
        } else if (clip.enabled && clip.noRotateTime > 0 && clip.stopRotation.get()) {
            return true;
        } else {
            return manager.shouldNoRotate() || stealer.shouldNoRotate();
        }
    }

    public static boolean shouldSilentScreen() {
        return stealer.isSilenting() || auto32K.enabled && auto32K.isSilenting();
    }
}
