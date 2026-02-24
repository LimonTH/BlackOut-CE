package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.*;
import bodevelopment.client.blackout.interfaces.functional.DoubleConsumer;
import bodevelopment.client.blackout.keys.KeyBind;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.OnlyDev;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.ItemUtils;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.block.*;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

// TODO: NEED PATCHES
@OnlyDev
public class Manager extends Module {
    private static Manager INSTANCE;

    public final SettingGroup sgGeneral = this.addGroup("General");
    public final SettingGroup sgAutoArmor = this.addGroup("Auto Armor");
    public final SettingGroup sgHotbar = this.addGroup("Hotbar");
    public final SettingGroup sgReplenish = this.addGroup("Replenish");
    public final SettingGroup sgCleaner = this.addGroup("Cleaner");

    private final Setting<Boolean> onlyInv = this.sgGeneral.booleanSetting("Inventory Only", false, "Restricts the module to operate only when the player's inventory screen is manually opened.");
    private final Setting<Double> inventoryOpenTime = this.sgGeneral.doubleSetting("Activation Delay", 0.1, 0.0, 1.0, 0.01, "The duration to wait after opening the inventory before starting management tasks.", this.onlyInv::get);
    private final Setting<Boolean> silentInstant = this.sgGeneral.booleanSetting("Silent Instant", true, "Performs inventory operations instantly without visual movements while the GUI is closed.", () -> !this.onlyInv.get());
    private final Setting<Boolean> inInventoryInstant = this.sgGeneral.booleanSetting("GUI Instant", true, "Enables instantaneous item movement while the inventory screen is visible.");
    private final Setting<Double> cooldown = this.sgGeneral.doubleSetting("Task Interval", 0.3, 0.0, 1.0, 0.01, "The cooldown in seconds between individual inventory actions.");
    private final Setting<Boolean> tpDisable = this.sgGeneral.booleanSetting("Teleport Disable", false, "Automatically deactivates the module upon changing dimensions or teleporting.");
    private final Setting<Boolean> pauseCombat = this.sgGeneral.booleanSetting("Pause in Combat", false, "Suspends inventory management while the player is taking damage.");
    private final Setting<Boolean> stopRotations = this.sgGeneral.booleanSetting("Lock Rotations", true, "Prevents head movement during automated item transfers to avoid desync.");
    private final Setting<Boolean> autoArmor = this.sgAutoArmor.booleanSetting("Auto Armor", true, "Automatically equips the most protective armor pieces found in the inventory.");
    private final Setting<KeyBind> chestSwap = this.sgAutoArmor.keySetting("Elytra Switch", "A hotkey to toggle between prioritizing a Chestplate or an Elytra.");
    private final Setting<Boolean> elytra = this.sgAutoArmor.booleanSetting("Elytra Priority", false, "Gives the Elytra higher priority than chestplates during automatic equipping.", () -> this.chestSwap.get().value == null || this.chestSwap.get().value.key >= 0);
    private final Setting<Integer> weaponSlot = this.sgHotbar.intSetting("Primary Weapon Slot", 0, 0, 9, 1, "The designated hotbar slot for your main weapon.");
    private final Setting<List<Item>> slot1 = this.sgHotbar.itemListSetting("Slot 1 Filter", "The items allowed to occupy hotbar slot 1.", () -> this.weaponSlot.get() != 1);
    private final Setting<List<Item>> slot2 = this.sgHotbar.itemListSetting("Slot 2 Filter", "The items allowed to occupy hotbar slot 2.", () -> this.weaponSlot.get() != 2);
    private final Setting<List<Item>> slot3 = this.sgHotbar.itemListSetting("Slot 3 Filter", "The items allowed to occupy hotbar slot 3.", () -> this.weaponSlot.get() != 3);
    private final Setting<List<Item>> slot4 = this.sgHotbar.itemListSetting("Slot 4 Filter", "The items allowed to occupy hotbar slot 4.", () -> this.weaponSlot.get() != 4);
    private final Setting<List<Item>> slot5 = this.sgHotbar.itemListSetting("Slot 5 Filter", "The items allowed to occupy hotbar slot 5.", () -> this.weaponSlot.get() != 5);
    private final Setting<List<Item>> slot6 = this.sgHotbar.itemListSetting("Slot 6 Filter", "The items allowed to occupy hotbar slot 6.", () -> this.weaponSlot.get() != 6);
    private final Setting<List<Item>> slot7 = this.sgHotbar.itemListSetting("Slot 7 Filter", "The items allowed to occupy hotbar slot 7.", () -> this.weaponSlot.get() != 7);
    private final Setting<List<Item>> slot8 = this.sgHotbar.itemListSetting("Slot 8 Filter", "The items allowed to occupy hotbar slot 8.", () -> this.weaponSlot.get() != 8);
    private final Setting<List<Item>> slot9 = this.sgHotbar.itemListSetting("Slot 9 Filter", "The items allowed to occupy hotbar slot 9.", () -> this.weaponSlot.get() != 9);
    @SuppressWarnings("unchecked")
    private final Setting<List<Item>>[] slotSettings = (Setting<List<Item>>[]) new Setting<?>[]{
            this.slot1, this.slot2, this.slot3, this.slot4, this.slot5, this.slot6, this.slot7, this.slot8, this.slot9
    };
    private final Setting<WeaponMode> weaponMode = this.sgHotbar.enumSetting("Preferred Weapon", WeaponMode.Sword, "The type of weapon to prioritize for the primary weapon slot.");
    private final Setting<Boolean> replenish = this.sgReplenish.booleanSetting("Auto Replenish", false, "Refills hotbar stacks from the inventory when they drop below a certain threshold.");
    private final Setting<Boolean> unstackableReplenish = this.sgReplenish.booleanSetting("Replenish Unstackable", true, "Automatically replaces spent single-use items like soups or potions.");
    private final Setting<Integer> percetageLeft = this.sgReplenish.intSetting("Refill Threshold %", 25, 0, 100, 1, "The percentage of a stack remaining before it is automatically replenished.");
    private final Setting<Double> replenishMemory = this.sgReplenish.doubleSetting("Slot Memory", 1.0, 0.0, 5.0, 0.05, "The time in seconds the module remembers which item previously occupied an empty slot.");
    private final Setting<List<Item>> cleanerItems = this.sgCleaner.itemListSetting("Trash Items", "Items that will be automatically dropped from the inventory.");
    private final Setting<Boolean> badArmor = this.sgCleaner.booleanSetting("Drop Inferior Armor", false, "Discards armor pieces that are statistically worse than what is currently equipped.");
    private final Setting<Boolean> badSwords = this.sgCleaner.booleanSetting("Drop Inferior Swords", false, "Discards swords with lower damage values than the primary weapon.");
    private final Setting<Boolean> badAxes = this.sgCleaner.booleanSetting("Drop Inferior Axes", false, "Discards axes that are inferior based on the selected comparison mode.");
    private final Setting<AxeCompareMode> axeComparing = this.sgCleaner.enumSetting("Axe Metric", AxeCompareMode.Efficiency, "Determines whether axes are evaluated by combat damage or mining efficiency.", this.badAxes::get);
    private final Setting<Boolean> badPickaxes = this.sgCleaner.booleanSetting("Drop Inferior Pickaxes", false, "Discards pickaxes with lower mining speeds than the best one available.");
    private final Setting<Boolean> badBows = this.sgCleaner.booleanSetting("Drop Inferior Bows", false, "Discards bows with lower damage or enchantments than the best one available.");

