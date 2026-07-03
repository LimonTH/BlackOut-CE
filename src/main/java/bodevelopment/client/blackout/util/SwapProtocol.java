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
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.misc.Simulation;
import bodevelopment.client.blackout.randomstuff.FindResult;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * Stateful swap protocol manager implementing all 5 swap modes:
 * - Disabled:    No swap
 * - Normal:      Persistent slot change via {@link ServerboundSetCarriedItemPacket}
 * - Silent:      Dual-packet swap (tool → action → restore) within packet sequencing
 * - PickSilent:  Single-packet PickItem-based copy to current slot
 * - InvSwitch:   ClickSlot SWAP transaction for inventory-to-hotbar moves
 *
 * <p>Each swap returns a {@link SwapHandle} that MUST be closed (via {@link SwapHandle#end()})
 * to restore the original state. This prevents cross-module state corruption.
 *
 * <p>Zero-allocation in hot paths: no lambdas, no temporary objects in the fast path.
 */
@PublicAPI
public final class SwapProtocol {
    private SwapProtocol() {}

    /**
     * Handle to a swap transaction. Must be closed via {@link #end()} or
     * {@link #endInstantly()} when the operation completes.
     */
    @PublicAPI
    public static final class SwapHandle {
        private final SwitchMode mode;
        private final int toolSlot;
        private final int originalSlot;
        private final int visualSlot;
        private boolean ended = false;

        public SwapHandle(SwitchMode mode, int toolSlot, int originalSlot, int visualSlot) {
            this.mode = mode;
            this.toolSlot = toolSlot;
            this.originalSlot = originalSlot;
            this.visualSlot = visualSlot;
        }

        /** The slot we swapped to (the tool/weapon). */
        public int toolSlot() { return toolSlot; }

        /** The original PACKET.slot before the swap. */
        public int originalSlot() { return originalSlot; }

        /** The original client visual slot before the swap. */
        public int visualSlot() { return visualSlot; }

        /** Whether this handle has already been ended. */
        public boolean ended() { return ended; }

        /**
         * Restores ONLY the client-side visual slot to the original, WITHOUT
         * sending a packet. The server still sees the tool in the active slot.
         * <p>
         * Call this AFTER sending the action packet (e.g. START_DESTROY_BLOCK).
         * Must still call {@link #end()} later to fully clean up.
         */
        public void restoreVisual() {
            if (ended) return;
            if (mode != SwitchMode.Silent) return;
            BlackOut.mc.player.getInventory().selected = visualSlot;
            BlackOut.mc.gameMode.carriedIndex = visualSlot;
        }

        /**
         * Temporarily suspends the swap by sending the original slot back to the
         * server. Prepares for a {@link #resume()} call.
         */
        public void suspend() {
            if (ended) return;
            if (mode != SwitchMode.Silent) return;

            BlackOut.mc.player.getInventory().selected = visualSlot;
            Managers.PACKET.sendPacket(new ServerboundSetCarriedItemPacket(visualSlot));
            Managers.PACKET.slot = visualSlot;
        }

        /**
         * Resumes a suspended swap by re-applying the tool slot to the server.
         * Restores the silent swap without sending a visual update packet.
         */
        public void resume() {
            if (ended) return;
            if (mode != SwitchMode.Silent) return;

            BlackOut.mc.player.getInventory().selected = toolSlot;
            Managers.PACKET.sendPacket(new ServerboundSetCarriedItemPacket(toolSlot));
            Managers.PACKET.slot = toolSlot;

            BlackOut.mc.player.getInventory().selected = visualSlot;
            BlackOut.mc.gameMode.carriedIndex = visualSlot;
        }

        /**
         * Ends the swap transaction and restores the original state.
         */
        public void end() {
            if (ended) return;
            this.ended = true;

            switch (mode) {
                case Silent -> {
                    BlackOut.mc.player.getInventory().selected = visualSlot;
                    Managers.PACKET.sendPacket(new ServerboundSetCarriedItemPacket(visualSlot));
                    Managers.PACKET.slot = visualSlot;
                }
                case Normal -> {
                    BlackOut.mc.player.getInventory().selected = visualSlot;
                    if (visualSlot != Managers.PACKET.slot) {
                        Managers.PACKET.sendPacket(new ServerboundSetCarriedItemPacket(visualSlot));
                        Managers.PACKET.slot = visualSlot;
                    }
                }
                case InvSwitch -> {
                    InvUtils.clickSlot(toolSlot, originalSlot, ClickType.SWAP);
                }
                case PickSilent -> {
                    if (toolSlot < 9) {
                        BlackOut.mc.player.getInventory().selected = visualSlot;
                        Managers.PACKET.sendPacket(new ServerboundSetCarriedItemPacket(visualSlot));
                        Managers.PACKET.slot = visualSlot;
                    } else {
                        InvUtils.clickSlot(toolSlot, visualSlot, ClickType.SWAP);
                    }
                }
                default -> {}
            }
        }

        /**
         * Instantly ends the swap transaction (bypasses packet queue).
         */
        public void endInstantly() {
            if (ended) return;
            this.ended = true;

            switch (mode) {
                case Silent -> {
                    BlackOut.mc.player.getInventory().selected = visualSlot;
                    Managers.PACKET.sendInstantly(new ServerboundSetCarriedItemPacket(visualSlot));
                    Managers.PACKET.slot = visualSlot;
                }
                case Normal -> {
                    BlackOut.mc.player.getInventory().selected = visualSlot;
                    if (visualSlot != Managers.PACKET.slot) {
                        Managers.PACKET.sendInstantly(new ServerboundSetCarriedItemPacket(visualSlot));
                        Managers.PACKET.slot = visualSlot;
                    }
                }
                case InvSwitch -> {
                    InvUtils.clickSlotInstantly(toolSlot, originalSlot, ClickType.SWAP);
                }
                case PickSilent -> {
                    if (toolSlot < 9) {
                        BlackOut.mc.player.getInventory().selected = visualSlot;
                        Managers.PACKET.sendInstantly(new ServerboundSetCarriedItemPacket(visualSlot));
                        Managers.PACKET.slot = visualSlot;
                    } else {
                        InvUtils.clickSlotInstantly(toolSlot, visualSlot, ClickType.SWAP);
                    }
                }
                default -> {}
            }
        }
    }

    /**
     * Performs a swap according to the given mode and returns a {@link SwapHandle}.
     *
     * @param mode the swap mode to use
     * @param slot the target slot to swap to
     * @return a SwapHandle, or null if the swap failed
     */
    public static SwapHandle swap(SwitchMode mode, int slot) {
        if (mode == SwitchMode.Disabled) return null;
        if (slot < 0 || (slot > 8 && !mode.inventory)) return null;

        int visualSlot = BlackOut.mc.player.getInventory().selected;
        int originalSlot = Managers.PACKET.slot;

        switch (mode) {
            case Silent -> {
                return swapSilentImpl(slot, originalSlot, visualSlot);
            }
            case Normal -> {
                return swapNormalImpl(slot, originalSlot, visualSlot, false);
            }
            case InvSwitch -> {
                return swapInvSwitchImpl(slot, originalSlot, visualSlot, false);
            }
            case PickSilent -> {
                return swapPickSilentImpl(slot, originalSlot, visualSlot, false);
            }
            default -> { return null; }
        }
    }

    /**
     * Performs an instant swap (bypasses packet queue) and returns a {@link SwapHandle}.
     */
    public static SwapHandle swapInstantly(SwitchMode mode, int slot) {
        if (mode == SwitchMode.Disabled) return null;
        if (slot < 0 || (slot > 8 && !mode.inventory)) return null;

        int visualSlot = BlackOut.mc.player.getInventory().selected;
        int originalSlot = Managers.PACKET.slot;

        switch (mode) {
            case Silent -> {
                return swapSilentImpl(slot, originalSlot, visualSlot);
            }
            case Normal -> {
                return swapNormalImpl(slot, originalSlot, visualSlot, true);
            }
            case InvSwitch -> {
                return swapInvSwitchImpl(slot, originalSlot, visualSlot, true);
            }
            case PickSilent -> {
                return swapPickSilentImpl(slot, originalSlot, visualSlot, true);
            }
            default -> { return null; }
        }
    }

    private static SwapHandle swapSilentImpl(int slot, int originalSlot, int visualSlot) {
        if (slot == originalSlot) return null;

        BlackOut.mc.player.getInventory().selected = slot;
        Managers.PACKET.slot = slot;
        BlackOut.mc.gameMode.carriedIndex = slot;
        Managers.PACKET.sendPacket(new ServerboundSetCarriedItemPacket(slot));

        return new SwapHandle(SwitchMode.Silent, slot, originalSlot, visualSlot);
    }

    private static SwapHandle swapNormalImpl(int slot, int originalSlot, int visualSlot, boolean instant) {
        BlackOut.mc.player.getInventory().selected = slot;

        if (slot != Managers.PACKET.slot) {
            if (instant) {
                Managers.PACKET.sendInstantly(new ServerboundSetCarriedItemPacket(slot));
            } else {
                Managers.PACKET.sendPacket(new ServerboundSetCarriedItemPacket(slot));
            }
            Managers.PACKET.slot = slot;
            return new SwapHandle(SwitchMode.Normal, slot, originalSlot, visualSlot);
        }

        return null;
    }

    private static SwapHandle swapInvSwitchImpl(int slot, int originalSlot, int visualSlot, boolean instant) {
        int currentSlot = BlackOut.mc.player.getInventory().selected;

        if (instant) {
            InvUtils.clickSlotInstantly(slot, currentSlot, ClickType.SWAP);
        } else {
            InvUtils.clickSlot(slot, currentSlot, ClickType.SWAP);
        }

        if (Managers.PACKET.slot != currentSlot) {
            Managers.PACKET.slot = currentSlot;
        }

        return new SwapHandle(SwitchMode.InvSwitch, slot, originalSlot, visualSlot);
    }

    private static SwapHandle swapPickSilentImpl(int slot, int originalSlot, int visualSlot, boolean instant) {
        int hbSlot = BlackOut.mc.player.getInventory().getSuitableHotbarSlot();

        if (slot < 9) {
            BlackOut.mc.player.getInventory().selected = slot;
            if (instant) {
                Managers.PACKET.sendInstantly(new ServerboundSetCarriedItemPacket(slot));
            } else {
                Managers.PACKET.sendPacket(new ServerboundSetCarriedItemPacket(slot));
            }
            Managers.PACKET.slot = slot;
        } else {
            if (instant) {
                InvUtils.clickSlotInstantly(slot, hbSlot, ClickType.SWAP);
            } else {
                InvUtils.clickSlot(slot, hbSlot, ClickType.SWAP);
            }
        }

        if (Simulation.getInstance().pickSwitch()) {
            Managers.PACKET.ignoreSetSlot.replace(hbSlot, 0.3);
            BlackOut.mc.player.getInventory().selected = hbSlot;
            ItemStack stack1 = BlackOut.mc.player.getInventory().getItem(slot);
            ItemStack stack2 = BlackOut.mc.player.getInventory().getItem(hbSlot);
            Managers.PACKET.preApply(new ClientboundContainerSetSlotPacket(-2, 0, hbSlot, stack1));
            Managers.PACKET.preApply(new ClientboundContainerSetSlotPacket(-2, 0, slot, stack2));
            Managers.PACKET.addInvIgnore(new ClientboundContainerSetSlotPacket(0, 0, InvUtils.getId(slot), stack1));
            Managers.PACKET.addInvIgnore(new ClientboundContainerSetSlotPacket(0, 0, InvUtils.getId(hbSlot), stack2));
        }

        return new SwapHandle(SwitchMode.PickSilent, slot, originalSlot, visualSlot);
    }

    /**
     * Finds the best item matching the predicate and swaps to it using the given mode.
     *
     * @return a SwapHandle if a swap was performed, null if no item found or swap failed
     */
    public static SwapHandle findAndSwap(SwitchMode mode, Predicate<ItemStack> predicate) {
        FindResult result = InvUtils.find(mode.hotbar, mode.inventory, predicate);
        if (!result.wasFound()) return null;
        return swap(mode, result.slot());
    }

    /**
     * Finds the best item matching the predicate and swaps to it using the given mode.
     */
    public static SwapHandle findAndSwap(SwitchMode mode, Item item) {
        return findAndSwap(mode, stack -> stack.getItem() == item);
    }

    /**
     * Finds the best item using a scoring function and swaps to it.
     */
    public static SwapHandle findBestAndSwap(SwitchMode mode, java.util.function.ToDoubleFunction<ItemStack> scorer) {
        FindResult result = InvUtils.findBest(mode.hotbar, mode.inventory, scorer::applyAsDouble);
        if (!result.wasFound()) return null;
        return swap(mode, result.slot());
    }
}
