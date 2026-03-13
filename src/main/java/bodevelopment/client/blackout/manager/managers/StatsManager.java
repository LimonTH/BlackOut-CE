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
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToDoubleFunction;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.item.ItemStack;

public class StatsManager extends Manager {
    private final Map<UUID, TrackerMap> dataMap = new Object2ObjectOpenHashMap<>();

    @Override
    public void init() {
        BlackOut.EVENT_BUS.subscribe(this, () -> BlackOut.mc.player == null || BlackOut.mc.level == null);
    }

    @Event
    public void onTickPost(TickEvent.Post event) {
        BlackOut.mc.level.players().forEach(player -> {
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
        if (event.entity instanceof ThrownExperienceBottle bottle) {
            AbstractClientPlayer bottleOwner = this.getOwner(bottle);
            TrackerData data = this.getStats(bottleOwner);
            if (data != null) {
                data.bottles++;
            }
        }
    }

    @Event
    public void onReceive(PacketEvent.Receive.Pre event) {
        if (event.packet instanceof ClientboundEntityEventPacket packet && packet.getEventId() == 35) {
            if (packet.getEntity(BlackOut.mc.level) instanceof AbstractClientPlayer player) {
                TrackerData data = this.getStats(player);
                if (data != null) data.onPop(player);
            }
        }

        if (event.packet instanceof ClientboundSoundPacket packet) {
            if (packet.getSound().value().equals(SoundEvents.PLAYER_BURP)) {
                AbstractClientPlayer closest = this.getClosest(
                        p -> p.position().distanceToSqr(packet.getX(), packet.getY(), packet.getZ())
                );
                TrackerData data = this.getStats(closest);
                if (data != null) data.eaten++;
            }
        }

        if (event.packet instanceof ClientboundSetEquipmentPacket packet) {
            if (BlackOut.mc.level.getEntity(packet.getEntity()) instanceof AbstractClientPlayer player) {
                packet.getSlots().forEach(pair -> {
                    EquipmentSlot slot = pair.getFirst();
                    if (!slot.isArmor()) return; // Нас интересует только броня

                    ItemStack newStack = pair.getSecond();
                    ItemStack oldStack = player.getItemBySlot(slot);

                    if (oldStack.is(newStack.getItem())) {
                        if (ItemStack.isSameItemSameComponents(oldStack, newStack)) {
                            int diff = oldStack.getDamageValue() - newStack.getDamageValue();
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

    private AbstractClientPlayer getOwner(ThrownExperienceBottle bottle) {
        return bottle.getOwner() instanceof AbstractClientPlayer player ? player : this.getClosest(playerx -> playerx.getEyePosition().distanceTo(bottle.position()));
    }

    private AbstractClientPlayer getClosest(ToDoubleFunction<AbstractClientPlayer> function) {
        if (BlackOut.mc.level == null || BlackOut.mc.level.players().isEmpty()) {
            return null;
        }
        return BlackOut.mc.level.players().stream()
                .min(Comparator.comparingDouble(function))
                .orElse(null);
    }

    public void reset() {
        this.dataMap.clear();
    }

    public TrackerData getStats(AbstractClientPlayer player) {
        if (player == null || player.getGameProfile() == null) {
            return null;
        }

        UUID uuid = player.getGameProfile().getId();
        if (!this.dataMap.containsKey(uuid)) {
            return null;
        } else {
            TrackerMap map = this.dataMap.get(uuid);
            return (map == null || !map.is(player)) ? null : map.get().data();
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
        private BlockPos prevPos = BlockPos.ZERO;
        private float prevHealth = 0.0F;

        private void tick(AbstractClientPlayer player) {
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

            if (OLEPOSSUtils.inside(player, player.getBoundingBox().deflate(0.04, 0.06, 0.04))) {
                this.phasedFor++;
            }

            this.prevHealth = health;
            this.prevPos = pos;
        }

        private void onPop(AbstractClientPlayer player) {
            this.pops++;
            BlackOut.EVENT_BUS.post(PopEvent.get(player, this.pops));
        }
    }

    private static class TrackerMap {
        private Tracker current;

        private void set(AbstractClientPlayer player) {
            if (this.entityChanged(player)) {
                this.current = new Tracker(player, new TrackerData());
            } else {
                this.current = new Tracker(player, this.current.data());
            }
        }

        private boolean entityChanged(AbstractClientPlayer newPlayer) {
            if (this.current == null) {
                return true;
            } else {
                long sinceUpdate = System.currentTimeMillis() - this.current.data.lastUpdate;
                if (sinceUpdate < 500L && this.current.data.prevPos.getCenter().distanceToSqr(newPlayer.position()) > 1000.0) {
                    return true;
                } else {
                    return sinceUpdate > 60000L || this.current.player.getRemovalReason() == Entity.RemovalReason.KILLED;
                }
            }
        }

        private boolean is(AbstractClientPlayer player) {
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

        private record Tracker(AbstractClientPlayer player, TrackerData data) {
        }
    }
}