    private final ReplenishSlot[] replenishItems = new ReplenishSlot[]{
            new ReplenishSlot(),
            new ReplenishSlot(),
            new ReplenishSlot(),
            new ReplenishSlot(),
            new ReplenishSlot(),
            new ReplenishSlot(),
            new ReplenishSlot(),
            new ReplenishSlot(),
            new ReplenishSlot()
    };
    private Action currentAction = null;
    private boolean prevOpen = false;
    private Boolean currentlyElytra = null;
    private int moveProgress = 0;
    private long openTime = 0L;
    private long prevMove = 0L;
    private long containerInteractTime = 0L;
    private long prevDamage = 0L;

    public Manager() {
        super("Manager", "An all-in-one inventory automation utility that handles armor equipping, hotbar organization, item replenishing, and inventory cleaning.", SubCategory.MISC, true);
        INSTANCE = this;
    }

    public static Manager getInstance() {
        return INSTANCE;
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        this.checkChestSwap();
        if (this.canUpdate()) {
            this.update();
        }
    }

    @Event
    public void onGameJoin(GameJoinEvent event) {
        if (this.tpDisable.get()) {
            this.disable(this.getDisplayName() + " was disabled due to server change/teleport", 5, Notifications.Type.Info);
        }
    }

    @Event
    public void onSent(PacketEvent.Sent event) {
        if (event.packet instanceof PlayerInteractBlockC2SPacket packet) {
            BlockHitResult hitResult = packet.getBlockHitResult();
            Block block = BlackOut.mc.world.getBlockState(hitResult.getBlockPos()).getBlock();
            if (block instanceof AbstractChestBlock || block instanceof AnvilBlock || block instanceof HopperBlock || block instanceof DispenserBlock) {
                this.containerInteractTime = System.currentTimeMillis();
            }
        }
    }

