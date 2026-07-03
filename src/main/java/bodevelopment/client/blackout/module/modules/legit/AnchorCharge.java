/*
 * Blackout Client (CE) - A cutting-edge, feature-rich cheat client for Minecraft.
 * A modernized continuation of the original Blackout project by OLEPOSSU & KassuK.
 * Copyright (C) 2026  LimonTH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

package bodevelopment.client.blackout.module.modules.legit;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.SwingHand;
import bodevelopment.client.blackout.enums.SwitchMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.BlockStateEvent;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.FindResult;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import bodevelopment.client.blackout.randomstuff.timers.TimerMap;
import bodevelopment.client.blackout.util.InvUtils;
import bodevelopment.client.blackout.util.PlayerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class AnchorCharge extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<SwitchMode> glowstoneSwitch = this.sgGeneral.enumSetting("Glowstone Switch", SwitchMode.Normal, "The method used to select glowstone in the hotbar.");
    private final Setting<Boolean> fullCharge = this.sgGeneral.booleanSetting("Maximum Charge", false, "Ensures the anchor is filled with 4 glowstone before attempting to detonate.");
    private final Setting<Boolean> allowOffhand = this.sgGeneral.booleanSetting("Allow Offhand", true, "Enables detonation using the offhand (non-vanilla behavior).");
    private final Setting<SwitchMode> explodeSwitch = this.sgGeneral.enumSetting("Explode Switch", SwitchMode.Normal, "The method used to switch to a non-interactive item for detonation.");
    private final Setting<Double> speed = this.sgGeneral.doubleSetting("Action Frequency", 4.0, 0.0, 20.0, 0.1, "The maximum number of interactions performed per second.");
    private final Setting<Boolean> onlyOwn = this.sgGeneral.booleanSetting("Owned Only", true, "Only interacts with anchors placed by the user.");
    private final Setting<Double> ownTime = this.sgGeneral.doubleSetting("Ownership Duration", 2.0, 0.0, 10.0, 0.1, "The time in seconds an anchor is considered 'owned' after placement.");

    private final TimerList<BlockPos> own = new TimerList<>(false);
    private final TimerMap<BlockPos, Integer> charges = new TimerMap<>(false);
    private final Map<BlockPos, BlockState> realStates = new ConcurrentHashMap<>();
    private final Predicate<ItemStack> emptyPredicate = stack -> {
        if (stack != null && !stack.isEmpty()) {
            return stack.has(DataComponents.TOOL);
        } else {
            return true;
        }
    };
    private double actions = 0.0;
    private int prevAnchor = -1;

    public AnchorCharge() {
        super("Anchor Charge", "Automates the process of charging Respawn Anchors with glowstone and triggering their explosion.", SubCategory.LEGIT, true);
    }

    @Event
    public void onRender(RenderEvent.World.Pre event) {
        this.actions = this.actions + event.frameTime * this.speed.get();
        this.actions = Math.min(this.actions, 1.0);
        this.realStates.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            if (this.charges.containsKey(pos)) {
                return false;
            } else {
                BlackOut.mc.level.setBlockAndUpdate(pos, entry.getValue());
                return true;
            }
        });
    }

    @Event
    public void onState(BlockStateEvent event) {
        this.charges.update();
        if (this.charges.containsKey(event.pos)) {
            if (event.state.getBlock() == Blocks.RESPAWN_ANCHOR) {
                int c = event.state.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES);
                if (c < this.charges.get(event.pos)) {
                    event.setCancelled(true);
                }
            } else if (this.charges.get(event.pos) != -1) {
                event.setCancelled(true);
            }

            this.realStates.remove(event.pos);
            this.realStates.put(event.pos, event.state);
        }
    }

    @Event
    public void onSent(PacketEvent.Sent event) {
        if (event.packet instanceof ServerboundUseItemOnPacket packet) {
            BlockPos pos = packet.getHitResult().getBlockPos().relative(packet.getHitResult().getDirection());
            ItemStack holdingStack = packet.getHand() == InteractionHand.MAIN_HAND ? Managers.PACKET.getStack() : BlackOut.mc.player.getOffhandItem();
            if (holdingStack.getItem() == Items.RESPAWN_ANCHOR) {
                this.own.add(pos, this.ownTime.get());
            }
        }
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        this.own.update();
        if (PlayerUtils.isInGame()) {
            if (Managers.PACKET.getStack().getItem() == Items.RESPAWN_ANCHOR) {
                this.prevAnchor = Managers.PACKET.slot;
            }

            if (BlackOut.mc.hitResult instanceof BlockHitResult hitResult) {
                if (hitResult.getType() != HitResult.Type.MISS) {
                    BlockState state = BlackOut.mc.level.getBlockState(hitResult.getBlockPos());
                    if (state.getBlock() == Blocks.RESPAWN_ANCHOR) {
                        if (!this.onlyOwn.get() || this.own.contains(hitResult.getBlockPos())) {
                            if (!(this.actions <= 0.0)) {
                                if (!this.fullCharge.get() && state.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES) > 0) {
                                    this.explode(hitResult);
                                } else {
                                    this.charge(hitResult);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void charge(BlockHitResult blockHitResult) {
        InteractionHand hand = null;
        if (Managers.PACKET.getStack().getItem() == Items.GLOWSTONE) {
            hand = InteractionHand.MAIN_HAND;
        }

        if (this.allowOffhand.get() && BlackOut.mc.player.getOffhandItem().getItem() == Items.GLOWSTONE) {
            hand = InteractionHand.OFF_HAND;
        }

        FindResult result = this.glowstoneSwitch.get().find(Items.GLOWSTONE);
        boolean switched = false;
        if (hand != null || result.wasFound() && (switched = this.glowstoneSwitch.get().swap(result.slot()))) {
            hand = hand == null ? InteractionHand.MAIN_HAND : hand;
            BlockPos pos = blockHitResult.getBlockPos();
            BlockState state = BlackOut.mc.level.getBlockState(pos);
            if (state.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES) < 4) {
                int c = state.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES) + 1;
                this.charges.add(pos, c, 0.3);
                BlackOut.mc.level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES, c));
            } else {
                this.charges.add(pos, -1, 0.3);
                BlackOut.mc.level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }

            BlackOut.mc.gameMode.useItemOn(BlackOut.mc.player, hand, blockHitResult);
            BlackOut.mc.getConnection().send(new ServerboundSwingPacket(hand));
            this.clientSwing(SwingHand.RealHand, hand);
            this.actions--;
            if (switched) {
                this.glowstoneSwitch.get().swapBack();
            }
        }
    }

    private void explode(BlockHitResult blockHitResult) {
        InteractionHand hand = null;
        FindResult result = InvUtils.findNullable(this.explodeSwitch.get().hotbar, false, this.emptyPredicate);
        boolean switched = false;
        if (this.prevAnchor > -1
                && BlackOut.mc.player.getInventory().getItem(this.prevAnchor).getItem() == Items.RESPAWN_ANCHOR
                && this.explodeSwitch.get().hotbar) {
            if (!(switched = this.explodeSwitch.get().swap(this.prevAnchor))) {
                return;
            }
        } else {
            if (this.emptyPredicate.test(Managers.PACKET.getStack())) {
                hand = InteractionHand.MAIN_HAND;
            }

            if (this.allowOffhand.get() && this.emptyPredicate.test(Managers.PACKET.getStack())) {
                hand = InteractionHand.OFF_HAND;
            }

            if (hand == null && (!result.wasFound() || !(switched = this.explodeSwitch.get().swap(result.slot())))) {
                return;
            }
        }

        BlockPos pos = blockHitResult.getBlockPos();
        this.charges.add(pos, -1, 0.3);
        BlackOut.mc.level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        hand = hand == null ? InteractionHand.MAIN_HAND : hand;
        BlackOut.mc.gameMode.useItemOn(BlackOut.mc.player, hand, blockHitResult);
        this.clientSwing(SwingHand.RealHand, hand);
        this.actions--;
        if (switched) {
            this.explodeSwitch.get().swapBack();
        }
    }
}
