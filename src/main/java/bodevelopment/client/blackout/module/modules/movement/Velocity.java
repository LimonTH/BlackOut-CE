package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.util.CompatUtils;
import bodevelopment.client.blackout.util.PlayerUtils;
import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.interfaces.mixin.IClientboundSetEntityMotionPacket;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.FakePlayerEntity;
import bodevelopment.client.blackout.randomstuff.timers.TickTimerList;
import java.util.concurrent.ThreadLocalRandom;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.phys.Vec3;

public class Velocity extends Module {
    private static Velocity INSTANCE;

    private final SettingGroup sgKnockback = this.addGroup("Knockback");
    private final SettingGroup sgPush = this.addGroup("Push");

    public final Setting<Mode> mode = this.sgKnockback.enumSetting("Reduction Mode", Mode.Simple, "The algorithm used to process incoming velocity packets.");
    public final Setting<Double> horizontal = this.sgKnockback.doubleSetting("Horizontal Velocity", 0.0, 0.0, 1.0, 0.01, "How much horizontal velocity to KEEP. (0.00 = No KB, 1.00 = Vanilla)");
    public final Setting<Double> vertical = this.sgKnockback.doubleSetting("Vertical Velocity", 0.0, 0.0, 1.0, 0.01, "How much vertical velocity to KEEP. (0.00 = No KB, 1.00 = Vanilla)");
    public final Setting<Double> hChance = this.sgKnockback.doubleSetting("Horizontal Probability", 1.0, 0.0, 1.0, 0.01, "The likelihood that horizontal reduction will be applied to a packet.", () -> this.mode.get() == Mode.Simple || this.mode.get() == Mode.Matrix_AAC);
    public final Setting<Double> vChance = this.sgKnockback.doubleSetting("Vertical Probability", 1.0, 0.0, 1.0, 0.01, "The likelihood that vertical reduction will be applied to a packet.", () -> this.mode.get() == Mode.Simple || this.mode.get() == Mode.Matrix_AAC);
    public final Setting<Double> chance = this.sgKnockback.doubleSetting("Execution Chance", 1.0, 0.0, 1.0, 0.01, "The overall probability of the velocity cancellation triggering.", () -> this.mode.get() == Mode.Grim);
    private final Setting<Boolean> single = this.sgKnockback.booleanSetting("Buffered Mode", true, "Consolidates multiple velocity updates into a single correction to reduce packet overhead.", () -> this.mode.get() == Mode.Grim);
    private final Setting<Integer> minDelay = this.sgKnockback.intSetting("Minimum Latency", 0, 0, 20, 1, "Minimum tick delay before the velocity packet is released.", () -> this.mode.get() == Mode.Delayed);
    private final Setting<Integer> maxDelay = this.sgKnockback.intSetting("Maximum Latency", 10, 0, 20, 1, "Maximum tick delay before the velocity packet is released.", () -> this.mode.get() == Mode.Delayed);
    private final Setting<Boolean> delayExplosion = this.sgKnockback.booleanSetting("Buffer Explosions", false, "Applies the latency delay to explosion-based velocity updates.", () -> this.mode.get() == Mode.Delayed);
    public final Setting<Boolean> fishingHook = this.sgKnockback.booleanSetting("Hook Immunity", true, "Negates knockback from fishing rod hooks.");
    public final Setting<Boolean> explosions = this.sgKnockback.booleanSetting("Explosive Immunity", true, "Applies velocity reduction logic to TNT and crystal explosions.");

    public final Setting<PushMode> entityPush = this.sgPush.enumSetting("Entity Collision", PushMode.Ignore, "Determines the interaction logic when colliding with other entities.");
    public final Setting<Double> acceleration = this.sgPush.doubleSetting("Force Multiplier", 1.0, 0.0, 2.0, 0.02, "The strength of the repulsive force applied during entity collisions.", () -> this.entityPush.get() == PushMode.Accelerate);
    public final Setting<Boolean> blockPush = this.sgPush.booleanSetting("Block Collision", true, "Prevents kinetic energy from blocks (like pistons) from moving the player.");