    @Event
    public void onKey(KeyEvent event) {
        if (event.pressed && this.chestSwap.get().isKey(event.key) && this.currentlyElytra != null) {
            this.doChestSwap();
        }
    }

    @Event
    public void onKey(MouseButtonEvent event) {
        if (event.pressed && this.chestSwap.get().isMouse(event.button) && this.currentlyElytra != null) {
            this.doChestSwap();
        }
    }

    @Override
    public String getInfo() {
        if ((this.elytra.get() || this.chestSwap.get().value != null && this.chestSwap.get().value.key >= 0) && this.currentlyElytra != null) {
            return this.currentlyElytra ? "Elytra" : "Armor";
        } else {
            return null;
        }
    }

    public boolean shouldNoRotate() {
        return this.stopRotations.get() && System.currentTimeMillis() - this.prevMove < 300L;
    }

    private void doChestSwap() {
        this.currentlyElytra = !this.currentlyElytra;
        this.sendNotification(
                this.getDisplayName() + " " + Formatting.BLUE + " changed to " + (this.currentlyElytra ? "Elytra" : "Chestplate"),
                this.getDisplayName() + "  changed to " + (this.currentlyElytra ? "Elytra" : "Chestplate"),
                "Chest Swap",
                Notifications.Type.Info,
                2.0
        );
    }

    private void checkChestSwap() {
        if (BlackOut.mc.player != null) {
            ItemStack stack = BlackOut.mc.player.getInventory().getArmorStack(EquipmentSlot.CHEST.getEntitySlotId());
            boolean isElytra = stack.isOf(Items.ELYTRA);
            if (this.currentlyElytra == null) {
                this.currentlyElytra = isElytra;
            }
        }
    }

    private boolean canUpdate() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (BlackOut.mc.player.hurtTime > 0) {
                this.prevDamage = System.currentTimeMillis();
            }

