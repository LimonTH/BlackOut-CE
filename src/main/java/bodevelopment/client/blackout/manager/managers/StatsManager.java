package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.EntityAddEvent;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.PopEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.util.HoleUtils;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

public class StatsManager extends Manager {
    private final Map<UUID, TrackerMap> dataMap = new Object2ObjectOpenHashMap<>();

    @Override
    public void init() {
        BlackOut.EVENT_BUS.subscribe(this, () -> BlackOut.mc.player == null || BlackOut.mc.world == null);
    }

    @Event
    public void onTickPost(TickEvent.Post event) {
        BlackOut.mc.world.getPlayers().forEach(player -> {
            UUID uuid = player.getGameProfile().getId();
            if (!this.dataMap.containsKey(uuid)) {
                this.dataMap.put(uuid, new TrackerMap());
            }

            TrackerMap map = this.dataMap.get(uuid);
            if (!map.is(player)) {
                map.set(player);
            }
        });
        this.dataMap.forEach((uuid, map) -> map.tick());
    }

    @Event
    public void onSpawn(EntityAddEvent.Pre event) {
        if (event.entity instanceof ExperienceBottleEntity bottle) {
            AbstractClientPlayerEntity bottleOwner = this.getOwner(bottle);
            TrackerData data = this.getStats(bottleOwner);
            if (data != null) {
                data.bottles++;
            }
        }
    }

    @Event
    public void onReceive(PacketEvent.Receive.Pre event) {
        // (Totem Pops)
        if (event.packet instanceof EntityStatusS2CPacket packet && packet.getStatus() == 35) {
            if (packet.getEntity(BlackOut.mc.world) instanceof AbstractClientPlayerEntity player) {
                TrackerData data = this.getStats(player);
                if (data != null) data.onPop(player);
            }
        }

        // (Burp sound)
        if (event.packet instanceof PlaySoundS2CPacket packet) {
            if (packet.getSound().value().equals(SoundEvents.ENTITY_PLAYER_BURP)) {
                AbstractClientPlayerEntity closest = this.getClosest(
                        p -> p.getPos().squaredDistanceTo(packet.getX(), packet.getY(), packet.getZ())
                );
                TrackerData data = this.getStats(closest);
                if (data != null) data.eaten++;
            }
        }

        // (Mending Tracker)
        if (event.packet instanceof EntityEquipmentUpdateS2CPacket packet) {
            if (BlackOut.mc.world.getEntityById(packet.getEntityId()) instanceof AbstractClientPlayerEntity player) {
                packet.getEquipmentList().forEach(pair -> {
                    EquipmentSlot slot = pair.getFirst();
                    if (!slot.isArmorSlot()) return; // Нас интересует только броня

                    ItemStack newStack = pair.getSecond();
                    ItemStack oldStack = player.getEquippedStack(slot);

                    if (oldStack.isOf(newStack.getItem())) {
                        if (ItemStack.areItemsAndComponentsEqual(oldStack, newStack)) {
                            int diff = oldStack.getDamage() - newStack.getDamage();
                            if (diff > 0) {
                                TrackerData data = this.getStats(player);
                                if (data != null) {
                                    data.armorDamage += diff;
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    private AbstractClientPlayerEntity getOwner(ExperienceBottleEntity bottle) {
        return bottle.getOwner() instanceof AbstractClientPlayerEntity player ? player : this.getClosest(playerx -> playerx.getEyePos().distanceTo(bottle.getPos()));
    }

    private AbstractClientPlayerEntity getClosest(ToDoubleFunction<AbstractClientPlayerEntity> function) {
        return BlackOut.mc.world.getPlayers().stream().min(Comparator.comparingDouble(function)).orElse(null);
    }

    public void reset() {
        this.dataMap.clear();
    }

    public TrackerData getStats(AbstractClientPlayerEntity player) {
        UUID uuid = player.getGameProfile().getId();
        if (!this.dataMap.containsKey(uuid)) {
            return null;
        } else {
            TrackerMap map = this.dataMap.get(uuid);
            return !map.is(player) ? null : map.get().data();
        }
    }

    public static class TrackerData {
        public long lastUpdate = System.currentTimeMillis();
        public int trackedFor = 0;
        public int inHoleFor = 0;
        public int phasedFor = 0;
        public int pops = 0;
        public int eaten = 0;
        public int blocksMoved = 0;
        public double damage = 0.0;
        public double armorDamage = 0.0;
        public int bottles = 0;
        private BlockPos prevPos = BlockPos.ORIGIN;
        private float prevHealth = 0.0F;

        private void tick(AbstractClientPlayerEntity player) {
            this.lastUpdate = System.currentTimeMillis();
            this.trackedFor++;
            float health = player.getHealth() + player.getAbsorptionAmount();
            if (health < this.prevHealth) {
                this.damage = this.damage + (this.prevHealth - health);
            }

            BlockPos pos = new BlockPos(player.getBlockX(), (int) Math.round(player.getY()), player.getBlockZ());
            if (pos.getX() != this.prevPos.getX() || pos.getZ() != this.prevPos.getZ()) {
                this.blocksMoved++;
            }

            if (HoleUtils.inHole(pos)) {
                this.inHoleFor++;
            }

            if (OLEPOSSUtils.inside(player, player.getBoundingBox().contract(0.04, 0.06, 0.04))) {
                this.phasedFor++;
            }

            this.prevHealth = health;
            this.prevPos = pos;
        }

        private void onPop(AbstractClientPlayerEntity player) {
            this.pops++;
            BlackOut.EVENT_BUS.post(PopEvent.get(player, this.pops));
        }
    }

    private static class TrackerMap {
        private Tracker current;

        private void set(AbstractClientPlayerEntity player) {
            if (this.entityChanged(player)) {
                this.current = new Tracker(player, new TrackerData());
            } else {
                this.current = new Tracker(player, this.current.data());
            }
        }

        private boolean entityChanged(AbstractClientPlayerEntity newPlayer) {
            if (this.current == null) {
                return true;
            } else {
                long sinceUpdate = System.currentTimeMillis() - this.current.data.lastUpdate;
                if (sinceUpdate < 500L && this.current.data.prevPos.toCenterPos().squaredDistanceTo(newPlayer.getPos()) > 1000.0) {
                    return true;
                } else {
                    return sinceUpdate > 60000L || this.current.player.getRemovalReason() == Entity.RemovalReason.KILLED;
                }
            }
        }

        private boolean is(AbstractClientPlayerEntity player) {
            return this.current != null && this.current.player() == player;
        }

        private void tick() {
            if (!this.current.player().isRemoved()) {
                this.current.data().tick(this.current.player());
            }
        }

        private Tracker get() {
            return this.current;
        }

        private record Tracker(AbstractClientPlayerEntity player, TrackerData data) {
        }
    }
}
