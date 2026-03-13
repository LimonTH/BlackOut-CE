package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RotationType;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.misc.Simulation;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.Pair;
import bodevelopment.client.blackout.randomstuff.timers.TickTimerList;
import bodevelopment.client.blackout.util.InvUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

public class Quiver extends Module {
    public static boolean charging = false;
    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Integer> charge = this.sgGeneral.intSetting("Charge Ticks", 5, 0, 20, 1,
            "How many ticks to draw the bow. 3-5 is usually enough for self-buffing.");
    public final Setting<Integer> delay = this.sgGeneral.intSetting("Shot Delay", 0, 0, 20, 1,
            "Ticks to wait before starting the next charge cycle.");
    public final Setting<Boolean> closeInv = this.sgGeneral.booleanSetting("Auto Close", true,
            "Automatically closes your inventory after rearranging arrows.");
    public final Setting<Boolean> instantMove = this.sgGeneral.booleanSetting("Instant Move", true,
            "Moves arrows using packets for near-instant execution.");
    public final Setting<Double> moveSpeed = this.sgGeneral.doubleSetting("Move Speed", 20.0, 0.0, 20.0, 0.2,
            "The speed of inventory interactions if Instant Move is disabled.", () -> !this.instantMove.get());
    public final Setting<Integer> durationLeft = this.sgGeneral.intSetting("Min Duration", 5, 0, 60, 1,
            "Won't shoot if the current effect lasts longer than this many seconds.");
    public final Setting<Integer> retryTime = this.sgGeneral.intSetting("Retry Cooldown", 50, 0, 100, 1,
            "Ticks to wait before retrying a failed effect application.");
    private final Setting<Boolean> instantRotate = this.sgGeneral.booleanSetting("Instant Rotate", true,
            "Forces the pitch to -90 instantly for the shot.");

    private final List<Pair<MobEffectInstance, Integer>> arrows = new ArrayList<>();
    private final List<Integer> actions = new ArrayList<>();
    private final TickTimerList<MobEffect> shot = new TickTimerList<>(false);
    private MobEffect currentEffect = null;
    private int timer = 0;
    private boolean charged = false;
    private double movesLeft = 0.0;

    public Quiver() {
        super("Quiver", "Automatically shoots positive effect arrows at yourself by looking straight up.", SubCategory.MISC, true);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        this.timer++;
        this.movesLeft = this.movesLeft + this.moveSpeed.get() / 20.0;
        this.shot.update();
        if (BlackOut.mc.player != null && BlackOut.mc.level != null && BlackOut.mc.player.getMainHandItem().getItem() == Items.BOW) {
            this.update();
        } else {
            charging = false;
        }

        this.movesLeft = Math.min(this.movesLeft, 1.0);
    }

    private void update() {
        this.updateArrows();
        this.updateShooting();
    }

    private void updateShooting() {
        ItemStack stack = BlackOut.mc.player.getMainHandItem();
        if (stack != null && stack.getItem() instanceof BowItem) {
            if (!this.updateMoving()) {
                if (this.charged) {
                    this.charged = false;
                    BlackOut.mc.gameMode.releaseUsingItem(BlackOut.mc.player);
                }
            } else if (this.rotatePitch(-90.0F, RotationType.Other.withInstant(this.instantRotate.get()), "bow")) {
                if (!BlackOut.mc.player.isUsingItem() && this.timer > this.delay.get()) {
                    BlackOut.mc.gameMode.useItem(BlackOut.mc.player, InteractionHand.MAIN_HAND);
                    charging = true;
                    this.charged = true;
                }

                if (BlackOut.mc.player.getTicksUsingItem() >= this.charge.get()) {
                    BlackOut.mc.gameMode.releaseUsingItem(BlackOut.mc.player);
                    this.predict(this.getSlot(true));
                    this.shot.add(this.currentEffect, this.retryTime.get());
                    charging = false;
                    this.timer = 0;
                    this.charged = false;
                }
            }
        }
    }

    private void predict(int slot) {
        if (slot >= 0 && Simulation.getInstance().quiverShoot()) {
            AbstractContainerMenu handler = BlackOut.mc.player.containerMenu;
            ItemStack stack = handler.getSlot(slot).getItem().copy();
            if (stack.getCount() > 1) {
                stack.setCount(stack.getCount() - 1);
            } else {
                stack = Items.AIR.getDefaultInstance();
            }

            Managers.PACKET.preApply(new ClientboundContainerSetSlotPacket(handler.containerId, handler.getStateId(), slot, stack));
        }
    }

    private boolean updateMoving() {
        if (BlackOut.mc.player.containerMenu instanceof InventoryMenu && !this.actions.isEmpty()) {
            while (this.movesLeft > 0.0 && !this.actions.isEmpty()) {
                this.click(this.actions.getFirst());
                this.actions.removeFirst();
                this.movesLeft--;
            }
            if (this.actions.isEmpty() && this.closeInv.get() && BlackOut.mc.player.containerMenu instanceof InventoryMenu) {
                this.closeInventory();
            }
        } else {
            this.actions.clear();
        }

        int toShoot = this.getSlot(false);
        int all = this.getSlot(true);
        if (toShoot < 0) {
            return false;
        } else {
            if (toShoot != all) {
                if (!this.actions.isEmpty()) {
                    return false;
                }

                if (!(BlackOut.mc.player.containerMenu instanceof InventoryMenu)) {
                    return false;
                }

                this.move(toShoot);
                this.move(all);
                this.move(toShoot);
                if (this.closeInv.get() && this.instantMove.get() && BlackOut.mc.player.containerMenu instanceof InventoryMenu) {
                    this.closeInventory();
                }
            }

            return true;
        }
    }

    private void move(int slot) {
        if (this.instantMove.get()) {
            this.click(slot);
        } else {
            this.actions.add(slot);
        }
    }

    private void click(int slot) {
        InvUtils.interactSlot(0, slot, 0, ClickType.PICKUP);
    }

    private int getSlot(boolean first) {
        int slot = -1;

        for (Pair<MobEffectInstance, Integer> entry : this.arrows) {
            MobEffectInstance effect = entry.getA();
            int s = entry.getB();
            if (s <= slot || slot <= -1) {
                if (!first) {
                    MobEffectInstance activeEffect = BlackOut.mc.player.getActiveEffectsMap().get(effect.getEffect());
                    if (BlackOut.mc.player.hasEffect(effect.getEffect())
                            && activeEffect != null && activeEffect.getDuration() > this.durationLeft.get() * 20
                            || this.shot.contains(effect.getEffect().value())
                            || !effect.getEffect().value().isBeneficial()) {
                        continue;
                    }

                    this.currentEffect = effect.getEffect().value();
                }

                slot = s;
            }
        }

        return slot;
    }

    private void updateArrows() {
        this.arrows.clear();
        if (BlackOut.mc.player.containerMenu instanceof InventoryMenu handler) {
            for (int slotIndex = 0; slotIndex < handler.slots.size(); slotIndex++) {
                ItemStack stack = handler.getSlot(slotIndex).getItem();
                if (!stack.isEmpty() && stack.getItem() instanceof ArrowItem) {
                    PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);

                    if (contents != null) {
                        Iterable<MobEffectInstance> effects = contents.getAllEffects();
                        var iterator = effects.iterator();
                        if (iterator.hasNext()) {
                            this.arrows.add(new Pair<>(iterator.next(), slotIndex));
                        }
                    }
                }
            }
        }
    }
}
