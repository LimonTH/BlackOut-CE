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

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.PublicAPI;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Common null-safe accessors for the local player and level.
 * Replaces scattered {@code !PlayerUtils.isInGame()} guards.
 */
@PublicAPI
public class PlayerUtils {

    /**
     * Returns true if the player and level are both loaded (i.e., we are in a world).
     */
    public static boolean isInGame() {
        return BlackOut.mc.player != null && BlackOut.mc.level != null && !BlackOut.mc.isPaused();
    }

    /**
     * Returns the local player, or null if not in game.
     */
    public static LocalPlayer player() {
        return BlackOut.mc.player;
    }

    /**
     * Returns the player's eye position. Caller must ensure {@link #isInGame()}.
     */
    public static Vec3 eyePos() {
        return BlackOut.mc.player.getEyePosition();
    }

    /**
     * Returns the player's foot position. Caller must ensure {@link #isInGame()}.
     */
    public static Vec3 pos() {
        return BlackOut.mc.player.position();
    }

    /**
     * Returns the item in the player's main hand. Caller must ensure {@link #isInGame()}.
     */
    public static ItemStack mainHandItem() {
        return BlackOut.mc.player.getMainHandItem();
    }

    /**
     * Returns the item in the player's off hand. Caller must ensure {@link #isInGame()}.
     */
    public static ItemStack offHandItem() {
        return BlackOut.mc.player.getOffhandItem();
    }

    /**
     * Returns the player's inventory. Caller must ensure {@link #isInGame()}.
     */
    public static Inventory inventory() {
        return BlackOut.mc.player.getInventory();
    }

    /**
     * Returns the network connection, or null if not connected.
     */
    public static ClientPacketListener connection() {
        return BlackOut.mc.getConnection();
    }
}
