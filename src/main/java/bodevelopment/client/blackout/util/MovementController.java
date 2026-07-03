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

import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;

/**
 * Centralized movement utility to eliminate code duplication across
 * Flight, Speed, LongJump, PacketFly, Strafe, and other movement modules.
 * <p>
 * Replaces the repeated pattern:
 * <pre>{@code
 *   event.setXZ(this, MovementUtils.xMovement(speed, yaw), MovementUtils.zMovement(speed, yaw));
 * }</pre>
 * with a single call to {@link #applyHorizontalMotion}.
 */
public final class MovementController {
    /**
     * Vanilla anti-kick offset: the minimal downward velocity to satisfy the server's movement check.
     */
    public static final double ANTI_KICK_OFFSET = -0.0315;
    /**
     * Vanilla walking speed constant (blocks/tick).
     */
    public static final double VANILLA_WALK_SPEED = 0.2873;

    private MovementController() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Applies horizontal motion using the module's movement yaw and the given speed.
     * Standard pattern used by Flight, Speed, Strafe, Jesus, etc.
     *
     * @param event  the move event to modify
     * @param module the calling module (for event attribution)
     * @param speed  horizontal speed in blocks/tick
     */
    public static void applyHorizontalMotion(MoveEvent.Pre event, Module module, double speed) {
        double yaw = Managers.ROTATION.moveYaw;
        event.setXZ(module, MovementUtils.xMovement(speed, yaw), MovementUtils.zMovement(speed, yaw));
    }

    /**
     * Applies horizontal motion with a custom yaw angle.
     */
    public static void applyHorizontalMotion(MoveEvent.Pre event, Module module, double speed, double yaw) {
        event.setXZ(module, MovementUtils.xMovement(speed, yaw), MovementUtils.zMovement(speed, yaw));
    }

    /**
     * Stops all horizontal motion.
     */
    public static void stopHorizontalMotion(MoveEvent.Pre event, Module module) {
        event.setXZ(module, 0.0, 0.0);
    }

    /**
     * Applies the anti-kick pattern: if {@code tickCounter > delay}, returns
     * {@link #ANTI_KICK_OFFSET} and resets the counter. Otherwise returns {@code currentY}.
     * <p>
     * Usage:
     * <pre>{@code
     *   double y = applyAntiKick(i, delay.get(), currentY);
     *   if (y != currentY) i = 0;
     *   event.setY(module, y);
     * }</pre>
     *
     * @param tickCounter mutable tick counter (caller must reset to 0 when kick applied)
     * @param delay       ticks between anti-kick applications
     * @param currentY    the current Y velocity to compare against
     * @return {@code Math.min(currentY, ANTI_KICK_OFFSET)} if kick needed, else {@code currentY}
     */
    public static double applyAntiKick(int tickCounter, int delay, double currentY) {
        if (tickCounter > delay) {
            return Math.min(currentY, ANTI_KICK_OFFSET);
        }
        return currentY;
    }

    /**
     * Returns the standard vanilla walking speed for ground-based movement.
     * Used when anti-cheat bypasses revert to normal movement.
     */
    public static double getVanillaWalkSpeed() {
        return VANILLA_WALK_SPEED;
    }
}
