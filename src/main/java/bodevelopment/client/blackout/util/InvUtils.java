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
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.interfaces.functional.EpicInterface;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.randomstuff.FindResult;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.function.Predicate;

/**
 * Inventory utility class for item searching, counting, and swapping.
 * <p>
 * All swap methods now delegate to {@link SwapProtocol} internally.
 * Per-mode handle tracking replaces the old static field state.
 * <p>
 * <h3>Swap Mode Protocol Reference</h3>
 * <ul>
 *   <li><b>Normal</b>: {@code UpdateSelectedSlotC2SPacket} — persistent slot change, safe.</li>
 *   <li><b>Silent</b>: {@code SetCarriedItem(tool) → Action → SetCarriedItem(orig)} — dual-packet,
 *       server sees tool during action, client sees original.</li>
 *   <li><b>PickSilent</b>: {@code PickItemC2SPacket} — copies item to current hotbar slot.</li>
 *   <li><b>InvSwitch</b>: {@code ClickSlotC2SPacket(SWAP)} — swaps inventory↔hotbar.</li>
 *   <li><b>Disabled</b>: No swap performed.</li>
 * </ul>
 */
@PublicAPI
public class InvUtils {
    private static SwapProtocol.SwapHandle normalHandle;
    private static SwapProtocol.SwapHandle invSwitchHandle;
    private static SwapProtocol.SwapHandle pickSilentHandle;
    private static SwapProtocol.SwapHandle silentHandle;

    public static InteractionHand getHand(Item item) {
        return getHand(stack -> stack.getItem() == item);
    }

    public static InteractionHand getHand(Predicate<ItemStack> predicate) {
        if (predicate.test(Managers.PACKET.getStack())) {
            return InteractionHand.MAIN_HAND;
        } else {
            return predicate.test(BlackOut.mc.player.getOffhandItem()) ? InteractionHand.OFF_HAND : null;
        }
    }

    public static ItemStack getHandItem(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? Managers.PACKET.getStack()
                : (hand == InteractionHand.OFF_HAND ? BlackOut.mc.player.getOffhandItem() : null);
    }

    public static int count(boolean hotbar, boolean inventory, Predicate<ItemStack> predicate) {
        if (BlackOut.mc.player == null) return 0;
        int count = 0;

        for (int i = hotbar ? 0 : 9; i < (inventory ? BlackOut.mc.player.getInventory().getContainerSize() : 9); i++) {
            ItemStack stack = BlackOut.mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && predicate.test(stack)) {
                count += stack.getCount();
            }
        }

