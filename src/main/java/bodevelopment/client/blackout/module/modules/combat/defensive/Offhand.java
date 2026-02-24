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
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class Offhand extends Module {
    private final SettingGroup sgItem = this.addGroup("Item");
    private final SettingGroup sgSwitch = this.addGroup("Switch");
    private final SettingGroup sgHealth = this.addGroup("Health");
    private final SettingGroup sgThreading = this.addGroup("Threading");

    private final Setting<TotemMode> totemMode = this.sgItem.enumSetting("Totem Mode", TotemMode.Always,
            "When to prioritize holding a Totem of Undying.");
    private final Setting<ItemMode> primary = this.sgItem.enumSetting("Primary", ItemMode.Crystal,
            "Default item to hold in offhand when you are safe.", () -> this.totemMode.get() != TotemMode.Always);
    private final Setting<ItemMode> secondary = this.sgItem.enumSetting("Secondary", ItemMode.Gapple,
            "Fall-back item if the primary item is out of stock.", () -> this.totemMode.get() != TotemMode.Always && this.primary.get() != ItemMode.Nothing);
    private final Setting<Boolean> swordGapple = this.sgItem.booleanSetting("Sword Gapple", true,
            "Automatically switches to Gapples when you hold a sword and press Use.");
    private final Setting<Boolean> safeSwordGapple = this.sgItem.booleanSetting("Safe Sword Gapple", true,
            "Forces a Totem instead of a Gapple if you are in danger, even while holding a sword.", () -> this.swordGapple.get() && this.totemMode.get() != TotemMode.Never);

    private final Setting<Boolean> onlyInInventory = this.sgSwitch.booleanSetting("Only In Inventory", false,
            "Only perform offhand switches while the inventory screen is open.");
    private final Setting<SwitchMode> switchMode = this.sgSwitch.enumSetting("Switch Mode", SwitchMode.FClick,
            "The packet/click method for swapping items. FClick is usually the fastest.");
    private final Setting<Double> cooldown = this.sgSwitch.doubleSetting("Cooldown", 0.2, 0.0, 1.0, 0.01,
            "Delay between offhand swaps to prevent inventory desync/ghost items.");

    private final Setting<Integer> latency = this.sgHealth.intSetting("Latency", 0, 0, 10, 1,
            "Compensates for server lag by checking your previous positions for damage.");
    private final Setting<Boolean> prediction = this.sgHealth.booleanSetting("Prediction", true,
            "Calculates potential damage based on your predicted movement.");
    private final Setting<Integer> hp = this.sgHealth.intSetting("Totem Health", 14, 0, 36, 1,
            "Health threshold to switch to a Totem when outside of a hole.");
    private final Setting<Integer> safeHealth = this.sgHealth.intSetting("Safe Health", 18, 0, 36, 1,
            "Stays on Totem until your health rises above this level for safety.");
    private final Setting<Boolean> mineCheck = this.sgHealth.booleanSetting("Mine Check", true,
            "Treats your hole as unsafe if someone is currently mining its walls.");
    private final Setting<Double> miningTime = this.sgHealth.doubleSetting("Mining Time", 4.0, 0.0, 10.0, 0.1,
            "How long to keep the 'Danger' status after a block starts being mined.");
    private final Setting<Integer> holeHp = this.sgHealth.intSetting("Hole Health", 10, 0, 36, 1,
            "Health threshold for Totem while safely inside a hole (usually lower than Totem Health).");
    private final Setting<Integer> holeSafeHp = this.sgHealth.intSetting("Hole Safe Health", 14, 0, 36, 1,
            "Recovery threshold while in a hole.");
    private final Setting<Double> safetyMultiplier = this.sgHealth.doubleSetting("Safety Multiplier", 1.0, 0.0, 5.0, 0.05,
            "Multiplies calculated crystal damage for extra safety padding.");

    private final Setting<Boolean> render = this.sgThreading.booleanSetting("Render", true, "Update offhand during world rendering.");
    private final Setting<Boolean> tickPre = this.sgThreading.booleanSetting("Tick Pre", true, "Update offhand during tick start.");
    private final Setting<Boolean> tickPost = this.sgThreading.booleanSetting("Tick Post", true, "Update offhand during tick end.");
    private final Setting<Boolean> move = this.sgThreading.booleanSetting("Move", true, "Update offhand when moving.");
    private final Setting<Boolean> crystalSpawn = this.sgThreading.booleanSetting("Crystal Spawn", true, "Instant update when a crystal spawns.");

    private final TimerMap<Integer, BlockPos> mining = new TimerMap<>(true);
    private final List<Box> prevPositions = new ArrayList<>();
    private final Predicate<ItemStack> totemPredicate = stack -> stack.isOf(Items.TOTEM_OF_UNDYING);
    private final TimerMap<Integer, Long> movedFrom = new TimerMap<>(true);
    private long prevSwitch = 0L;

    public Offhand() {
        super("Offhand", "Manages totems and items with advanced damage prediction.", SubCategory.DEFENSIVE, true);
    }

    @Event
    public void onReceive(PacketEvent.Receive.Pre event) {
        if (BlackOut.mc.world != null && event.packet instanceof BlockBreakingProgressS2CPacket packet && BlockUtils.mineable(packet.getPos())) {
            this.mining.remove((id, timer) -> id == packet.getEntityId());
            this.mining.add(packet.getEntityId(), packet.getPos(), this.miningTime.get());
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
        OLEPOSSUtils.limitList(this.prevPositions, this.latency.get());
        if (this.move.get()) {
            this.update();
        }
    }

    @Event
    public void onEntity(EntityAddEvent.Post event) {
        if (event.entity instanceof EndCrystalEntity && this.crystalSpawn.get()) {
            this.update();
        }
    }

    private void update() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null && BlackOut.mc.player.currentScreenHandler instanceof PlayerScreenHandler) {
            if (!this.onlyInInventory.get() || BlackOut.mc.currentScreen instanceof InventoryScreen) {
                Predicate<ItemStack> predicate = this.getItem();
                if (predicate != null) {
                    if (!predicate.test(BlackOut.mc.player.getOffHandStack())) {
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
                        this.clickSlot(45, 0, SlotActionType.PICKUP);
                    } else {
                        Slot slotxx = this.find(predicate);
                        if (slotxx != null) {
                            this.clickSlot(slotxx.id, 0, SlotActionType.PICKUP);
                            this.clickSlot(45, 0, SlotActionType.PICKUP);
                            this.addMoveTime(slotxx);
                        }
                    }

                    if (this.anythingPicked()) {
                        Slot empty = this.findEmpty();
                        if (empty != null) {
                            this.clickSlot(empty.id, 0, SlotActionType.PICKUP);
                        }
                    }

                    this.prevSwitch = System.currentTimeMillis();
                    this.closeInventory();
                    break;
                case FClick:
                    Slot slotx = this.find(predicate);
                    if (slotx != null) {
                        this.clickSlot(slotx.id, 40, SlotActionType.SWAP);
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
                        int selectedSlot = BlackOut.mc.player.getInventory().selectedSlot;
                        InvUtils.pickSwap(slot.getIndex());
                        this.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
                        InvUtils.swap(selectedSlot);
                        this.prevSwitch = System.currentTimeMillis();
                        this.addMoveTime(slot);
                    }
            }
        }
    }

    private void addMoveTime(Slot slot) {
        this.movedFrom.removeKey(slot.id);
        this.movedFrom.add(slot.id, System.currentTimeMillis(), 5.0);
    }

    private Slot findEmpty() {
        for (int i = 9; i < 45; i++) {
            Slot slot = BlackOut.mc.player.currentScreenHandler.getSlot(i);
            if (slot.getStack().isEmpty()) {
                return slot;
            }
        }

        return null;
    }

    private Slot find(Predicate<ItemStack> predicate) {
        List<Slot> possible = new ArrayList<>();

        for (Slot slot : BlackOut.mc.player.currentScreenHandler.slots) {
            if (predicate.test(slot.getStack())) {
                possible.add(slot);
            }
        }

        Optional<Slot> optional = possible.stream()
                .min(Comparator.comparingLong(slotx -> this.movedFrom.containsKey(slotx.id) ? this.movedFrom.get(slotx.id) : 0L));
        return optional.orElse(null);
    }

    private void clickSlot(int id, int button, SlotActionType actionType) {
        ScreenHandler handler = BlackOut.mc.player.currentScreenHandler;
        BlackOut.mc.interactionManager.clickSlot(handler.syncId, id, button, actionType, BlackOut.mc.player);
    }

    private boolean isPicked(Predicate<ItemStack> predicate) {
        return predicate.test(BlackOut.mc.player.currentScreenHandler.getCursorStack());
    }

    private boolean anythingPicked() {
        return !BlackOut.mc.player.currentScreenHandler.getCursorStack().isEmpty();
    }

    private Predicate<ItemStack> getItem() {
        boolean shouldSG = this.swordGapple.get()
                && BlackOut.mc.options.useKey.isPressed()
                && BlackOut.mc.player.getMainHandStack().getItem() instanceof SwordItem;

        if (shouldSG && (!this.safeSwordGapple.get() || !this.inDanger())) {
            return ItemMode.Gapple.predicate;
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
        if (Suicide.getInstance().enabled && Suicide.getInstance().offHand.get()) {
            return false;
        } else {
            double health = BlackOut.mc.player.getHealth() + BlackOut.mc.player.getAbsorptionAmount();
            if (health <= this.getHealth()) {
                return true;
            } else {
                for (Box box : this.prevPositions) {
                    if (this.inDanger(box, health)) {
                        return true;
                    }
                }

                return this.inDanger(BlackOut.mc.player.getBoundingBox(), health) || this.prediction.get() && this.inDanger(this.predictedBox(), health);
            }
        }
    }

    private Box predictedBox() {
        Vec3d pos = MovementPrediction.predict(BlackOut.mc.player);
        double lx = BlackOut.mc.player.getBoundingBox().getLengthX();
        double lz = BlackOut.mc.player.getBoundingBox().getLengthZ();
        double height = BlackOut.mc.player.getBoundingBox().getLengthY();
        return new Box(
                pos.getX() - lx / 2.0,
                pos.getY(),
                pos.getZ() - lz / 2.0,
                pos.getX() + lx / 2.0,
                pos.getY() + height,
                pos.getZ() + lz / 2.0
        );
    }

    private boolean inDanger(Box box, double health) {
        for (Entity entity : BlackOut.mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity
                    && DamageUtils.crystalDamage(BlackOut.mc.player, box, entity.getPos()) * this.safetyMultiplier.get() >= health) {
                return true;
            }
        }

        return false;
    }

    private double getHealth() {
        boolean holdingTot = BlackOut.mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
        return this.isInHole() ? (holdingTot ? this.holeSafeHp : this.holeHp).get() : (holdingTot ? this.safeHealth : this.hp).get().intValue();
    }

    private boolean isInHole() {
        for (Box box : this.prevPositions) {
            if (!this.isInHole(BoxUtils.feet(box))) {
                return false;
            }
        }

        return true;
    }

    private boolean isInHole(Vec3d feet) {
        Hole hole = HoleUtils.currentHole(BlockPos.ofFloored(feet.add(0.0, 0.5, 0.0)));
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
        Crystal(stack -> stack.isOf(Items.END_CRYSTAL)),
        Exp(stack -> stack.isOf(Items.EXPERIENCE_BOTTLE)),
        Gapple(OLEPOSSUtils::isGapple),
        Bed(OLEPOSSUtils::isBed),
        Obsidian(stack -> stack.isOf(Items.OBSIDIAN));

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
