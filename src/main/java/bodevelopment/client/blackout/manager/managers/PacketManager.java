package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.EntityAddEvent;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.interfaces.mixin.IEndCrystalEntity;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.mixin.accessors.AccessorInteractEntityC2SPacket;
import bodevelopment.client.blackout.mixin.accessors.AccessorPlayerMoveC2SPacket;
import bodevelopment.client.blackout.module.modules.misc.Simulation;
import bodevelopment.client.blackout.randomstuff.FakePlayerEntity;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import bodevelopment.client.blackout.randomstuff.timers.TimerMap;
import bodevelopment.client.blackout.util.SettingUtils;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PacketManager extends Manager {
    public final TimerList<Integer> ids = new TimerList<>(true);
    public final TimerMap<Integer, Vec3d> validPos = new TimerMap<>(true);
    public final TimerList<Integer> ignoreSetSlot = new TimerList<>(true);
    public final TimerList<ScreenHandlerSlotUpdateS2CPacket> ignoredInventory = new TimerList<>(true);
    private final List<Consumer<? super ClientPlayNetworkHandler>> grimQueue = new ArrayList<>();
    private final List<Consumer<? super ClientPlayNetworkHandler>> postGrimQueue = new ArrayList<>();
    private final TimerList<BlockPos> own = new TimerList<>(true);
    public int slot = 0;
    public Vec3d pos = Vec3d.ZERO;
    public int teleportId = 0;
    public int receivedId = 0;
    public int prevReceived = 0;
    private boolean onGround;
    private boolean spoofOG = false;
    private boolean spoofedOG = false;

    @Override
    public void init() {
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
        this.onGround = false;
    }

    @Event
    public void onSent(PacketEvent.Sent event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket packet && packet.getSelectedSlot() >= 0) {
            this.slot = packet.getSelectedSlot();
            BlackOut.mc.interactionManager.lastSelectedSlot = packet.getSelectedSlot();
        }

        if (event.packet instanceof PlayerMoveC2SPacket packet) {
            this.onGround = packet.isOnGround();
            if (packet.changesPosition()) {
                this.pos = new Vec3d(packet.getX(0.0), packet.getY(0.0), packet.getZ(0.0));
            }
        }

        if (event.packet instanceof TeleportConfirmC2SPacket packetx) {
            this.teleportId = packetx.getTeleportId();
        }

        if (event.packet instanceof PlayerInteractEntityC2SPacket packetx && ((AccessorInteractEntityC2SPacket) packetx).getType().getType() == PlayerInteractEntityC2SPacket.InteractType.ATTACK) {
            if (BlackOut.mc.world.getEntityById(((AccessorInteractEntityC2SPacket) packetx).getId()) instanceof FakePlayerEntity player) {
                Managers.FAKE_PLAYER.onAttack(player);
            }

            if (Simulation.getInstance().hitReset()) {
                BlackOut.mc.player.resetLastAttackedTicks();
            }

            if (Simulation.getInstance().stopSprint()) {
                BlackOut.mc.player.setSprinting(false);
            }
        }

        if (event.packet instanceof PlayerInteractBlockC2SPacket packetx && this.handStack(packetx.getHand()).isOf(Items.END_CRYSTAL)) {
            this.own.replace(packetx.getBlockHitResult().getBlockPos().up(), 1.0);
        }
    }

    @Event
    public void onEntityAdd(EntityAddEvent.Post event) {
        if (event.entity instanceof EndCrystalEntity entity && this.own.contains(entity.getBlockPos())) {
            ((IEndCrystalEntity) entity).blackout_Client$markOwn();
        }
    }

    @Event
    public void onSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket packet && this.spoofOG) {
            ((AccessorPlayerMoveC2SPacket) packet).setOnGround(this.spoofedOG);
            this.spoofOG = false;
        }
    }

    @Event
    public void onReceive(PacketEvent.Receive.Post e) {
        if (e.packet instanceof PlayerPositionLookS2CPacket packet) {
            Vec3d vec = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
            int id = packet.getTeleportId();
            if (this.validPos.containsKey(id) && this.validPos.get(id).equals(vec)) {
                e.setCancelled(true);
                this.validPos.removeKey(packet.getTeleportId());
            }

            this.prevReceived = this.receivedId;
            this.receivedId = packet.getTeleportId();
            if (!this.ids.contains(id)) {
                this.teleportId = id;
            }
        }

        if (e.packet instanceof UpdateSelectedSlotS2CPacket packet && this.ignoreSetSlot.contains(packet.getSlot())) {
            e.setCancelled(true);
        }

        if (e.packet instanceof ScreenHandlerSlotUpdateS2CPacket packet
                && this.ignoredInventory.contains(timer -> this.inventoryEquals(packet, timer.value))
                && !this.isItemEquals(packet)) {
            e.setCancelled(true);
        }
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        this.sendPackets();
    }

    private boolean isItemEquals(ScreenHandlerSlotUpdateS2CPacket packet) {
        return this.getStackInSlot(packet).isOf(packet.getStack().getItem());
    }

    private ItemStack getStackInSlot(ScreenHandlerSlotUpdateS2CPacket packet) {
        if (packet.getSyncId() == -1) {
            return null;
        } else if (packet.getSyncId() == -2) {
            return BlackOut.mc.player.getInventory().getStack(packet.getSlot());
        } else if (packet.getSyncId() == 0 && PlayerScreenHandler.isInHotbar(packet.getSlot())) {
            return BlackOut.mc.player.playerScreenHandler.getSlot(packet.getSlot()).getStack();
        } else {
            return packet.getSyncId() == BlackOut.mc.player.currentScreenHandler.syncId
                    ? BlackOut.mc.player.currentScreenHandler.getSlot(packet.getSlot()).getStack()
                    : null;
        }
    }

    public void sendPackets() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.sendList(this.grimQueue);
            this.sendList(this.postGrimQueue);
        }
    }

    private void sendList(List<Consumer<? super ClientPlayNetworkHandler>> list) {
        list.forEach(consumer -> this.sendPacket(BlackOut.mc.getNetworkHandler(), consumer));
        list.clear();
    }

    private void sendPacket(ClientPlayNetworkHandler handler, Consumer<? super ClientPlayNetworkHandler> consumer) {
        if (handler != null) {
            consumer.accept(handler);
        }
    }

    public void sendPacket(Packet<?> packet) {
        this.sendPacketToList(packet, this.grimQueue);
    }

    public void sendPostPacket(Packet<?> packet) {
        this.sendPacketToList(packet, this.postGrimQueue);
    }

    public void sendInstantly(Packet<?> packet) {
        this.sendPacket(BlackOut.mc.getNetworkHandler(), handler -> handler.sendPacket(packet));
    }

    private void sendPacketToList(Packet<?> packet, List<Consumer<? super ClientPlayNetworkHandler>> list) {
        if (this.shouldBeDelayed(packet)) {
            this.addToQueue(handler -> handler.sendPacket(packet), list);
        } else {
            BlackOut.mc.getNetworkHandler().sendPacket(packet);
        }
    }

    public void addToQueue(Consumer<? super ClientPlayNetworkHandler> consumer) {
        this.addToQueue(consumer, this.grimQueue);
    }

    public void addToPostQueue(Consumer<? super ClientPlayNetworkHandler> consumer) {
        this.addToQueue(consumer, this.postGrimQueue);
    }

    private void addToQueue(Consumer<? super ClientPlayNetworkHandler> consumer, List<Consumer<? super ClientPlayNetworkHandler>> list) {
        if (SettingUtils.grimPackets()) {
            list.add(consumer);
        } else {
            consumer.accept(BlackOut.mc.getNetworkHandler());
        }
    }

    private boolean shouldBeDelayed(Packet<?> packet) {
        if (!SettingUtils.grimPackets()) {
            return false;
        } else if (packet instanceof PlayerInteractEntityC2SPacket) {
            return true;
        } else if (packet instanceof PlayerInteractBlockC2SPacket) {
            return true;
        } else if (packet instanceof PlayerInteractItemC2SPacket) {
            return true;
        } else if (packet instanceof PlayerActionC2SPacket) {
            return true;
        } else if (packet instanceof HandSwingC2SPacket) {
            return true;
        } else if (packet instanceof UpdateSelectedSlotC2SPacket) {
            return true;
        } else {
            return packet instanceof ClickSlotC2SPacket || packet instanceof PickFromInventoryC2SPacket;
        }
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public ItemStack getStack() {
        return BlackOut.mc.player.getInventory().getStack(this.slot);
    }

    public ItemStack stackInHand(Hand hand) {
        return switch (hand) {
            case MAIN_HAND -> this.getStack();
            case OFF_HAND -> BlackOut.mc.player.getOffHandStack();
            default -> throw new IncompatibleClassChangeError();
        };
    }

    public boolean isHolding(Item... items) {
        ItemStack stack = this.getStack();
        if (stack == null) {
            return false;
        } else {
            for (Item item : items) {
                if (item.equals(stack.getItem())) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean isHolding(Item item) {
        ItemStack stack = this.getStack();
        return stack != null && stack.getItem().equals(item);
    }

    public void spoofOG(boolean state) {
        this.spoofOG = true;
        this.spoofedOG = state;
    }

    public ItemStack handStack(Hand hand) {
        return hand == Hand.MAIN_HAND ? this.getStack() : BlackOut.mc.player.getOffHandStack();
    }

    public TeleportConfirmC2SPacket incrementedPacket(Vec3d vec3d) {
        int id = this.teleportId + 1;
        this.ids.add(id, 1.0);
        this.validPos.add(id, vec3d, 1.0);
        return new TeleportConfirmC2SPacket(id);
    }

    public TeleportConfirmC2SPacket incrementedPacket2(Vec3d vec3d) {
        int id = this.receivedId + 1;
        this.ids.replace(id, 1.0);
        this.validPos.add(id, vec3d, 1.0);
        return new TeleportConfirmC2SPacket(id);
    }

    public void preApply(ScreenHandlerSlotUpdateS2CPacket packet) {
        packet.apply(BlackOut.mc.getNetworkHandler());
        this.addInvIgnore(packet);
    }

    public void addInvIgnore(ScreenHandlerSlotUpdateS2CPacket packet) {
        this.ignoredInventory.remove(timer -> this.inventoryEquals(timer.value, packet));
        this.ignoredInventory.add(packet, 0.3);
    }

    private boolean inventoryEquals(ScreenHandlerSlotUpdateS2CPacket packet1, ScreenHandlerSlotUpdateS2CPacket packet2) {
        return packet1.getSlot() == packet2.getSlot() && packet1.getStack().isOf(packet2.getStack().getItem());
    }

    public void sendPreUse() {
        this.sendInstantly(
                new PlayerMoveC2SPacket.Full(
                        BlackOut.mc.player.getX(),
                        BlackOut.mc.player.getY(),
                        BlackOut.mc.player.getZ(),
                        Managers.ROTATION.prevYaw,
                        Managers.ROTATION.prevPitch,
                        this.isOnGround()
                )
        );
    }

    public void sendPositionSync(Vec3d pos, float yaw, float pitch) {
        yaw = MathHelper.wrapDegrees(yaw);
        if (yaw >= 0.0F) {
            yaw = -180.0F - (180.0F - yaw);
        }

        Managers.PACKET.sendInstantly(new PlayerMoveC2SPacket.Full(pos.x, pos.y, pos.z, yaw, pitch, false));
    }
}