        return count;
    }

    public static FindResult find(boolean hotbar, boolean inventory, Item item) {
        return find(hotbar, inventory, stack -> stack.getItem() == item);
    }

    public static FindResult find(boolean hotbar, boolean inventory, Predicate<ItemStack> predicate) {
        if (BlackOut.mc.player != null) {
            for (int i = hotbar ? 0 : 9; i < (inventory ? BlackOut.mc.player.getInventory().getContainerSize() : 9); i++) {
                ItemStack stack = BlackOut.mc.player.getInventory().getItem(i);
                if (!stack.isEmpty() && predicate.test(stack)) {
                    return new FindResult(i, stack.getCount(), stack);
                }
            }
        }

        return new FindResult(-1, 0, null);
    }

    public static FindResult findNullable(boolean hotbar, boolean inventory, Item item) {
        return findNullable(hotbar, inventory, stack -> stack.getItem() == item);
    }

    public static FindResult findNullable(boolean hotbar, boolean inventory, Predicate<ItemStack> predicate) {
        if (BlackOut.mc.player != null) {
            for (int i = hotbar ? 0 : 9; i < (inventory ? BlackOut.mc.player.getInventory().getContainerSize() : 9); i++) {
                ItemStack stack = BlackOut.mc.player.getInventory().getItem(i);
                if (predicate.test(stack)) {
                    return new FindResult(i, stack.getCount(), stack);
                }
            }
        }

        return new FindResult(-1, 0, null);
    }

    public static FindResult findBest(boolean hotbar, boolean inventory, EpicInterface<ItemStack, Double> test) {
        if (BlackOut.mc.player != null) {
            double bestValue = Double.NEGATIVE_INFINITY;
            FindResult best = null;

            for (int i = hotbar ? 0 : 9; i < (inventory ? BlackOut.mc.player.getInventory().getContainerSize() : 9); i++) {
                ItemStack stack = BlackOut.mc.player.getInventory().getItem(i);
                double value = test.get(stack);
                if (best == null || value > bestValue) {
                    bestValue = value;
                    best = new FindResult(i, stack.getCount(), stack);
                }
            }

            if (best != null) {
                return best;
            }
        }

        return new FindResult(-1, 0, null);
    }

    public static int getId(int slot) {
        AbstractContainerMenu screen = BlackOut.mc.player.containerMenu;
        int length = screen.slots.size();
        return slot < 9 ? length + slot - 10 : slot + length - 46;
    }

    public static void clickF(int slot) {
        clickSlot(slot, 40, ClickType.SWAP);
    }

    public static void clickSlot(int slot, int button, ClickType action) {
        AbstractContainerMenu handler = BlackOut.mc.player.containerMenu;
        interactSlot(handler.containerId, getId(slot), button, action);
    }

    static void clickSlotInstantly(int slot, int button, ClickType action) {
        AbstractContainerMenu handler = BlackOut.mc.player.containerMenu;
        interactSlot(handler.containerId, getId(slot), button, action, true);
    }

    public static void interactSlot(int syncId, int slotId, int button, ClickType actionType) {
        interactSlot(syncId, slotId, button, actionType, false);
    }

    public static void interactHandler(int slot, int button, ClickType actionType) {
        interactSlot(BlackOut.mc.player.containerMenu.containerId, slot, button, actionType);
    }

    public static void interactSlot(int syncId, int slotId, int button, ClickType actionType, boolean instant) {
        AbstractContainerMenu screenHandler = BlackOut.mc.player.containerMenu;
        NonNullList<Slot> defaultedList = screenHandler.slots;
        int i = defaultedList.size();
        ArrayList<ItemStack> list = Lists.newArrayListWithCapacity(i);

        for (Slot slot : defaultedList) {
            list.add(slot.getItem().copy());
        }

        screenHandler.clicked(slotId, button, actionType, BlackOut.mc.player);
        Int2ObjectOpenHashMap<ItemStack> int2ObjectMap = new Int2ObjectOpenHashMap<>();

        for (int j = 0; j < i; j++) {
            ItemStack itemStack = list.get(j);
            ItemStack itemStack2 = defaultedList.get(j).getItem();
            if (!ItemStack.matches(itemStack, itemStack2)) {
                int2ObjectMap.put(j, itemStack2.copy());
            }
        }

        if (instant) {
            Managers.PACKET
                    .sendInstantly(
                            new ServerboundContainerClickPacket(syncId, screenHandler.getStateId(), slotId, button, actionType, screenHandler.getCarried().copy(), int2ObjectMap)
                    );
        } else {
            Managers.PACKET
                    .sendPacket(
                            new ServerboundContainerClickPacket(syncId, screenHandler.getStateId(), slotId, button, actionType, screenHandler.getCarried().copy(), int2ObjectMap)
                    );
        }
    }

    /**
     * Normal swap: changes the active slot both client-side and server-side
     * via {@link ServerboundSetCarriedItemPacket}.
     */
    public static boolean swap(int to) {
        SwapProtocol.SwapHandle handle = SwapProtocol.swap(SwitchMode.Normal, to);
        if (handle != null) {
            normalHandle = handle;
            return true;
        }
        return false;
    }

    /**
     * Normal swap (instant/queueless version).
     */
    public static boolean swapInstantly(int to) {
        SwapProtocol.SwapHandle handle = SwapProtocol.swapInstantly(SwitchMode.Normal, to);
        if (handle != null) {
            normalHandle = handle;
            return true;
        }
        return false;
    }

    /**
     * Reverses the last normal swap.
     */
    public static boolean swapBack() {
        if (normalHandle != null && !normalHandle.ended()) {
            normalHandle.end();
            normalHandle = null;
            return true;
        }
        return false;
    }

    /**
     * Reverses the last normal swap (instant version).
     */
    public static boolean swapBackInstantly() {
        if (normalHandle != null && !normalHandle.ended()) {
            normalHandle.endInstantly();
            normalHandle = null;
            return true;
        }
        return false;
    }

    /**
     * Silent swap: sends the tool slot to the server via the proper packet queue,
     * updates client visual to the tool for {@code getStack()} consistency.
     * <p>
     * After sending the action packet (e.g. START_DESTROY_BLOCK), call
     * {@link #swapSilentRestoreVisual()} to fix client-side rendering WITHOUT
     * sending a restore packet (server still sees the tool).
     * <p>
     * When mining completes, call {@link #swapSilentBack()} to restore the
     * original slot on the server side.
     *
     * @param to target hotbar slot (0-8)
     * @return true if the swap was initiated
     */
    public static boolean swapSilent(int to) {
        if (to < 0 || to > 8) return false;
        if (to == Managers.PACKET.slot) return true;

        SwapProtocol.SwapHandle handle = SwapProtocol.swap(SwitchMode.Silent, to);
        if (handle != null) {
            silentHandle = handle;
            return true;
        }
        return false;
    }

    /**
     * Restores only the client-side visual selected slot after a Silent swap.
     * <p>
     * <b>No packet is sent</b> — the server still sees the tool in the active slot.
     * This allows mining progress to continue while the player visually holds
     * their original item.
     * <p>
     * Call this AFTER sending the action packet (e.g., START_DESTROY_BLOCK).
     * Must still call {@link #swapSilentBack()} after the action completes.
     */
    public static void swapSilentRestoreVisual() {
        if (silentHandle != null) {
            silentHandle.restoreVisual();
        }
    }

    /**
     * Restores the previous slot after a silent swap.
     * Sends {@link ServerboundSetCarriedItemPacket} to the server through the
     * proper packet queue.
     */
    public static boolean swapSilentBack() {
        if (silentHandle != null && !silentHandle.ended()) {
            silentHandle.end();
            silentHandle = null;
            return true;
        }
        return false;
    }

    /**
     * Inventory-swap: moves the item at the given slot to the current hotbar slot
     * via {@link ServerboundContainerClickPacket} with SWAP type.
     */
    public static boolean invSwap(int slot) {
        SwapProtocol.SwapHandle handle = SwapProtocol.swap(SwitchMode.InvSwitch, slot);
        if (handle != null) {
            invSwitchHandle = handle;
            return true;
        }
        return false;
    }

    /**
     * Inventory-swap (instant/queueless version).
     */
    public static boolean invSwapInstantly(int slot) {
        SwapProtocol.SwapHandle handle = SwapProtocol.swapInstantly(SwitchMode.InvSwitch, slot);
        if (handle != null) {
            invSwitchHandle = handle;
            return true;
        }
        return false;
    }

    /**
     * Reverses the last inv-swap by sending the same SWAP click again.
     */
    public static boolean invSwapBack() {
        if (invSwitchHandle != null && !invSwitchHandle.ended()) {
            invSwitchHandle.end();
            invSwitchHandle = null;
            return true;
        }
        return false;
    }

    /**
     * Reverses the last inv-swap (instant version).
     */
    public static boolean invSwapBackInstantly() {
        if (invSwitchHandle != null && !invSwitchHandle.ended()) {
            invSwitchHandle.endInstantly();
            invSwitchHandle = null;
            return true;
        }
        return false;
    }

    /**
     * Pick-silent swap: copies the item at the given slot to the current hotbar slot
     * using the PickItem protocol.
     *
     * @return true if the swap was initiated
     */
    public static boolean pickSwap(int slot) {
        SwapProtocol.SwapHandle handle = SwapProtocol.swap(SwitchMode.PickSilent, slot);
        if (handle != null) {
            pickSilentHandle = handle;
            return true;
        }
        return false;
    }

    /**
     * Pick-silent swap (instant/queueless version).
     */
    public static boolean pickSwapInstantly(int slot) {
        SwapProtocol.SwapHandle handle = SwapProtocol.swapInstantly(SwitchMode.PickSilent, slot);
        if (handle != null) {
            pickSilentHandle = handle;
            return true;
        }
        return false;
    }

    /**
     * Reverses the last pick-swap.
     */
    public static boolean pickSwapBack() {
        if (pickSilentHandle != null && !pickSilentHandle.ended()) {
            pickSilentHandle.end();
            pickSilentHandle = null;
            return true;
        }
        return false;
    }

    /**
     * Instantly reverses the last pick-swap.
     */
    public static boolean pickSwapBackInstantly() {
        if (pickSilentHandle != null && !pickSilentHandle.ended()) {
            pickSilentHandle.endInstantly();
            pickSilentHandle = null;
            return true;
        }
        return false;
    }
}
