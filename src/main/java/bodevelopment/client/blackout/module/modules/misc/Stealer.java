package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.util.PlayerUtils;
import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.GameJoinEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BoxMultiSetting;
import bodevelopment.client.blackout.randomstuff.timers.RenderList;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import bodevelopment.client.blackout.util.BoxUtils;
import bodevelopment.client.blackout.util.ItemUtils;
import bodevelopment.client.blackout.util.SettingUtils;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class Stealer extends Module {
    private static Stealer INSTANCE;

    public final SettingGroup sgGeneral = this.addGroup("General");
    public final SettingGroup sgBest = this.addGroup("Best");
    public final SettingGroup sgRender = this.addGroup("Render");

    private final Setting<Boolean> instant = this.sgGeneral.booleanSetting("Instant Mode", false, "Transfers all items in a single tick without delay.");
    private final Setting<Double> speed = this.sgGeneral.doubleSetting("Transfer Speed", 5.0, 0.0, 20.0, 0.1, "The number of items to transfer per second.", () -> !this.instant.get());
    private final Setting<Double> openFor = this.sgGeneral.doubleSetting("Interaction Delay", 0.2, 0.0, 20.0, 0.1, "The minimum duration to keep the container open before beginning to steal.");
    private final Setting<Boolean> close = this.sgGeneral.booleanSetting("Auto Close", true, "Automatically closes the container GUI once all desired items are stolen.");
    private final Setting<Double> closeDelay = this.sgGeneral.doubleSetting("Close Delay", 0.2, 0.0, 20.0, 0.1, "The delay in seconds before closing the container after the operation is complete.");
    private final Setting<Boolean> autoOpen = this.sgGeneral.booleanSetting("Auto Open", false, "Automatically scans and opens nearby containers within interaction range.");
    private final Setting<Double> openCooldown = this.sgGeneral.doubleSetting("Open Cooldown", 0.0, 0.0, 10.0, 0.1, "The cooldown between opening different containers.");
    private final Setting<Double> retryTime = this.sgGeneral.doubleSetting("Retry Interval", 0.5, 0.0, 10.0, 0.1, "The time to wait before re-attempting to open a failed container.");
    private final Setting<Double> reopenCooldown = this.sgGeneral.doubleSetting("Blacklist Time", 30.0, 0.0, 10.0, 0.1, "The duration to wait before allowing the module to reopen the same container.");
    private final Setting<Boolean> silent = this.sgGeneral.booleanSetting("Silent Steal", false, "Allows stealing items while the GUI is visually hidden (Server-dependent).");
    private final Setting<Boolean> stopRotations = this.sgGeneral.booleanSetting("Lock Rotations", true, "Prevents look-at movements from interrupting the container interaction.");
    private final Setting<Boolean> tpDisable = this.sgGeneral.booleanSetting("Disable on Teleport", false, "Automatically disables the module when changing dimensions or teleporting.");

    private final Setting<Boolean> bestWeapon = this.sgBest.booleanSetting("Filter Best Weapon", true, "Prioritizes keeping only the highest damage weapons.");
    private final Setting<Boolean> swords = this.sgBest.booleanSetting("Include Swords", true, "Includes swords in the weapon filter.");
    private final Setting<Boolean> axes = this.sgBest.booleanSetting("Include Axes", true, "Includes axes in the weapon filter.");
    private final Setting<Boolean> bestPickaxe = this.sgBest.booleanSetting("Filter Best Pickaxe", true, "Prioritizes keeping only the most efficient pickaxe.");
    private final Setting<Boolean> tools = this.sgBest.booleanSetting("Steal All Tools", false, "Steals all tools regardless of their quality.");
    private final Setting<Boolean> chestCheck = this.sgBest.booleanSetting("Name Check (Chest)", true, "Only interacts with containers that have 'Chest' in their name.");
    private final Setting<ArmorMode> armor = this.sgBest.enumSetting("Armor Strategy", ArmorMode.Best, "Logic for selecting armor: Never, All, or Best only.");
    private final Setting<List<Item>> items = this.sgBest.itemListSetting("Priority Items", "List of specific items that should always be stolen (e.g., Gapples, blocks).", Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.STONE, Items.OAK_PLANKS, Items.SNOWBALL, Items.EGG);

    private final Setting<Double> renderTime = this.sgRender.doubleSetting("Highlight Duration", 5.0, 0.0, 10.0, 0.1, "How long the chest highlight remains visible.");
    private final Setting<Double> fadeTime = this.sgRender.doubleSetting("Fade Out Time", 3.0, 0.0, 10.0, 0.1, "The duration of the fading effect for the highlight.");
    private final BoxMultiSetting renderSetting = BoxMultiSetting.of(this.sgRender);

    private final Predicate<ItemStack> weaponPredicate = stack -> stack != null
            && (this.swords.get() && stack.getItem() instanceof SwordItem || this.axes.get() && stack.getItem() instanceof AxeItem);
    private final List<Slot> movable = new ArrayList<>();
    private final List<Slot> container = new ArrayList<>();
    private final List<Slot> inventory = new ArrayList<>();
    private final TimerList<BlockPos> opened = new TimerList<>(true);
    private final TimerList<BlockPos> retryTimers = new TimerList<>(true);
    private final RenderList<BlockPos> renderList = RenderList.getList(false);
    private final EquipmentSlot[] equipmentSlots = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private long prevOpen = 0L;
    private boolean wasOpen = false;
    private boolean isOpen = false;
    private long openStateChange = 0L;
    private int calcR;
    private BlockPos calcMiddle;
    private BlockPos bestChest;
    private Direction bestDir;
    private int progress;
    private double bestDist;
    private BlockPos prevOpened = null;
    private double movesLeft = 0.0;

    public Stealer() {
        super("Stealer", "Automatically loots containers and manages your inventory by prioritizing high-value gear.", SubCategory.MISC, true);
        INSTANCE = this;
    }

    public static Stealer getInstance() {
        return INSTANCE;
    }

    public boolean shouldNoRotate() {
        if (!this.enabled) {
            return false;
        } else if (!this.stopRotations.get()) {
            return false;
        } else if (BlackOut.mc.screen instanceof ContainerScreen screen) {
            return (!this.chestCheck.get() || screen.getTitle().getString().toLowerCase().contains("chest")) && BlackOut.mc.player.containerMenu instanceof ChestMenu;
        } else {
            return false;
        }
    }

    public boolean isSilenting() {
        if (!this.enabled) {
            return false;
        } else if (!PlayerUtils.isInGame()) {
            return false;
        } else if (!this.silent.get()) {
            return false;
        } else if (BlackOut.mc.screen instanceof ContainerScreen screen) {
            return (!this.chestCheck.get() || screen.getTitle().getString().toLowerCase().contains("chest")) && BlackOut.mc.player.containerMenu instanceof ChestMenu;
        } else {
            return false;
        }
    }

    @Event
    public void onGameJoin(GameJoinEvent event) {
        if (this.tpDisable.get()) {
            this.disable(this.getDisplayName() + " was disabled due to server change/teleport", 5, Notifications.Type.Info);
        }
    }

    @Event
    public void onRender(RenderEvent.World.Pre event) {
        if (this.autoOpen.get()) {
            this.calc(BlackOut.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
        }
    }

    @Event
    public void onRenderPost(RenderEvent.World.Post event) {
        this.renderList
                .update(
                        (pos, time, d) -> this.renderSetting
                                .render(BoxUtils.get(pos), (float) (1.0 - Math.max(time - this.renderTime.get(), 0.0) / this.fadeTime.get()), 1.0F)
                );
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (PlayerUtils.isInGame()) {
            if (!(BlackOut.mc.player.containerMenu instanceof ChestMenu handler && BlackOut.mc.screen instanceof ContainerScreen screen)) {
                this.setOpen(false);
                if (this.autoOpen.get()) {
                    this.calc(1.0F);
                    this.openUpdate();
                    this.startCalc();
                }
            } else if (this.chestCheck.get() && !screen.getTitle().getString().toLowerCase().contains("chest")) {
                this.setOpen(false);
            } else {
                if (this.prevOpened != null && this.autoOpen.get()) {
                    if (!this.wasOpen) {
                        this.opened.add(this.prevOpened, this.reopenCooldown.get());
                        this.renderList.add(this.prevOpened, this.renderTime.get() + this.fadeTime.get());
                    }

                    if (this.close.get() && !SettingUtils.inInteractRange(this.prevOpened)) {
                        BlackOut.mc.player.closeContainer();
                    }
                }

                this.setOpen(true);
                if (!(System.currentTimeMillis() - this.openStateChange < this.openFor.get() * 1000.0)) {
                    for (int i = 0; i < (this.instant.get() ? 5 : 1); i++) {
                        this.update(handler);
                        this.updateClose(handler);
                        this.updateMoving(handler);
                    }
                }
            }
        }
    }

    private void setOpen(boolean state) {
        this.wasOpen = this.isOpen;
        this.isOpen = state;
        if (this.wasOpen != state) {
            this.openStateChange = System.currentTimeMillis();
        }
    }

    private void calc(float tickDelta) {
        if (this.calcMiddle != null) {
            int d = this.calcR * 2 + 1;
            int target = d * d * d;

            for (int i = this.progress; i < target * tickDelta; i++) {
                this.progress = i;
                int x = i % d - this.calcR;
                int y = i / d % d - this.calcR;
                int z = i / d / d % d - this.calcR;
                BlockPos pos = this.calcMiddle.offset(x, y, z);
                this.calcPos(pos);
            }
        }
    }

    private void calcPos(BlockPos pos) {
        BlockState state = BlackOut.mc.level.getBlockState(pos);
        if (state.getBlock() == Blocks.CHEST) {
            if (!this.opened.contains(pos)) {
                if (!this.retryTimers.contains(pos)) {
                    if (SettingUtils.inInteractRange(pos)) {
                        Direction dir = SettingUtils.getPlaceOnDirection(pos);
                        if (dir != null) {
                            double dist = BlackOut.mc.player.getEyePosition().distanceToSqr(pos.getCenter());
                            if (!(dist > this.bestDist)) {
                                this.bestDist = dist;
                                this.bestChest = pos;
                                this.bestDir = dir;
                            }
                        }
                    }
                }
            }
        }
    }

    private void openUpdate() {
        if (!(System.currentTimeMillis() - this.prevOpen < this.openCooldown.get() * 1000.0)) {
            if (!(System.currentTimeMillis() - this.openStateChange < this.closeDelay.get() * 1000.0)) {
                if (this.bestChest != null) {
                    if (!SettingUtils.shouldRotate(RotationType.Interact) || this.rotation.rotateBlock(this.bestChest, this.bestDir, RotationType.Interact, "open")) {
                        this.interactBlock(InteractionHand.MAIN_HAND, this.bestChest.getCenter(), this.bestDir, this.bestChest);
                        this.retryTimers.add(this.bestChest, this.retryTime.get());
                        this.prevOpened = this.bestChest;
                        this.prevOpen = System.currentTimeMillis();
                    }
                }
            }
        }
    }

    private void startCalc() {
        this.calcMiddle = BlockPos.containing(BlackOut.mc.player.getEyePosition());
        this.calcR = (int) Math.ceil(SettingUtils.maxInteractRange());
        this.progress = 0;
        this.bestDist = Double.MAX_VALUE;
        this.bestChest = null;
        this.bestDir = null;
    }

    private void updateClose(ChestMenu handler) {
        if (this.close.get() && this.movable.isEmpty()) {
            BlackOut.mc.player.closeContainer();
        }
    }

    private void updateMoving(ChestMenu handler) {
        this.movesLeft = this.movesLeft + this.speed.get() / 20.0;

        while ((this.movesLeft > 0.0 || this.instant.get()) && !this.movable.isEmpty()) {
            this.move(handler, this.movable.removeFirst());
        }

        this.movesLeft = Math.min(this.movesLeft, 1.0);
    }

    private void move(ChestMenu handler, Slot slot) {
        BlackOut.mc.gameMode.handleInventoryMouseClick(handler.containerId, slot.index, 0, ClickType.QUICK_MOVE, BlackOut.mc.player);
        this.movesLeft--;
    }

    private void update(ChestMenu handler) {
        this.movable.clear();
        this.getSlots(this.container, handler, true, 0, handler.getRowCount() * 9);
        this.getSlots(this.inventory, handler, false, handler.getRowCount() * 9, handler.getItems().size());
        if (this.bestWeapon.get()) {
            Slot bestWeapon = this.getBestWeapon();
            if (bestWeapon != null) {
                if (bestWeapon.container) {
                    this.movable.add(bestWeapon);
                }

                this.dump(slot -> slot != bestWeapon && this.weaponPredicate.test(slot.stack));
            }
        }

        if (this.bestPickaxe.get()) {
            Slot bestPickaxe = this.getBestPickaxe();
            if (bestPickaxe != null) {
                if (bestPickaxe.container) {
                    this.movable.add(bestPickaxe);
                }

                this.dump(slot -> slot != bestPickaxe && slot.stack.getItem() instanceof PickaxeItem);
            }
        }

        if (!this.tools.get()) {
            this.dump(slot -> {
                ItemStack stack = slot.stack;
                boolean isTool = stack.has(DataComponents.TOOL);

                return isTool
                        && !this.weaponPredicate.test(stack)
                        && !(stack.getItem() instanceof PickaxeItem);
            });
        } else {
            this.steal(slot -> {
                ItemStack stack = slot.stack;
                boolean isTool = stack.has(DataComponents.TOOL);

                return isTool
                        && !this.weaponPredicate.test(stack)
                        && !(stack.getItem() instanceof PickaxeItem);
            });
        }

        switch (this.armor.get()) {
            case Never:
                this.dump(slot -> slot.stack.getItem() instanceof ArmorItem);
                break;
            case All:
                this.steal(slot -> slot.stack.getItem() instanceof ArmorItem);
                break;
            case Best:
                List<Slot> list = new ArrayList<>();
                list.addAll(this.inventory);
                list.addAll(this.container);

                for (EquipmentSlot equipmentSlot : this.equipmentSlots) {
                    Slot best = this.getBestArmor(equipmentSlot, list);
                    if (best != null) {
                        if (best.container) {
                            this.movable.add(best);
                        }

                        this.dump(slot -> {
                            if (slot == best) return false;

                            ItemStack stack = slot.stack;
                            Equippable equippable = stack.get(DataComponents.EQUIPPABLE);

                            return equippable != null && equippable.slot() == equipmentSlot;
                        });
                    }
                }
        }

        this.steal(slot -> this.items.get().contains(slot.stack.getItem()));
        this.dump(slot -> {
            if (slot.stack().isEmpty()) {
                return false;
            } else if (!this.bestPickaxe.get() && slot.stack.getItem() instanceof DiggerItem) {
                return false;
            } else if (slot.stack.getItem() instanceof ArmorItem) {
                return false;
            } else {
                return (!this.bestWeapon.get() || !this.weaponPredicate.test(slot.stack)) && !this.items.get().contains(slot.stack.getItem());
            }
        });
    }

    private Slot getBestArmor(EquipmentSlot equipmentSlot, List<Slot> list) {
        Slot bestSlot = new Slot(false, -1, BlackOut.mc.player.getInventory().getArmor(equipmentSlot.getIndex()));
        double bestValue = ItemUtils.getArmorValue(bestSlot.stack);

        for (Slot slot : list.stream()
                .filter(slotx -> {
                    ItemStack stack = slotx.stack;
                    Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
                    return equippable != null && equippable.slot() == equipmentSlot;
                })
                .toList()) {
            double value = ItemUtils.getArmorValue(slot.stack);
            if (!(bestValue >= value)) {
                bestSlot = slot;
                bestValue = value;
            }
        }

        return bestSlot;
    }

    private void steal(Predicate<Slot> predicate) {
        this.container.stream().filter(predicate).forEach(this.movable::add);
    }

    private void dump(Predicate<Slot> predicate) {
        this.inventory.stream().filter(predicate).forEach(this.movable::add);
    }

    private Slot getBestWeapon() {
        Slot bestSlot = null;
        double bestValue = 0.0;
        List<Slot> list = new ArrayList<>();
        list.addAll(this.inventory);
        list.addAll(this.container);

        for (Slot slot : list.stream().filter(slotx -> this.weaponPredicate.test(slotx.stack)).toList()) {
            double value = ItemUtils.getWeaponValue(slot.stack);
            if (!(bestValue >= value)) {
                bestSlot = slot;
                bestValue = value;
            }
        }

        return bestSlot;
    }

    private Slot getBestPickaxe() {
        Slot bestSlot = null;
        double bestValue = 0.0;
        List<Slot> list = new ArrayList<>();
        list.addAll(this.inventory);
        list.addAll(this.container);

        for (Slot slot : list.stream().filter(slotx -> slotx.stack.getItem() instanceof PickaxeItem).toList()) {
            double value = ItemUtils.getPickaxeValue(slot.stack);
            if (!(bestValue >= value)) {
                bestSlot = slot;
                bestValue = value;
            }
        }

        return bestSlot;
    }

    private void getSlots(List<Slot> slots, ChestMenu handler, boolean container, int start, int end) {
        slots.clear();

        for (ItemStack stack : handler.getItems().subList(start, end)) {
            slots.add(new Slot(container, start++, stack));
        }
    }

    public enum ArmorMode {
        Never,
        All,
        Best
    }

    record Slot(boolean container, int index, ItemStack stack) {
    }
}
