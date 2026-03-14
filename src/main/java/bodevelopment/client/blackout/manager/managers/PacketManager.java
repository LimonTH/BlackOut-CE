package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.EntityAddEvent;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.interfaces.mixin.IEndCrystal;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.mixin.accessors.AccessorServerboundInteractPacket;
import bodevelopment.client.blackout.mixin.accessors.AccessorServerboundMovePlayerPacket;
import bodevelopment.client.blackout.module.modules.misc.Simulation;
import bodevelopment.client.blackout.randomstuff.FakePlayerEntity;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import bodevelopment.client.blackout.randomstuff.timers.TimerMap;
import bodevelopment.client.blackout.util.SettingUtils;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PacketManager extends Manager {
    public final TimerList<Integer> ids = new TimerList<>(true);
    public final TimerMap<Integer, Vec3> validPos = new TimerMap<>(true);
    public final TimerList<Integer> ignoreSetSlot = new TimerList<>(true);
    public final TimerList<ClientboundContainerSetSlotPacket> ignoredInventory = new TimerList<>(true);
    private final List<Consumer<? super ClientPacketListener>> grimQueue = new ArrayList<>();
    private final List<Consumer<? super ClientPacketListener>> postGrimQueue = new ArrayList<>();
    private final TimerList<BlockPos> own = new TimerList<>(true);
    public int slot = 0;
    public Vec3 pos = Vec3.ZERO;
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
        if (event.packet instanceof ServerboundSetCarriedItemPacket packet && packet.getSlot() >= 0) {
            this.slot = packet.getSlot();
            BlackOut.mc.gameMode.carriedIndex = packet.getSlot();
        }

        if (event.packet instanceof ServerboundMovePlayerPacket packet) {
            this.onGround = packet.isOnGround();
            if (packet.hasPosition()) {
                this.pos = new Vec3(packet.getX(0.0), packet.getY(0.0), packet.getZ(0.0));
            }
        }

        if (event.packet instanceof ServerboundAcceptTeleportationPacket packetx) {
            this.teleportId = packetx.getId();
        }

        if (event.packet instanceof ServerboundInteractPacket packetx && ((AccessorServerboundInteractPacket) packetx).getType().getType() == ServerboundInteractPacket.ActionType.ATTACK) {
            if (BlackOut.mc.level.getEntity(((AccessorServerboundInteractPacket) packetx).getId()) instanceof FakePlayerEntity player) {
                Managers.FAKE_PLAYER.onAttack(player);
            }

            if (Simulation.getInstance().hitReset()) {
                BlackOut.mc.player.resetAttackStrengthTicker();
            }

            if (Simulation.getInstance().stopSprint()) {
                BlackOut.mc.player.setSprinting(false);
            }
        }

        if (event.packet instanceof ServerboundUseItemOnPacket packetx && this.handStack(packetx.getHand()).is(Items.END_CRYSTAL)) {
            this.own.replace(packetx.getHitResult().getBlockPos().above(), 1.0);
        }
    }

    public void syncRotation(float yaw, float pitch) {
        this.sendInstantly(new ServerboundMovePlayerPacket.Rot(yaw, pitch, this.isOnGround(), BlackOut.mc.player.horizontalCollision));
    }

    public boolean isQueueHeavy() {
        return grimQueue.size() > 10;
    }

    @Event
    public void onEntityAdd(EntityAddEvent.Post event) {
        if (event.entity instanceof EndCrystal entity && this.own.contains(entity.blockPosition())) {
            ((IEndCrystal) entity).blackout_Client$markOwn();
        }
    }

    @Event
    public void onSend(PacketEvent.Send event) {
        if (event.packet instanceof ServerboundMovePlayerPacket packet && this.spoofOG) {
            ((AccessorServerboundMovePlayerPacket) packet).setOnGround(this.spoofedOG);
            this.spoofOG = false;
        }
    }

    @Event
    public void onReceive(PacketEvent.Receive.Post e) {
        if (e.packet instanceof ClientboundPlayerPositionPacket packet) {
            Vec3 vec = new Vec3(packet.change().position().x(), packet.change().position().y(), packet.change().position().z());
            int id = packet.id();
            if (this.validPos.containsKey(id) && this.validPos.get(id).equals(vec)) {
                e.setCancelled(true);
                this.validPos.removeKey(packet.id());
            }

            this.prevReceived = this.receivedId;
            this.receivedId = packet.id();
            if (!this.ids.contains(id)) {
                this.teleportId = id;
            }
        }

        if (e.packet instanceof ClientboundSetHeldSlotPacket packet && this.ignoreSetSlot.contains(packet.getSlot())) {
            e.setCancelled(true);
        }

        if (e.packet instanceof ClientboundContainerSetSlotPacket packet
                && this.ignoredInventory.contains(timer -> this.inventoryEquals(packet, timer.value))
                && !this.isItemEquals(packet)) {
            e.setCancelled(true);
        }
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        this.sendPackets();
    }

    private boolean isItemEquals(ClientboundContainerSetSlotPacket packet) {
        return this.getStackInSlot(packet).is(packet.getItem().getItem());
    }

    private ItemStack getStackInSlot(ClientboundContainerSetSlotPacket packet) {
        if (packet.getContainerId() == -1) {
            return null;
        } else if (packet.getContainerId() == -2) {
            return BlackOut.mc.player.getInventory().getItem(packet.getSlot());
        } else if (packet.getContainerId() == 0 && InventoryMenu.isHotbarSlot(packet.getSlot())) {
            return BlackOut.mc.player.inventoryMenu.getSlot(packet.getSlot()).getItem();
        } else {
            return packet.getContainerId() == BlackOut.mc.player.containerMenu.containerId
                    ? BlackOut.mc.player.containerMenu.getSlot(packet.getSlot()).getItem()
                    : null;
        }
    }

    public void sendPackets() {
        if (BlackOut.mc.player != null && BlackOut.mc.level != null) {
            this.sendList(this.grimQueue);
            this.sendList(this.postGrimQueue);
        }
    }

    private void sendList(List<Consumer<? super ClientPacketListener>> list) {
        list.forEach(consumer -> this.sendPacket(BlackOut.mc.getConnection(), consumer));
        list.clear();
    }

    private void sendPacket(ClientPacketListener handler, Consumer<? super ClientPacketListener> consumer) {
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
        this.sendPacket(BlackOut.mc.getConnection(), handler -> handler.send(packet));
    }

    private void sendPacketToList(Packet<?> packet, List<Consumer<? super ClientPacketListener>> list) {
        if (this.shouldBeDelayed(packet)) {
            this.addToQueue(handler -> handler.send(packet), list);
        } else {
            BlackOut.mc.getConnection().send(packet);
        }
    }

    public void addToQueue(Consumer<? super ClientPacketListener> consumer) {
        this.addToQueue(consumer, this.grimQueue);
    }

    public void addToPostQueue(Consumer<? super ClientPacketListener> consumer) {
        this.addToQueue(consumer, this.postGrimQueue);
    }

    private void addToQueue(Consumer<? super ClientPacketListener> consumer, List<Consumer<? super ClientPacketListener>> list) {
        if (SettingUtils.grimPackets()) {
            list.add(consumer);
        } else {
            consumer.accept(BlackOut.mc.getConnection());
        }
    }

    private boolean shouldBeDelayed(Packet<?> packet) {
        if (!SettingUtils.grimPackets()) {
            return false;
        }
        return packet instanceof ServerboundInteractPacket
                || packet instanceof ServerboundUseItemOnPacket
                || packet instanceof ServerboundUseItemPacket
                || packet instanceof ServerboundPlayerActionPacket
                || packet instanceof ServerboundSwingPacket
                || packet instanceof ServerboundSetCarriedItemPacket
                || packet instanceof ServerboundContainerClickPacket
                || packet instanceof ServerboundPickItemPacket
                || packet instanceof ServerboundPlayerInputPacket;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public ItemStack getStack() {
        return BlackOut.mc.player.getInventory().getItem(this.slot);
    }

    public ItemStack stackInHand(InteractionHand hand) {
        return switch (hand) {
            case MAIN_HAND -> this.getStack();
            case OFF_HAND -> BlackOut.mc.player.getOffhandItem();
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

    public ItemStack handStack(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? this.getStack() : BlackOut.mc.player.getOffhandItem();
    }

    public ServerboundAcceptTeleportationPacket incrementedPacket(Vec3 vec3d) {
        int id = this.teleportId + 1;
        this.ids.add(id, 1.0);
        this.validPos.add(id, vec3d, 1.0);
        return new ServerboundAcceptTeleportationPacket(id);
    }

    public ServerboundAcceptTeleportationPacket incrementedPacket2(Vec3 vec3d) {
        int id = this.receivedId + 1;
        this.ids.replace(id, 1.0);
        this.validPos.add(id, vec3d, 1.0);
        return new ServerboundAcceptTeleportationPacket(id);
    }

    public void preApply(ClientboundContainerSetSlotPacket packet) {
        packet.handle(BlackOut.mc.getConnection());
        this.addInvIgnore(packet);
    }

    public void addInvIgnore(ClientboundContainerSetSlotPacket packet) {
        this.ignoredInventory.remove(timer -> this.inventoryEquals(timer.value, packet));
        this.ignoredInventory.add(packet, 0.3);
    }

    private boolean inventoryEquals(ClientboundContainerSetSlotPacket packet1, ClientboundContainerSetSlotPacket packet2) {
        return packet1.getSlot() == packet2.getSlot() && packet1.getItem().is(packet2.getItem().getItem());
    }

    public void sendPreUse() {
        this.sendInstantly(
                new ServerboundMovePlayerPacket.PosRot(
                        BlackOut.mc.player.getX(),
                        BlackOut.mc.player.getY(),
                        BlackOut.mc.player.getZ(),
                        Managers.ROTATION.prevYaw,
                        Managers.ROTATION.prevPitch,
                        this.isOnGround(),
                        BlackOut.mc.player.horizontalCollision
                )
        );
    }

    public void sendPositionSync(Vec3 pos, float yaw, float pitch) {
        yaw = Mth.wrapDegrees(yaw);
        if (yaw >= 0.0F) {
            yaw = -180.0F - (180.0F - yaw);
        }

        Managers.PACKET.sendInstantly(new ServerboundMovePlayerPacket.PosRot(pos.x, pos.y, pos.z, yaw, pitch, false, BlackOut.mc.player.horizontalCollision));
    }
}
