package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class XCarry extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Boolean> fill = this.sgGeneral.booleanSetting("Auto Refill", false, "Automatically moves specific items from your inventory into the crafting slots for extra storage.");
    private final Setting<Double> fillDelay = this.sgGeneral.doubleSetting("Refill Delay", 1.0, 0.0, 5.0, 0.05, "The delay between automated inventory movements.", this.fill::get);
    private final Setting<List<Item>> fillItems = this.sgGeneral.itemListSetting("Priority Items", "The specific items allowed to be stored in the crafting grid slots.", this.fill::get, Items.END_CRYSTAL, Items.EXPERIENCE_BOTTLE);
    private final Setting<Integer> minStacks = this.sgGeneral.intSetting("Stack Threshold", 1, 0, 10, 1, "The minimum number of stacks required in your main inventory before refilling the crafting grid.", this.fill::get);
    private final Setting<Boolean> onlyInventory = this.sgGeneral.booleanSetting("Inventory Only", true, "Limits packet cancellation to the player's main inventory, ignoring other container types.");

    private long prevMove = 0L;

    public XCarry() {
        super("XCarry", "Allows you to store items in your crafting grid by canceling the packets that clear it when closing your inventory.", SubCategory.MISC, true);
    }

    @Event
    public void onSend(PacketEvent.Send event) {
        if (event.packet instanceof ServerboundContainerClosePacket packet && this.shouldCancel(packet)) {
            event.setCancelled(true);
        }
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.level != null && BlackOut.mc.player.containerMenu instanceof InventoryMenu && this.fill.get()) {
            Slot returnSlot = this.returnSlot();
            Slot emptySlot = this.emptySlot();
            if (returnSlot != null && emptySlot != null) {
                if (this.delayCheck()) {
                    this.clickSlot(returnSlot.index, 0, ClickType.QUICK_MOVE);
                    if (!this.anythingPicked()) {
                        this.closeInventory();
                    }
                }
            } else {
                Slot craftSlot = this.craftSlot();
                Slot fillSlot = this.fillSlot();
                if (fillSlot != null && craftSlot != null && this.delayCheck()) {
                    if (this.isPicked(stack -> stack.is(fillSlot.getItem().getItem()))) {
                        this.clickSlot(craftSlot.index, 0, ClickType.PICKUP);
                    } else {
                        this.clickSlot(fillSlot.index, 0, ClickType.PICKUP);
                        this.clickSlot(craftSlot.index, 0, ClickType.PICKUP);
                    }

                    if (this.anythingPicked()) {
                        Slot empty = this.emptySlot();
                        if (empty != null) {
                            this.clickSlot(empty.index, 0, ClickType.PICKUP);
                        }
                    }

                    this.closeInventory();
                }
            }
        }
    }

    private void clickSlot(int id, int button, ClickType actionType) {
        AbstractContainerMenu handler = BlackOut.mc.player.containerMenu;
        BlackOut.mc.gameMode.handleInventoryMouseClick(handler.containerId, id, button, actionType, BlackOut.mc.player);
        this.prevMove = System.currentTimeMillis();
    }

    private boolean isPicked(Predicate<ItemStack> predicate) {
        return predicate.test(BlackOut.mc.player.containerMenu.getCarried());
    }

    private boolean anythingPicked() {
        return !BlackOut.mc.player.containerMenu.getCarried().isEmpty();
    }

    private boolean delayCheck() {
        return System.currentTimeMillis() - this.prevMove > this.fillDelay.get() * 1000.0;
    }

    private Slot emptySlot() {
        for (int i = 9; i < 45; i++) {
            Slot slot = BlackOut.mc.player.containerMenu.getSlot(i);
            if (slot.getItem().isEmpty()) {
                return slot;
            }
        }

        return null;
    }

    private Slot fillSlot() {
        for (int i = 9; i < 36; i++) {
            Slot slot = BlackOut.mc.player.containerMenu.getSlot(i);
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && this.fillItems.get().contains(stack.getItem()) && this.stacksOf(stack.getItem()) > this.minStacks.get()) {
                return slot;
            }
        }

        return null;
    }

    private Slot craftSlot() {
        for (int i = 1; i < 5; i++) {
            Slot slot = BlackOut.mc.player.containerMenu.getSlot(i);
            if (slot.getItem().isEmpty()) {
                return slot;
            }
        }

        return null;
    }

    private Slot returnSlot() {
        for (int i = 1; i < 5; i++) {
            Slot slot = BlackOut.mc.player.containerMenu.getSlot(i);
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && this.fillItems.get().contains(stack.getItem()) && this.stacksOf(stack.getItem()) < this.minStacks.get()) {
                return slot;
            }
        }

        return null;
    }

    private int stacksOf(Item item) {
        int stacks = 0;

        for (int i = 9; i < 45; i++) {
            if (BlackOut.mc.player.containerMenu.getSlot(i).getItem().is(item)) {
                stacks++;
            }
        }

        return stacks;
    }

    private boolean shouldCancel(ServerboundContainerClosePacket packet) {
        return !this.onlyInventory.get() || packet.getContainerId() == 0;
    }
}