    private final TickTimerList<Tuple<Vec3, Boolean>> delayed = new TickTimerList<>(false);
    public boolean grim = false;

    public Velocity() {
        super("Velocity", "Mitigates or negates knockback received from entities, projectiles, and environmental hazards.", SubCategory.MOVEMENT, true);
        INSTANCE = this;
    }

    public static Velocity getInstance() {
        return INSTANCE;
    }

    @Override
    public String getInfo() {
        return this.mode.get().name();
    }

    @Event
    public void onTickPre(MoveEvent.Pre event) {
        if (this.enabled) {
            if (this.grim && this.single.get()) {
                this.sendGrimPackets();
                this.grim = false;
            }

            this.delayed
                    .timers
                    .removeIf(
                            item -> {
                                if (item.ticks-- <= 0) {
                                    if (item.value.getB()) {
                                        BlackOut.mc.player.push(item.value.getA());
                                    } else {
                                        BlackOut.mc
                                                .player
                                                .lerpMotion(
                                                        item.value.getA().x,
                                                        item.value.getA().y,
                                                        item.value.getA().z
                                                );
                                    }

                                    return true;
                                } else {
                                    return false;
                                }
                            }
                    );
        }
    }

    @Event
    public void onVelocity(PacketEvent.Receive.Pre event) {
        if (event.packet instanceof ClientboundSetEntityMotionPacket packet) {
            if (BlackOut.mc.player == null || BlackOut.mc.player.getId() != packet.getId()) {
                return;
            }
            switch (this.mode.get()) {
                case Simple:
                    double xVel = packet.getXa();
                    double yVel = packet.getYa();
                    double zVel = packet.getZa();

                    double random = ThreadLocalRandom.current().nextDouble();

                    if (this.hChance.get() >= random) {
                        ((IClientboundSetEntityMotionPacket) packet).blackout_Client$setX((int) (xVel * this.horizontal.get()));
                        ((IClientboundSetEntityMotionPacket) packet).blackout_Client$setZ((int) (zVel * this.horizontal.get()));
                    }

                    if (this.vChance.get() >= random) {
                        ((IClientboundSetEntityMotionPacket) packet).blackout_Client$setY((int) (yVel * this.vertical.get()));
                    }
                    break;
                case Delayed:
                    this.delayed
                            .add(
                                    new Tuple<>(new Vec3(packet.getXa() / 8000.0, packet.getYa() / 8000.0, packet.getZa() / 8000.0), false),
                                    this.getDelay()
                            );
                    event.setCancelled(true);
                    break;
                case Matrix_AAC:
                    double velX = (packet.getXa() / 8000.0 - BlackOut.mc.player.getDeltaMovement().x) * this.horizontal.get();
                    double velY = (packet.getYa() / 8000.0 - BlackOut.mc.player.getDeltaMovement().y) * this.vertical.get();
                    double velZ = (packet.getZa() / 8000.0 - BlackOut.mc.player.getDeltaMovement().z) * this.horizontal.get();

                    ((IClientboundSetEntityMotionPacket) packet).blackout_Client$setX((int) ((velX + BlackOut.mc.player.getDeltaMovement().x) * 8000.0));
                    ((IClientboundSetEntityMotionPacket) packet).blackout_Client$setY((int) ((velY + BlackOut.mc.player.getDeltaMovement().y) * 8000.0));
                    ((IClientboundSetEntityMotionPacket) packet).blackout_Client$setZ((int) ((velZ + BlackOut.mc.player.getDeltaMovement().z) * 8000.0));
                    break;
                case Vulcan:
                    if (BlackOut.mc.player.onGround()) {
                        ((IClientboundSetEntityMotionPacket) packet).blackout_Client$setY((int) (0.42 * 8000.0));
                        ((IClientboundSetEntityMotionPacket) packet).blackout_Client$setX(0);
                        ((IClientboundSetEntityMotionPacket) packet).blackout_Client$setZ(0);
                    }
                    break;
                case Grim:
                    if (this.chance.get() >= ThreadLocalRandom.current().nextDouble()) {
                        this.grimCancel(event, false);
                    }
            }
        }

        if (event.packet instanceof ClientboundExplodePacket packet && this.explosions.get()) {
            packet.playerKnockback().ifPresent(knockback -> {
                switch (this.mode.get()) {
                    case Grim:
                        if (this.chance.get() >= ThreadLocalRandom.current().nextDouble()) {
                            this.grimCancel(event, true);
                        }
                        break;
                    case Delayed:
                        if (this.delayExplosion.get()) {
                            this.delayed.add(new Tuple<>(new Vec3(knockback.x, knockback.y, knockback.z), true), this.getDelay());
                            event.setCancelled(true);
                        }
                        break;
                    default:
                        // Simple / Matrix_AAC / Vulcan: handled directly in
                        // MixinClientPacketListener.wrapExplosionKnockback which intercepts
                        // the LocalPlayer.push() call inside handleExplosion(). This works for
                        // both normal packets and bundle-wrapped packets that bypass PacketEvent.
                        break;
                }
            });
        }
    }

