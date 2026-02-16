package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.interfaces.functional.EpicInterface;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.misc.Simulation;
import bodevelopment.client.blackout.randomstuff.FindResult;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.function.Predicate;

public class InvUtils {
    public static int pickSlot = -1;
    public static int prevSlot = -1;
    private static int[] slots;

    public static int count(boolean hotbar, boolean inventory, Predicate<ItemStack> predicate) {
        int count = 0;

        for (int i = hotbar ? 0 : 9; i < (inventory ? BlackOut.mc.player.getInventory().size() : 9); i++) {
            ItemStack stack = BlackOut.mc.player.getInventory().getStack(i);
            if (stack != null && predicate.test(stack)) {
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
            for (int i = hotbar ? 0 : 9; i < (inventory ? BlackOut.mc.player.getInventory().size() : 9); i++) {
                ItemStack stack = BlackOut.mc.player.getInventory().getStack(i);
                if (stack != null && predicate.test(stack)) {
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
            for (int i = hotbar ? 0 : 9; i < (inventory ? BlackOut.mc.player.getInventory().size() : 9); i++) {
                ItemStack stack = BlackOut.mc.player.getInventory().getStack(i);
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

            for (int i = hotbar ? 0 : 9; i < (inventory ? BlackOut.mc.player.getInventory().size() : 9); i++) {
                ItemStack stack = BlackOut.mc.player.getInventory().getStack(i);
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
        ScreenHandler screen = BlackOut.mc.player.currentScreenHandler;
        int length = screen.slots.size();
        return slot < 9 ? length + slot - 10 : slot + length - 46;
    }

    public static void clickF(int slot) {
        clickSlot(slot, 40, SlotActionType.SWAP);
    }

    public static void clickSlot(int slot, int button, SlotActionType action) {
        ScreenHandler handler = BlackOut.mc.player.currentScreenHandler;
        interactSlot(handler.syncId, getId(slot), button, action);
    }

    private static void clickSlotInstantly(int slot, int button, SlotActionType action) {
        ScreenHandler handler = BlackOut.mc.player.currentScreenHandler;
        interactSlot(handler.syncId, getId(slot), button, action, true);
    }

    public static void interactSlot(int syncId, int slotId, int button, SlotActionType actionType) {
        interactSlot(syncId, slotId, button, actionType, false);
    }

    public static void interactHandler(int slot, int button, SlotActionType actionType) {
        interactSlot(BlackOut.mc.player.currentScreenHandler.syncId, slot, button, actionType);
    }

    public static void interactSlot(int syncId, int slotId, int button, SlotActionType actionType, boolean instant) {
        ScreenHandler screenHandler = BlackOut.mc.player.currentScreenHandler;
        DefaultedList<Slot> defaultedList = screenHandler.slots;
        int i = defaultedList.size();
        ArrayList<ItemStack> list = Lists.newArrayListWithCapacity(i);

        for (Slot slot : defaultedList) {
            list.add(slot.getStack().copy());
        }

        screenHandler.onSlotClick(slotId, button, actionType, BlackOut.mc.player);
        Int2ObjectOpenHashMap<ItemStack> int2ObjectMap = new Int2ObjectOpenHashMap<>();

        for (int j = 0; j < i; j++) {
            ItemStack itemStack = list.get(j);
            ItemStack itemStack2 = defaultedList.get(j).getStack();
            if (!ItemStack.areEqual(itemStack, itemStack2)) {
                int2ObjectMap.put(j, itemStack2.copy());
            }
        }

        if (instant) {
            Managers.PACKET
                    .sendInstantly(
                            new ClickSlotC2SPacket(syncId, screenHandler.getRevision(), slotId, button, actionType, screenHandler.getCursorStack().copy(), int2ObjectMap)
                    );
        } else {
            Managers.PACKET
                    .sendPacket(
                            new ClickSlotC2SPacket(syncId, screenHandler.getRevision(), slotId, button, actionType, screenHandler.getCursorStack().copy(), int2ObjectMap)
                    );
        }
    }

    public static void pickSwap(int slot) {
        pickSlot = slot;
        sendPick(slot, false);
    }

    public static void pickSwapInstantly(int slot) {
        pickSlot = slot;
        sendPick(slot, true);
    }

    public static void pickSwapBack() {
        if (pickSlot >= 0) {
            sendPick(pickSlot, false);
            pickSlot = -1;
        }
    }

    public static void pickSwapBackInstantly() {
        if (pickSlot >= 0) {
            sendPick(pickSlot, true);
            pickSlot = -1;
        }
    }

    private static void sendPick(int slot, boolean instant) {
        if (instant) {
            Managers.PACKET.sendInstantly(new PickFromInventoryC2SPacket(slot));
        } else {
            Managers.PACKET.sendPacket(new PickFromInventoryC2SPacket(slot));
        }

        if (Simulation.getInstance().pickSwitch()) {
            int hbSlot = BlackOut.mc.player.getInventory().getSwappableHotbarSlot();
            Managers.PACKET.ignoreSetSlot.replace(hbSlot, 0.3);
            BlackOut.mc.player.getInventory().selectedSlot = hbSlot;
            ItemStack stack1 = BlackOut.mc.player.getInventory().getStack(slot);
            ItemStack stack2 = BlackOut.mc.player.getInventory().getStack(hbSlot);
            Managers.PACKET.preApply(new ScreenHandlerSlotUpdateS2CPacket(-2, 0, hbSlot, stack1));
            Managers.PACKET.preApply(new ScreenHandlerSlotUpdateS2CPacket(-2, 0, slot, stack2));
            Managers.PACKET.addInvIgnore(new ScreenHandlerSlotUpdateS2CPacket(0, 0, getId(slot), stack1));
            Managers.PACKET.addInvIgnore(new ScreenHandlerSlotUpdateS2CPacket(0, 0, getId(hbSlot), stack2));
        }
    }

    public static void invSwap(int slot) {
        clickSlot(slot, Managers.PACKET.slot, SlotActionType.SWAP);
        slots = new int[]{slot, Managers.PACKET.slot};
    }

    public static void invSwapInstantly(int slot) {
        clickSlotInstantly(slot, Managers.PACKET.slot, SlotActionType.SWAP);
        slots = new int[]{slot, Managers.PACKET.slot};
    }

    public static void invSwapBack() {
        if (slots != null) {
            clickSlot(slots[0], slots[1], SlotActionType.SWAP);
        }
    }

    public static void invSwapBackInstantly() {
        if (slots != null) {
            clickSlotInstantly(slots[0], slots[1], SlotActionType.SWAP);
        }
    }

    public static void swap(int to) {
        prevSlot = BlackOut.mc.player.getInventory().selectedSlot;
        BlackOut.mc.player.getInventory().selectedSlot = to;
        syncSlot(false);
    }

    public static void swapInstantly(int to) {
        prevSlot = BlackOut.mc.player.getInventory().selectedSlot;
        BlackOut.mc.player.getInventory().selectedSlot = to;
        syncSlot(true);
    }

    private static void syncSlot(boolean instant) {
        int i = BlackOut.mc.player.getInventory().selectedSlot;
        if (i != Managers.PACKET.slot) {
            if (instant) {
                Managers.PACKET.sendInstantly(new UpdateSelectedSlotC2SPacket(i));
            } else {
                Managers.PACKET.sendPacket(new UpdateSelectedSlotC2SPacket(i));
            }
        }
    }

    public static void swapBack() {
        if (prevSlot >= 0) {
            swap(prevSlot);
            prevSlot = -1;
        }
    }

    public static void swapBackInstantly() {
        if (prevSlot >= 0) {
            swapInstantly(prevSlot);
            prevSlot = -1;
        }
    }
}
