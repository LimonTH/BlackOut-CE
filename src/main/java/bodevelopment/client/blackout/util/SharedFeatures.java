/*
 * Blackout Client (CE) - A cutting-edge, feature-rich cheat client for Minecraft.
 * A modernized continuation of the original Blackout project by OLEPOSSU & KassuK.
 * Copyright (C) 2026  LimonTH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

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
