package bodevelopment.client.blackout.enums;

import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.util.InvUtils;
import bodevelopment.client.blackout.util.SwapProtocol;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.function.Predicate;

/**
 * Inventory switch mode enum.
 * <p>
 * Uses a LIFO stack internally to track {@link SwapProtocol.SwapHandle}s,
 * preventing cross-module static state conflicts. Each {@link #swap(int)}
 * pushes a new handle; each {@link #swapBack()} pops and ends the last one.
 * <p>
 * New code should prefer {@link #createHandle(int)} for explicit handle management.
 */
public enum SwitchMode {
    Disabled(false, false),
    Normal(true, false),
    Silent(true, false),
    InvSwitch(true, true),
    PickSilent(true, true);

    public final boolean hotbar;
    public final boolean inventory;

    /** LIFO stack of active swap handles — one per module, no cross-contamination. */
    private static final ArrayDeque<SwapProtocol.SwapHandle> SWAP_STACK = new ArrayDeque<>();

    SwitchMode(boolean h, boolean i) {
        this.hotbar = h;
        this.inventory = i;
    }

    /**
     * Performs the swap and returns true if successful.
     * Stores a {@link SwapProtocol.SwapHandle} internally on a LIFO stack.
     * Later call {@link #swapBack()} to restore.
     */
    public boolean swap(int slot) {
        SwapProtocol.SwapHandle handle = SwapProtocol.swap(this, slot);
        if (handle != null) SWAP_STACK.push(handle);
        return handle != null;
    }

    /**
     * Pops and ends the last swap handle from the LIFO stack.
     */
    public boolean swapBack() {
        SwapProtocol.SwapHandle handle = SWAP_STACK.poll();
        if (handle != null && !handle.ended()) {
            handle.end();
            return true;
        }
        return false;
    }

    /**
     * Instant version of {@link #swap(int)}.
     */
    public boolean swapInstantly(int slot) {
        SwapProtocol.SwapHandle handle = SwapProtocol.swapInstantly(this, slot);
        if (handle != null) SWAP_STACK.push(handle);
        return handle != null;
    }

    /**
     * Instant version of {@link #swapBack()}.
     */
    public boolean swapBackInstantly() {
        SwapProtocol.SwapHandle handle = SWAP_STACK.poll();
        if (handle != null && !handle.ended()) {
            handle.endInstantly();
            return true;
        }
        return false;
    }

    /**
     * Creates a stateful {@link SwapProtocol.SwapHandle} for this swap mode.
     * Recommended for new code — provides explicit handle management and
     * prevents cross-module conflicts entirely.
     */
    public SwapProtocol.SwapHandle createHandle(int slot) {
        return SwapProtocol.swap(this, slot);
    }

    public FindResult find(Predicate<ItemStack> predicate) {
        return InvUtils.find(this.hotbar, this.inventory, predicate);
    }

    public FindResult find(Item item) {
        return InvUtils.find(this.hotbar, this.inventory, item);
    }
}
