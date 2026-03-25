package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Common null-safe accessors for the local player and level.
 * Replaces scattered {@code !PlayerUtils.isInGame()} guards.
 */
public class PlayerUtils {

    /**
     * Returns true if the player and level are both loaded (i.e., we are in a world).
     */
    public static boolean isInGame() {
        return BlackOut.mc.player != null && BlackOut.mc.level != null;
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
