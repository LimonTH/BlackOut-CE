package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.EntityAddEvent;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.mixin.accessors.AccessorInteractEntityC2SPacket;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class EntityManager extends Manager {
    private final TimerList<Integer> renderDead = new TimerList<>(true);
    private final TimerList<BlockPos> spawningItems = new TimerList<>(true);
    private final TimerList<Integer> semiDead = new TimerList<>(true);
    private final TimerList<BlockPos> waitingToRemoveItem = new TimerList<>(true);
    private final TimerList<Integer> attacked = new TimerList<>(true);

    public void setDead(int id, boolean full) {
        if (full) {
            BlackOut.mc.world.removeEntity(id, Entity.RemovalReason.KILLED);
        } else {
            this.renderDead.add(id, 1.0);
        }
    }

    public void setSemiDead(int i) {
        this.semiDead.add(i, 0.3);
    }

    public void addSpawning(BlockPos pos) {
        this.spawningItems.add(pos, 2.0);
    }

    public void removeSpawning(BlockPos pos) {
        this.spawningItems.remove(timer -> timer.value.equals(pos));
    }

    public boolean containsItem(BlockPos pos) {
        return this.spawningItems.contains(pos);
    }

    public void removeItems(BlockPos pos) {
        this.spawningItems.remove(p -> {
            if (p.value.equals(pos)) {
                this.waitingToRemoveItem.add(pos, 0.5);
                return true;
            } else {
                return false;
            }
        });
    }

    public boolean isDead(int id) {
        return this.semiDead.contains(id);
    }

    public boolean shouldRender(int id) {
        return !this.renderDead.contains(id);
    }

    @Override
    public void init() {
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
    }

    @Event
    public void onEntity(EntityAddEvent.Post event) {
        if (event.entity.getType() == EntityType.ITEM) {
            BlockPos pos = event.entity.getBlockPos();
            if (this.spawningItems.contains(pos)) {
                this.removeSpawning(pos);
            }

            if (this.waitingToRemoveItem.contains(pos)) {
                this.waitingToRemoveItem.remove(timer -> timer.value.equals(pos));
                this.setSemiDead(event.id);
            }
        }
    }

    @Event
    public void packetSendEvent(PacketEvent.Sent event) {
        if (event.packet instanceof PlayerInteractEntityC2SPacket packet) {
            AccessorInteractEntityC2SPacket packetAccessor = (AccessorInteractEntityC2SPacket) packet;
            if (packetAccessor.getType().getType() == PlayerInteractEntityC2SPacket.InteractType.ATTACK) {
                int id = packetAccessor.getId();
                Entity entity = BlackOut.mc.world.getEntityById(id);
                if (entity instanceof EndCrystalEntity && !this.attacked.contains(id)) {
                    BlockPos center = entity.getBlockPos();

                    for (Direction dir : Direction.values()) {
                        this.removeItems(center.offset(dir));
                    }
                }

                this.attacked.replace(id, 0.25);
            }
        }
    }
}