            if (this.inCombat()) {
                return false;
            } else {
                if (this.onlyInv.get()) {
                    boolean open = BlackOut.mc.currentScreen instanceof InventoryScreen;
                    if (open && !this.prevOpen) {
                        this.openTime = System.currentTimeMillis();
                    }

                    this.prevOpen = open;
                    if (!open) {
                        return false;
                    }

                    if (System.currentTimeMillis() - this.openTime < this.inventoryOpenTime.get() * 1000.0) {
                        return false;
                    }
                }

                return BlackOut.mc.player.currentScreenHandler instanceof PlayerScreenHandler;
            }
        } else {
            return false;
        }
    }

    private void update() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = BlackOut.mc.player.currentScreenHandler.getSlot(36 + i).getStack();
            ReplenishSlot slot = this.replenishItems[i];
            if (stack.isEmpty()) {
                if (System.currentTimeMillis() - slot.lastSeen > this.replenishMemory.get() * 1000.0) {
                    slot.item = null;
                }
            } else {
                slot.item = stack.getItem();
                slot.lastSeen = System.currentTimeMillis();
            }
        }

        if (this.shouldUpdateAction()) {
            this.currentAction = this.nextAction();
        }

        if (this.currentAction != null) {
            this.handleAction();
        }
    }

    private boolean shouldUpdateAction() {
        return !this.anythingPicked() && this.moveProgress <= 0;
    }

    private void handleAction() {
        if (this.currentAction instanceof Drop drop && this.delayCheck()) {
            drop.consumer.accept(BlackOut.mc.interactionManager, BlackOut.mc.player.currentScreenHandler);
            this.setPrev();
            this.closeIfPossible();
            this.resetAction();
        }

        if (this.currentAction instanceof Move move && this.delayCheck()) {
            if (this.shouldInstant()) {
                this.moveInstantly(move);
            } else if (this.updateSlowMove(move)) {
                this.setPrev();
            }
        }

        if (this.currentAction instanceof QuickMove quickMove && this.delayCheck()) {
            quickMove.consumer.accept(BlackOut.mc.interactionManager, BlackOut.mc.player.currentScreenHandler);
            this.setPrev();
            this.closeIfPossible();
            this.resetAction();
        }

        if (this.currentAction instanceof Swap swap && this.delayCheck()) {
            swap.consumer.accept(BlackOut.mc.interactionManager, BlackOut.mc.player.currentScreenHandler);
            this.setPrev();
            this.closeIfPossible();
            this.resetAction();
        }
    }

    private void moveInstantly(Move move) {
        if (this.isPicked(move.predicate)) {
            this.clickSlot(move.to, 0, SlotActionType.PICKUP);
        } else {
            this.clickSlot(move.from, 0, SlotActionType.PICKUP);
            this.clickSlot(move.to, 0, SlotActionType.PICKUP);
        }

        if (this.anythingPicked()) {
            Slot empty = this.findSlot(slot -> slot.getStack().isEmpty(), null, FindArea.Both);
            if (empty != null) {
                this.clickSlot(empty.id, 0, SlotActionType.PICKUP);
            }
        }

        this.resetAction();
        this.setPrev();
        this.closeInventory();
    }

    private boolean isPicked(Predicate<ItemStack> predicate) {
        return predicate.test(BlackOut.mc.player.currentScreenHandler.getCursorStack());
    }

    private boolean anythingPicked() {
        return !BlackOut.mc.player.currentScreenHandler.getCursorStack().isEmpty();
    }

    private boolean shouldInstant() {
        return BlackOut.mc.currentScreen instanceof InventoryScreen ? this.inInventoryInstant.get() : this.silentInstant.get();
    }

    private void closeIfPossible() {
        if (!this.anythingPicked()) {
            this.closeInventory();
        }
    }

    private boolean updateSlowMove(Move move) {
        switch (this.moveProgress) {
            case 0:
                if (this.delayCheck()) {
                    this.clickSlot(move.from, 0, SlotActionType.PICKUP);
                    this.moveProgress++;
                    return true;
                }
                break;
            case 1:
                if (!move.predicate.test(BlackOut.mc.player.currentScreenHandler.getCursorStack())) {
                    this.resetAction();
                    this.closeIfPossible();
                    return false;
                }

                if (this.delayCheck()) {
                    this.clickSlot(move.to, 0, SlotActionType.PICKUP);
                    this.moveProgress++;
                    return true;
                }
                break;
            case 2:
                if (!move.predicate.test(BlackOut.mc.player.currentScreenHandler.getCursorStack())) {
                    this.resetAction();
                    this.closeIfPossible();
                    return false;
                }

                if (this.delayCheck()) {
                    Slot empty = this.findSlot(slot -> slot.getStack().isEmpty(), null, FindArea.Inventory);
                    if (empty != null) {
                        this.clickSlot(empty.id, 0, SlotActionType.PICKUP);
                    }

                    this.closeIfPossible();
                    this.resetAction();
                    return empty != null;
                }
        }

        return false;
    }

    private void resetAction() {
        this.currentAction = null;
        this.moveProgress = 0;
    }

    private void clickSlot(int id, int button, SlotActionType actionType) {
        ScreenHandler handler = BlackOut.mc.player.currentScreenHandler;
        BlackOut.mc.interactionManager.clickSlot(handler.syncId, id, button, actionType, BlackOut.mc.player);
    }

    private boolean delayCheck() {
        return !(System.currentTimeMillis() - this.containerInteractTime < Simulation.getInstance().managerStop() * 1000.0) && System.currentTimeMillis() - this.prevMove > this.cooldown.get() * 1000.0;
    }

    private void setPrev() {
        this.prevMove = System.currentTimeMillis();
    }

    private Action nextAction() {
        Action hotbarAction = this.findHotbar();
        if (hotbarAction != null) {
            return hotbarAction;
        } else {
            Slot cleanerSlot = this.findCleaner();
            if (cleanerSlot != null) {
                return new Drop(cleanerSlot.id);
            } else {
                if (this.autoArmor.get()) {
                    Action autoArmorAction = this.findAutoArmor();
                    if (autoArmorAction != null) {
                        return autoArmorAction;
                    }
                }

                if (this.replenish.get()) {
                    Action replenishAction = this.findReplenish();
                    return replenishAction;
                }

                return null;
            }
        }
    }

    private Action findReplenish() {
        for (int i = 1; i <= 9; i++) {
            int slotId = 35 + i;
            Slot slot = BlackOut.mc.player.currentScreenHandler.getSlot(slotId);
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) {
                Item item = this.replenishItems[i - 1].item;
                if (item != null && (this.unstackableReplenish.get() || item.getMaxCount() > 1)) {
                    Slot from = this.findSlot(s -> s.getStack().isOf(item), s -> s.getStack().getCount(), FindArea.Inventory);
                    if (from != null) {
                        return new Swap(from.id, i - 1);
                    }
                }
            } else if (!((float) slot.getStack().getCount() / slot.getStack().getMaxCount() * 100.0F > this.percetageLeft.get())) {
                Slot from = this.findSlot(
                        s -> ItemStack.areItemsAndComponentsEqual(stack, s.getStack()), s -> -s.getStack().getCount(), FindArea.Inventory
                );
                if (from != null) {
                    if (this.shouldQuick(i)) {
                        return new QuickMove(from.id);
                    }

                    return new Move(from.id, slotId);
                }
            }
        }

        return null;
    }

    private boolean shouldQuick(int hotbarSlot) {
        for (int i = 1; i <= 9; i++) {
            if (i != hotbarSlot && BlackOut.mc.player.currentScreenHandler.getSlot(35 + i).getStack().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private Action findHotbar() {
        for (int i = 1; i <= 9; i++) {
            int slotId = 35 + i;
            Slot slot = BlackOut.mc.player.currentScreenHandler.getSlot(slotId);
            HotbarSearch search = this.getHotbarSearch(i);
            if (!search.predicate().test(slot)) {
                Slot fromSlot = this.findSlot(s -> search.predicate().test(s) && !this.alreadyValid(s), search.function(), FindArea.Both);
                if (fromSlot != null) {
                    return new Swap(fromSlot.id, i - 1);
                }
            }
        }

        return null;
    }

    private boolean alreadyValid(Slot slot) {
        if (slot.id < 36) {
            return false;
        } else {
            int slotId = slot.id - 35;
            HotbarSearch hotbarSearch = this.getHotbarSearch(slotId);
            return hotbarSearch.predicate().test(slot);
        }
    }

    private HotbarSearch getHotbarSearch(int slot) {
        return this.weaponSlot.get() == slot
                ? this.weaponMode.get().search
                : new HotbarSearch(s -> this.slotSettings[slot - 1].get().contains(s.getStack().getItem()), s -> s.getStack().getCount());
    }

    private Action findAutoArmor() {
        for (EquipmentSlot equipmentSlot : OLEPOSSUtils.equipmentSlots) {
            int toSlot = 8 - equipmentSlot.getEntitySlotId();
            Slot bestArmor = this.findBestArmor(equipmentSlot);
            if (bestArmor != null && bestArmor.id != toSlot) {
                return new Move(bestArmor.id, toSlot);
            }
        }

        return null;
    }

    private Slot findBestArmor(EquipmentSlot equipmentSlot) {
        Slot bestArmor = this.findSlot(
                slot -> slot.getStack().getItem() instanceof ArmorItem armorItem && armorItem.getSlotType() == equipmentSlot,
                slot -> ItemUtils.getArmorValue(slot.getStack()),
                FindArea.All
        );
        if (equipmentSlot != EquipmentSlot.CHEST) {
            return bestArmor;
        } else {
            Slot bestElytra = this.findSlot(
                    slot -> slot.getStack().getItem() instanceof ElytraItem, slot -> ItemUtils.getElytraValue(slot.getStack()), FindArea.All
            );
            boolean elytraPriority = this.swapBinded() ? this.currentlyElytra != null && this.currentlyElytra : this.elytra.get();
            Slot higherPriority = elytraPriority ? bestElytra : bestArmor;
            if (higherPriority != null) {
                return higherPriority;
            } else {
                return elytraPriority ? bestArmor : bestElytra;
            }
        }
    }

    private boolean swapBinded() {
        KeyBind keyBind = this.chestSwap.get();
        return keyBind.value != null && keyBind.value.key >= 0;
    }

    private Slot findCleaner() {
        Slot basicCleaner = this.findSlot(slot -> this.cleanerItems.get().contains(slot.getStack().getItem()), null, FindArea.Both);
        if (basicCleaner != null) {
            return basicCleaner;
        } else {
            if (this.badArmor.get()) {
                for (EquipmentSlot equipmentSlot : OLEPOSSUtils.equipmentSlots) {
                    Slot badArmor = this.findBadItem(
                            slot -> slot.getStack().getItem() instanceof ArmorItem armorItem && armorItem.getSlotType() == equipmentSlot,
                            slot -> ItemUtils.getArmorValue(slot.getStack()),
                            FindArea.All
                    );
                    if (badArmor != null) {
                        return badArmor;
                    }
                }
            }

            if (this.badSwords.get()) {
                Slot badSword = this.findBadItem(
                        slot -> slot.getStack().getItem() instanceof SwordItem, slot -> ItemUtils.getWeaponValue(slot.getStack()), FindArea.Both
                );
                if (badSword != null) {
                    return badSword;
                }
            }

            if (this.badAxes.get()) {
                Slot badAxe = this.findBadItem(
                        slot -> slot.getStack().getItem() instanceof AxeItem, this.axeComparing.get().function, FindArea.Both
                );
                if (badAxe != null) {
                    return badAxe;
                }
            }

            if (this.badPickaxes.get()) {
                Slot badPickaxe = this.findBadItem(
                        slot -> slot.getStack().getItem() instanceof PickaxeItem, slot -> ItemUtils.getPickaxeValue(slot.getStack()), FindArea.Both
                );
                if (badPickaxe != null) {
                    return badPickaxe;
                }
            }

            return this.badBows.get()
                    ? this.findBadItem(
                    slot -> slot.getStack().getItem() instanceof BowItem, slot -> ItemUtils.getBowValue(slot.getStack()), FindArea.Both
            )
                    : null;
        }
    }

    private Slot findBadItem(Predicate<Slot> predicate, ToDoubleFunction<Slot> value, FindArea area) {
        Slot best = this.findSlot(predicate, value, area);
        return best == null ? null : this.findSlot(slot -> predicate.test(slot) && slot != best, slot -> -value.applyAsDouble(slot), area);
    }

    private Slot findSlot(Predicate<Slot> predicate, ToDoubleFunction<Slot> value, FindArea area) {
        boolean best = value != null;
        List<Slot> valid = best ? new ArrayList<>() : null;

        for (int i = area.start; i <= area.end; i++) {
            Slot slot = BlackOut.mc.player.currentScreenHandler.getSlot(i);
            if (predicate.test(slot)) {
                if (!best) {
                    return slot;
                }

                valid.add(slot);
            }
        }

        return best ? valid.stream().max(Comparator.comparingDouble(value)).orElse(null) : null;
    }

    private boolean inCombat() {
        return this.pauseCombat.get() && System.currentTimeMillis() - this.prevDamage < 1000L;
    }

    public enum AxeCompareMode {
        Damage(slot -> ItemUtils.getWeaponValue(slot.getStack())),
        Efficiency(slot -> ItemUtils.getAxeValue(slot.getStack()));

        private final ToDoubleFunction<Slot> function;

        AxeCompareMode(ToDoubleFunction<Slot> function) {
            this.function = function;
        }
    }

    public enum FindArea {
        Armor(5, 8),
        Hotbar(36, 44),
        Inventory(9, 35),
        Both(9, 44),
        All(5, 44);

        private final int start;
        private final int end;

        FindArea(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    public enum WeaponMode {
        Sword(slot -> slot.getStack().getItem() instanceof SwordItem),
        Axe(slot -> slot.getStack().getItem() instanceof AxeItem),
        Both(slot -> {
            Item item = slot.getStack().getItem();
            return item instanceof SwordItem || item instanceof AxeItem;
        });

        private final HotbarSearch search;

        WeaponMode(Predicate<Slot> predicate) {
            this.search = new HotbarSearch(predicate, slot -> ItemUtils.getWeaponValue(slot.getStack()));
        }
    }

    private record HotbarSearch(Predicate<Slot> predicate, ToDoubleFunction<Slot> function) {
    }

    private static class Action {
    }

    private static class Drop extends Action {
        private final DoubleConsumer<ClientPlayerInteractionManager, ScreenHandler> consumer;

        private Drop(int id) {
            this.consumer = (manager, handler) -> manager.clickSlot(handler.syncId, id, 1, SlotActionType.THROW, BlackOut.mc.player);
        }
    }

    private static class Move extends Action {
        private final int from;
        private final int to;
        private final Predicate<ItemStack> predicate;

        private Move(int from, int to) {
            this.from = from;
            this.to = to;
            ItemStack copy = BlackOut.mc.player.currentScreenHandler.getSlot(from).getStack().copy();
            this.predicate = stack -> ItemStack.areItemsEqual(stack, copy);
        }
    }

    private static class QuickMove extends Action {
        private final DoubleConsumer<ClientPlayerInteractionManager, ScreenHandler> consumer;

        private QuickMove(int id) {
            this.consumer = (manager, handler) -> manager.clickSlot(handler.syncId, id, 0, SlotActionType.QUICK_MOVE, BlackOut.mc.player);
        }
    }

    private static class ReplenishSlot {
        private Item item = null;
        private long lastSeen = 0L;
    }

    private static class Swap extends Action {
        private final DoubleConsumer<ClientPlayerInteractionManager, ScreenHandler> consumer;

        private Swap(int id, int slotId) {
            this.consumer = (manager, handler) -> manager.clickSlot(handler.syncId, id, slotId, SlotActionType.SWAP, BlackOut.mc.player);
        }
    }
}
