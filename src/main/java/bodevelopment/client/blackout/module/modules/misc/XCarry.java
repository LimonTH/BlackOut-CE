package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;
import java.util.function.Predicate;

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
        if (event.packet instanceof CloseHandledScreenC2SPacket packet && this.shouldCancel(packet)) {
            event.setCancelled(true);
        }
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null && BlackOut.mc.player.currentScreenHandler instanceof PlayerScreenHandler && this.fill.get()) {
            Slot returnSlot = this.returnSlot();
            Slot emptySlot = this.emptySlot();
            if (returnSlot != null && emptySlot != null) {
                if (this.delayCheck()) {
                    this.clickSlot(returnSlot.id, 0, SlotActionType.QUICK_MOVE);
                    if (!this.anythingPicked()) {
                        this.closeInventory();
                    }
                }
            } else {
                Slot craftSlot = this.craftSlot();
                Slot fillSlot = this.fillSlot();
                if (fillSlot != null && craftSlot != null && this.delayCheck()) {
                    if (this.isPicked(stack -> stack.isOf(fillSlot.getStack().getItem()))) {
                        this.clickSlot(craftSlot.id, 0, SlotActionType.PICKUP);
                    } else {
                        this.clickSlot(fillSlot.id, 0, SlotActionType.PICKUP);
                        this.clickSlot(craftSlot.id, 0, SlotActionType.PICKUP);
                    }

                    if (this.anythingPicked()) {
                        Slot empty = this.emptySlot();
                        if (empty != null) {
                            this.clickSlot(empty.id, 0, SlotActionType.PICKUP);
                        }
                    }

                    this.closeInventory();
                }
            }
        }
    }

    private void clickSlot(int id, int button, SlotActionType actionType) {
        ScreenHandler handler = BlackOut.mc.player.currentScreenHandler;
        BlackOut.mc.interactionManager.clickSlot(handler.syncId, id, button, actionType, BlackOut.mc.player);
        this.prevMove = System.currentTimeMillis();
    }

    private boolean isPicked(Predicate<ItemStack> predicate) {
        return predicate.test(BlackOut.mc.player.currentScreenHandler.getCursorStack());
    }

    private boolean anythingPicked() {
        return !BlackOut.mc.player.currentScreenHandler.getCursorStack().isEmpty();
    }

    private boolean delayCheck() {
        return System.currentTimeMillis() - this.prevMove > this.fillDelay.get() * 1000.0;
    }

    private Slot emptySlot() {
        for (int i = 9; i < 45; i++) {
            Slot slot = BlackOut.mc.player.currentScreenHandler.getSlot(i);
            if (slot.getStack().isEmpty()) {
                return slot;
            }
        }

        return null;
    }

    private Slot fillSlot() {
        for (int i = 9; i < 36; i++) {
            Slot slot = BlackOut.mc.player.currentScreenHandler.getSlot(i);
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty() && this.fillItems.get().contains(stack.getItem()) && this.stacksOf(stack.getItem()) > this.minStacks.get()) {
                return slot;
            }
        }

        return null;
    }

    private Slot craftSlot() {
        for (int i = 1; i < 5; i++) {
            Slot slot = BlackOut.mc.player.currentScreenHandler.getSlot(i);
            if (slot.getStack().isEmpty()) {
                return slot;
            }
        }

        return null;
    }

    private Slot returnSlot() {
        for (int i = 1; i < 5; i++) {
            Slot slot = BlackOut.mc.player.currentScreenHandler.getSlot(i);
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty() && this.fillItems.get().contains(stack.getItem()) && this.stacksOf(stack.getItem()) < this.minStacks.get()) {
                return slot;
            }
        }

        return null;
    }

    private int stacksOf(Item item) {
        int stacks = 0;

        for (int i = 9; i < 45; i++) {
            if (BlackOut.mc.player.currentScreenHandler.getSlot(i).getStack().isOf(item)) {
                stacks++;
            }
        }

        return stacks;
    }

    private boolean shouldCancel(CloseHandledScreenC2SPacket packet) {
        return !this.onlyInventory.get() || packet.getSyncId() == 0;
    }
}