    @Event
    public void onTickPre(TickEvent.Pre event) {
        if (PlayerUtils.isInGame()) {
            if (this.entityPush.get() == PushMode.Accelerate) {
                if (Managers.ROTATION.move) {
                    BlackOut.mc
                            .level
                            .getEntities(
                                    BlackOut.mc.player,
                                    BlackOut.mc.player.getBoundingBox().inflate(1.0),
                                    entity -> this.validForCollisions(entity) && !BlackOut.mc.player.isPassengerOfSameVehicle(entity)
                            )
                            .forEach(entity -> {
                                double distX = entity.getX() - BlackOut.mc.player.getX();
                                double distZ = entity.getZ() - BlackOut.mc.player.getZ();
                                double maxDist = Mth.absMax(distX, distZ);
                                if (!(maxDist < 0.01F)) {
                                    maxDist = Math.sqrt(maxDist);
                                    distX /= maxDist;
                                    distZ /= maxDist;
                                    double d = Math.min(1.0 / maxDist, 1.0);
                                    distX *= d;
                                    distZ *= d;
                                    double speed = Math.sqrt(distX * distX + distZ * distZ) * this.acceleration.get() * 0.05;
                                    double yaw = Math.toRadians(Managers.ROTATION.moveYaw + 90.0F);
                                    BlackOut.mc.player.push(Math.cos(yaw) * speed, 0.0, Math.sin(yaw) * speed);
                                }
                            });
                }
            }
        }
    }

    private void sendGrimPackets() {
        if (BlackOut.mc.player == null) return;
        // Use the normal packet queue (sendPacket) rather than sendInstantly so this packet
        // is not injected in the middle of another module's inventory transaction sequence.
        this.sendPacket(new ServerboundContainerClickPacket(
                0, 0, -1, 0, ClickType.PICKUP,
                BlackOut.mc.player.getInventory().getSelected(),
                new Int2ObjectOpenHashMap<>()
        ));
    }

    private void grimCancel(PacketEvent.Receive.Pre event, boolean explosion) {
        this.sendGrimPackets();
        event.setCancelled(true);
    }

    private boolean validForCollisions(Entity entity) {
        return switch (entity) {
            case FakePlayerEntity fakePlayerEntity -> false;
            case Boat boatEntity -> true;
            case Minecart minecartEntity -> true;
            default -> entity instanceof LivingEntity && !(entity instanceof ArmorStand);
        };
    }

    private int getDelay() {
        return Mth.lerpInt((float) ThreadLocalRandom.current().nextDouble(), this.minDelay.get(), this.maxDelay.get());
    }

    public enum Mode {
        Simple,
        Matrix_AAC,
        Delayed,
        Grim,
        Vulcan
    }

    public enum PushMode {
        Accelerate,
        Ignore,
        Disabled
    }
}
