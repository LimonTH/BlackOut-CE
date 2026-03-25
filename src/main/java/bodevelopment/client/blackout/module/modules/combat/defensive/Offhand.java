package bodevelopment.client.blackout.module.modules.combat.defensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.*;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.misc.Suicide;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.Hole;
import bodevelopment.client.blackout.randomstuff.timers.TimerMap;
import bodevelopment.client.blackout.util.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Offhand extends Module {
    private final SettingGroup sgItem = this.addGroup("Item");
    private final SettingGroup sgSwitch = this.addGroup("Switch");
    private final SettingGroup sgHealth = this.addGroup("Health");
    private final SettingGroup sgThreading = this.addGroup("Threading");

    private final Setting<TotemMode> totemMode = this.sgItem.enumSetting("Totem Mode", TotemMode.Always, "When to prioritize holding a Totem of Undying.");
    private final Setting<ItemMode> primary = this.sgItem.enumSetting("Primary", ItemMode.Crystal, "Default item to hold in offhand when you are safe.", () -> this.totemMode.get() != TotemMode.Always);
    private final Setting<ItemMode> secondary = this.sgItem.enumSetting("Secondary", ItemMode.Gapple, "Fall-back item if the primary item is out of stock.", () -> this.totemMode.get() != TotemMode.Always && this.primary.get() != ItemMode.Nothing);
    private final Setting<Boolean> swordGapple = this.sgItem.booleanSetting("Sword Gapple", true, "Automatically switches to Gapples when you hold a sword and press Use.");
    private final Setting<Boolean> safeSwordGapple = this.sgItem.booleanSetting("Safe Sword Gapple", true, "Forces a Totem instead of a Gapple if you are in danger, even while holding a sword.", () -> this.swordGapple.get() && this.totemMode.get() != TotemMode.Never);
    private final Setting<Integer> swordGappleHealth = this.sgItem.intSetting("Sword Gapple Health", 18, 0, 36, 1, "Health threshold for allowing Gapple when holding sword with Safe Sword Gapple enabled.", () -> this.swordGapple.get() && this.safeSwordGapple.get());

    private final Setting<Boolean> onlyInInventory = this.sgSwitch.booleanSetting("Only In Inventory", false, "Only perform offhand switches while the inventory screen is open.");
    private final Setting<SwitchMode> switchMode = this.sgSwitch.enumSetting("Switch Mode", SwitchMode.FClick, "The packet/click method for swapping items. FClick is usually the fastest.");
    private final Setting<Double> cooldown = this.sgSwitch.doubleSetting("Cooldown", 0.2, 0.0, 1.0, 0.01, "Delay between offhand swaps to prevent inventory desync/ghost items.");

    private final Setting<Integer> latency = this.sgHealth.intSetting("Latency", 0, 0, 10, 1, "Compensates for server lag by checking your previous positions for damage.");
    private final Setting<Boolean> prediction = this.sgHealth.booleanSetting("Prediction", true, "Calculates potential damage based on your predicted movement.");
    private final Setting<Integer> hp = this.sgHealth.intSetting("Totem Health", 14, 0, 36, 1, "Health threshold to switch to a Totem when outside of a hole.");
    private final Setting<Integer> safeHealth = this.sgHealth.intSetting("Safe Health", 18, 0, 36, 1, "Stays on Totem until your health rises above this level for safety.");
    private final Setting<Boolean> mineCheck = this.sgHealth.booleanSetting("Mine Check", true, "Treats your hole as unsafe if someone is currently mining its walls.");
    private final Setting<Double> miningTime = this.sgHealth.doubleSetting("Mining Time", 4.0, 0.0, 10.0, 0.1, "How long to keep the 'Danger' status after a block starts being mined.");
    private final Setting<Integer> holeHp = this.sgHealth.intSetting("Hole Health", 10, 0, 36, 1, "Health threshold for Totem while safely inside a hole (usually lower than Totem Health).");
    private final Setting<Integer> holeSafeHp = this.sgHealth.intSetting("Hole Safe Health", 14, 0, 36, 1, "Recovery threshold while in a hole.");
    private final Setting<Double> safetyMultiplier = this.sgHealth.doubleSetting("Safety Multiplier", 1.0, 0.0, 5.0, 0.05, "Multiplies calculated crystal damage for extra safety padding.");

    private final Setting<Boolean> render = this.sgThreading.booleanSetting("Render", true, "Update offhand during world rendering.");
    private final Setting<Boolean> tickPre = this.sgThreading.booleanSetting("Tick Pre", true, "Update offhand during tick start.");
    private final Setting<Boolean> tickPost = this.sgThreading.booleanSetting("Tick Post", true, "Update offhand during tick end.");
    private final Setting<Boolean> move = this.sgThreading.booleanSetting("Move", true, "Update offhand when moving.");
    private final Setting<Boolean> crystalSpawn = this.sgThreading.booleanSetting("Crystal Spawn", true, "Instant update when a crystal spawns.");

    private final Predicate<ItemStack> totemPredicate = stack -> stack.is(Items.TOTEM_OF_UNDYING);

    private final TimerMap<Integer, BlockPos> mining = new TimerMap<>(true);
    private final List<AABB> prevPositions = new ArrayList<>();
    private final TimerMap<Integer, Long> movedFrom = new TimerMap<>(true);
    private long prevSwitch = 0L;
    boolean lookingAtInteractive = false;

    public Offhand() {
        super("Offhand", "Manages totems and items with advanced damage prediction.", SubCategory.DEFENSIVE, true);
    }

    @Event
    public void onReceive(PacketEvent.Receive.Pre event) {
        if (BlackOut.mc.level != null && event.packet instanceof ClientboundBlockDestructionPacket packet && BlockUtils.mineable(packet.getPos())) {
            this.mining.remove((id, timer) -> id == packet.getId());
            this.mining.add(packet.getId(), packet.getPos(), this.miningTime.get());
        }
    }

    @Event
    public void onRender(RenderEvent.World.Pre event) {
        if (this.render.get()) {
            this.update();
        }
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (this.tickPre.get()) {
            this.update();
        }
    }

    @Event
    public void onTickPost(TickEvent.Post event) {
        if (this.tickPost.get()) {
            this.update();
        }
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        this.prevPositions.add(BlackOut.mc.player.getBoundingBox());
        CollectionUtils.limitSize(this.prevPositions, this.latency.get());
        if (this.move.get()) {
            this.update();
        }
    }

    @Event
    public void onEntity(EntityAddEvent.Post event) {
        if (event.entity instanceof EndCrystal && this.crystalSpawn.get()) {
            this.update();
        }
    }

    private void update() {
        if (PlayerUtils.isInGame() && BlackOut.mc.player.containerMenu instanceof InventoryMenu) {
            if (!this.onlyInInventory.get() || BlackOut.mc.screen instanceof InventoryScreen) {
                Predicate<ItemStack> predicate = this.getItem();
                if (predicate != null) {
                    if (!predicate.test(BlackOut.mc.player.getOffhandItem())) {
                        if (this.available(predicate)) {
                            this.doSwitch(predicate);
                        }
                    }
                }
            }
        }
    }

    private void doSwitch(Predicate<ItemStack> predicate) {
        if (!(System.currentTimeMillis() - this.prevSwitch < this.cooldown.get() * 1000.0)) {
            switch (this.switchMode.get()) {
                case Basic:
                    if (this.isPicked(predicate)) {
                        this.clickSlot(45, 0, ClickType.PICKUP);
                    } else {
                        Slot slotxx = this.find(predicate);
                        if (slotxx != null) {
                            this.clickSlot(slotxx.index, 0, ClickType.PICKUP);
                            this.clickSlot(45, 0, ClickType.PICKUP);
                            this.addMoveTime(slotxx);
                        }
                    }

                    if (this.anythingPicked()) {
                        Slot empty = this.findEmpty();
                        if (empty != null) {
                            this.clickSlot(empty.index, 0, ClickType.PICKUP);
                        }
                    }

                    this.prevSwitch = System.currentTimeMillis();
                    this.closeInventory();
                    break;
                case FClick:
                    Slot slotx = this.find(predicate);
                    if (slotx != null) {
                        this.clickSlot(slotx.index, 40, ClickType.SWAP);
                        this.prevSwitch = System.currentTimeMillis();
                        this.addMoveTime(slotx);
                    }

                    if (!this.anythingPicked()) {
                        this.closeInventory();
                    }
                    break;
                case Pick:
                    Slot slot = this.find(predicate);
                    if (slot != null) {
                        int selectedSlot = BlackOut.mc.player.getInventory().selected;
                        InvUtils.pickSwap(slot.getContainerSlot());
                        this.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
                        InvUtils.swap(selectedSlot);
                        this.prevSwitch = System.currentTimeMillis();
                        this.addMoveTime(slot);
                    }
            }
        }
    }

    private void addMoveTime(Slot slot) {
        this.movedFrom.removeKey(slot.index);
        this.movedFrom.add(slot.index, System.currentTimeMillis(), 5.0);
    }

    private Slot findEmpty() {
        for (int i = 9; i < 45; i++) {
            Slot slot = BlackOut.mc.player.containerMenu.getSlot(i);
            if (slot.getItem().isEmpty()) {
                return slot;
            }
        }

        return null;
    }

    private Slot find(Predicate<ItemStack> predicate) {
        List<Slot> possible = new ArrayList<>();

        for (Slot slot : BlackOut.mc.player.containerMenu.slots) {
            if (predicate.test(slot.getItem())) {
                possible.add(slot);
            }
        }

        Optional<Slot> optional = possible.stream()
                .max(Comparator.comparingLong(slotx -> this.movedFrom.containsKey(slotx.index) ? this.movedFrom.get(slotx.index) : 0L));
        return optional.orElse(null);
    }

    private void clickSlot(int id, int button, ClickType actionType) {
        AbstractContainerMenu handler = BlackOut.mc.player.containerMenu;
        BlackOut.mc.gameMode.handleInventoryMouseClick(handler.containerId, id, button, actionType, BlackOut.mc.player);
    }

    private boolean isPicked(Predicate<ItemStack> predicate) {
        return predicate.test(BlackOut.mc.player.containerMenu.getCarried());
    }

    private boolean anythingPicked() {
        return !BlackOut.mc.player.containerMenu.getCarried().isEmpty();
    }

    private Predicate<ItemStack> getItem() {
        lookingAtInteractive = false;

        if (BlackOut.mc.hitResult instanceof BlockHitResult bhr) {
            BlockState state = BlackOut.mc.level.getBlockState(bhr.getBlockPos());
            Block block = state.getBlock();
            if (block instanceof BaseEntityBlock || block instanceof CraftingTableBlock || block instanceof AnvilBlock) {
                lookingAtInteractive = true;
            }
        }

        boolean shouldSG = this.swordGapple.get()
                && !lookingAtInteractive
                && BlackOut.mc.options.keyUse.isDown()
                && BlackOut.mc.player.getMainHandItem().getItem() instanceof SwordItem;

        if (shouldSG) {
            boolean allowGapple = true;
            if (this.safeSwordGapple.get()) {
                double health = BlackOut.mc.player.getHealth() + BlackOut.mc.player.getAbsorptionAmount();
                if (health < this.swordGappleHealth.get() || this.isCrystalDangerous(health)) {
                    allowGapple = false;
                }
            }
            if (allowGapple && this.available(ItemMode.Gapple.predicate)) {
                return ItemMode.Gapple.predicate;
            }
        }

        TotemMode mode = this.totemMode.get();
        if (mode == TotemMode.Always || (mode == TotemMode.Danger && this.inDanger())) {
            if (this.available(this.totemPredicate)) {
                return this.totemPredicate;
            }
        }

        Predicate<ItemStack> primaryPredicate = this.primary.get().predicate;
        if (primaryPredicate != null && this.available(primaryPredicate)) {
            return primaryPredicate;
        }

        Predicate<ItemStack> secondaryPredicate = this.secondary.get().predicate;
        if (secondaryPredicate != null && this.available(secondaryPredicate)) {
            return secondaryPredicate;
        }

        return null;
    }

    private boolean available(Predicate<ItemStack> predicate) {
        return this.find(predicate) != null;
    }

    private boolean inDanger() {
        return this.isDangerous(this.getHealth());
    }

    private boolean isDangerous(double threshold) {
        if (Suicide.getInstance().enabled && Suicide.getInstance().offHand.get()) {
            return false;
        } else {
            double health = BlackOut.mc.player.getHealth() + BlackOut.mc.player.getAbsorptionAmount();
            if (health <= threshold) {
                return true;
            } else {
                for (AABB box : this.prevPositions) {
                    if (this.inDanger(box, health)) {
                        return true;
                    }
                }

                return this.inDanger(BlackOut.mc.player.getBoundingBox(), health) || this.prediction.get() && this.inDanger(this.predictedBox(), health);
            }
        }
    }

    private AABB predictedBox() {
        Vec3 pos = MovementPrediction.predict(BlackOut.mc.player);
        double lx = BlackOut.mc.player.getBoundingBox().getXsize();
        double lz = BlackOut.mc.player.getBoundingBox().getZsize();
        double height = BlackOut.mc.player.getBoundingBox().getYsize();
        return new AABB(
                pos.x() - lx / 2.0,
                pos.y(),
                pos.z() - lz / 2.0,
                pos.x() + lx / 2.0,
                pos.y() + height,
                pos.z() + lz / 2.0
        );
    }

    private boolean inDanger(AABB box, double health) {
        for (Entity entity : BlackOut.mc.level.entitiesForRendering()) {
            if (entity instanceof EndCrystal
                    && DamageUtils.crystalDamage(BlackOut.mc.player, box, entity.position()) * this.safetyMultiplier.get() >= health) {
                return true;
            }
        }

        return false;
    }

    private boolean isCrystalDangerous(double health) {
        if (Suicide.getInstance().enabled && Suicide.getInstance().offHand.get()) {
            return false;
        }
        for (AABB box : this.prevPositions) {
            if (this.inDanger(box, health)) {
                return true;
            }
        }
        if (this.inDanger(BlackOut.mc.player.getBoundingBox(), health)) {
            return true;
        }
        if (this.prediction.get() && this.inDanger(this.predictedBox(), health)) {
            return true;
        }
        return false;
    }

    private double getHealth() {
        boolean holdingTot = BlackOut.mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);
        return this.isInHole() ? (holdingTot ? this.holeSafeHp : this.holeHp).get() : (holdingTot ? this.safeHealth : this.hp).get().intValue();
    }

    private boolean isInHole() {
        for (AABB box : this.prevPositions) {
            if (!this.isInHole(BoxUtils.feet(box))) {
                return false;
            }
        }

        return true;
    }

    private boolean isInHole(Vec3 feet) {
        Hole hole = HoleUtils.currentHole(BlockPos.containing(feet.add(0.0, 0.5, 0.0)));
        if (this.mineCheck.get()) {
            for (BlockPos pos : hole.positions) {
                if (this.mining.containsValue(pos)) {
                    return false;
                }
            }
        }

        return true;
    }

    public enum ItemMode {
        Nothing(null),
        Crystal(stack -> stack.is(Items.END_CRYSTAL)),
        Exp(stack -> stack.is(Items.EXPERIENCE_BOTTLE)),
        Gapple(ItemUtils::isGapple),
        Bed(ItemUtils::isBed),
        Obsidian(stack -> stack.is(Items.OBSIDIAN));

        private final Predicate<ItemStack> predicate;

        ItemMode(Predicate<ItemStack> predicate) {
            this.predicate = predicate;
        }
    }

    public enum SwitchMode {
        Basic,
        FClick,
        Pick
    }

    public enum TotemMode {
        Always,
        Danger,
        Never
    }
}
