package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.interfaces.functional.EpicInterface;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.misc.Simulation;
import bodevelopment.client.blackout.randomstuff.FindResult;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class InvUtils {
    public static int pickSlot = -1;
    public static int prevSlot = -1;
    private static int[] slots;

    public static int count(boolean hotbar, boolean inventory, Predicate<ItemStack> predicate) {
        int count = 0;

        for (int i = hotbar ? 0 : 9; i < (inventory ? BlackOut.mc.player.getInventory().getContainerSize() : 9); i++) {
            ItemStack stack = BlackOut.mc.player.getInventory().getItem(i);
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
            for (int i = hotbar ? 0 : 9; i < (inventory ? BlackOut.mc.player.getInventory().getContainerSize() : 9); i++) {
                ItemStack stack = BlackOut.mc.player.getInventory().getItem(i);
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

    private static void clickSlotInstantly(int slot, int button, ClickType action) {
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

    public static boolean pickSwap(int slot) {
        if (slot < 0 || slot >= 36) return false;
        pickSlot = slot;
        sendPick(slot, false);
        return true;
    }

    public static boolean pickSwapInstantly(int slot) {
        if (slot < 0 || slot >= 36) return false;
        pickSlot = slot;
        sendPick(slot, true);
        return true;
    }

    public static boolean pickSwapBack() {
        if (pickSlot >= 0) {
            sendPick(pickSlot, false);
            pickSlot = -1;
            return true;
        }
        return false;
    }

    public static boolean pickSwapBackInstantly() {
        if (pickSlot >= 0) {
            sendPick(pickSlot, true);
            pickSlot = -1;
            return true;
        }
        return false;
    }

    private static void sendPick(int slot, boolean instant) {
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
                clickSlotInstantly(slot, hbSlot, ClickType.SWAP);
            } else {
                clickSlot(slot, hbSlot, ClickType.SWAP);
            }
        }

        if (Simulation.getInstance().pickSwitch()) {
            Managers.PACKET.ignoreSetSlot.replace(hbSlot, 0.3);
            BlackOut.mc.player.getInventory().selected = hbSlot;
            ItemStack stack1 = BlackOut.mc.player.getInventory().getItem(slot);
            ItemStack stack2 = BlackOut.mc.player.getInventory().getItem(hbSlot);
            Managers.PACKET.preApply(new ClientboundContainerSetSlotPacket(-2, 0, hbSlot, stack1));
            Managers.PACKET.preApply(new ClientboundContainerSetSlotPacket(-2, 0, slot, stack2));
            Managers.PACKET.addInvIgnore(new ClientboundContainerSetSlotPacket(0, 0, getId(slot), stack1));
            Managers.PACKET.addInvIgnore(new ClientboundContainerSetSlotPacket(0, 0, getId(hbSlot), stack2));
        }
    }

    public static boolean invSwap(int slot) {
        if (slot < 0 || slot >= 36) return false;
        int currentSlot = BlackOut.mc.player.getInventory().selected;
        clickSlot(slot, currentSlot, ClickType.SWAP);
        slots = new int[]{slot, currentSlot};
        if (Managers.PACKET.slot != currentSlot) {
            Managers.PACKET.slot = currentSlot;
        }
        return true;
    }

    public static boolean invSwapInstantly(int slot) {
        if (slot < 0 || slot >= 36) return false;
        int currentSlot = BlackOut.mc.player.getInventory().selected;
        clickSlotInstantly(slot, currentSlot, ClickType.SWAP);
        slots = new int[]{slot, currentSlot};
        if (Managers.PACKET.slot != currentSlot) {
            Managers.PACKET.slot = currentSlot;
        }
        return true;
    }

    public static boolean invSwapBack() {
        if (slots != null) {
            clickSlot(slots[0], slots[1], ClickType.SWAP);
            return true;
        }
        return false;
    }

    public static boolean invSwapBackInstantly() {
        if (slots != null) {
            clickSlotInstantly(slots[0], slots[1], ClickType.SWAP);
            return true;
        }
        return false;
    }

    public static boolean swap(int to) {
        prevSlot = BlackOut.mc.player.getInventory().selected;
        BlackOut.mc.player.getInventory().selected = to;
        return syncSlot(false);
    }

    public static boolean swapInstantly(int to) {
        prevSlot = BlackOut.mc.player.getInventory().selected;
        BlackOut.mc.player.getInventory().selected = to;
        return syncSlot(true);
    }

    private static boolean syncSlot(boolean instant) {
        int i = BlackOut.mc.player.getInventory().selected;
        if (i != Managers.PACKET.slot) {
            if (instant) {
                Managers.PACKET.sendInstantly(new ServerboundSetCarriedItemPacket(i));
            } else {
                Managers.PACKET.sendPacket(new ServerboundSetCarriedItemPacket(i));
            }
            Managers.PACKET.slot = i;
            return true;
        }
        return false;
    }

    public static boolean swapBack() {
        if (prevSlot >= 0) {
            boolean sent = swap(prevSlot);
            prevSlot = -1;
            return sent;
        }
        return false;
    }

    public static boolean swapBackInstantly() {
        if (prevSlot >= 0) {
            boolean sent = swapInstantly(prevSlot);
            prevSlot = -1;
            return sent;
        }
        return false;
    }
}
