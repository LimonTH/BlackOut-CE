package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.interfaces.mixin.IEntityVelocityUpdateS2CPacket;
import bodevelopment.client.blackout.interfaces.mixin.IExplosionS2CPacket;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.FakePlayerEntity;
import bodevelopment.client.blackout.randomstuff.timers.TickTimerList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

public class Velocity extends Module {
    private static Velocity INSTANCE;

    private final SettingGroup sgKnockback = this.addGroup("Knockback");
    private final SettingGroup sgPush = this.addGroup("Push");

    public final Setting<Mode> mode = this.sgKnockback.enumSetting("Reduction Mode", Mode.Simple, "The algorithm used to process incoming velocity packets.");
    public final Setting<Double> horizontal = this.sgKnockback.doubleSetting("Horizontal Factor", 0.0, 0.0, 1.0, 0.01, "The percentage of horizontal impulse to retain (0% is full cancel).", () -> this.mode.get() == Mode.Simple);
    public final Setting<Double> vertical = this.sgKnockback.doubleSetting("Vertical Factor", 0.0, 0.0, 1.0, 0.01, "The percentage of vertical impulse to retain.", () -> this.mode.get() == Mode.Simple);
    public final Setting<Double> hChance = this.sgKnockback.doubleSetting("Horizontal Probability", 1.0, 0.0, 1.0, 0.01, "The likelihood that horizontal reduction will be applied to a packet.", () -> this.mode.get() == Mode.Simple);
    public final Setting<Double> vChance = this.sgKnockback.doubleSetting("Vertical Probability", 1.0, 0.0, 1.0, 0.01, "The likelihood that vertical reduction will be applied to a packet.", () -> this.mode.get() == Mode.Simple);
    public final Setting<Double> chance = this.sgKnockback.doubleSetting("Execution Chance", 1.0, 0.0, 1.0, 0.01, "The overall probability of the velocity cancellation triggering.", () -> this.mode.get() == Mode.Grim);
    private final Setting<Boolean> single = this.sgKnockback.booleanSetting("Buffered Mode", true, "Consolidates multiple velocity updates into a single correction to reduce packet overhead.", () -> this.mode.get() == Mode.Grim);
    private final Setting<Integer> minDelay = this.sgKnockback.intSetting("Minimum Latency", 0, 0, 20, 1, "Minimum tick delay before the velocity packet is released.", () -> this.mode.get() == Mode.Delayed);
    private final Setting<Integer> maxDelay = this.sgKnockback.intSetting("Maximum Latency", 10, 0, 20, 1, "Maximum tick delay before the velocity packet is released.", () -> this.mode.get() == Mode.Delayed);
    private final Setting<Boolean> delayExplosion = this.sgKnockback.booleanSetting("Buffer Explosions", false, "Applies the latency delay to explosion-based velocity updates.", () -> this.mode.get() == Mode.Delayed);
    public final Setting<Boolean> fishingHook = this.sgKnockback.booleanSetting("Hook Immunity", true, "Negates knockback from fishing rod hooks.");
    private final Setting<Boolean> explosions = this.sgKnockback.booleanSetting("Explosive Immunity", true, "Applies velocity reduction logic to TNT and crystal explosions.");

    public final Setting<PushMode> entityPush = this.sgPush.enumSetting("Entity Collision", PushMode.Ignore, "Determines the interaction logic when colliding with other entities.");
    public final Setting<Double> acceleration = this.sgPush.doubleSetting("Force Multiplier", 1.0, 0.0, 2.0, 0.02, "The strength of the repulsive force applied during entity collisions.", () -> this.entityPush.get() == PushMode.Accelerate);
    public final Setting<Boolean> blockPush = this.sgPush.booleanSetting("Block Collision", true, "Prevents kinetic energy from blocks (like pistons) from moving the player.");

    private final TickTimerList<Pair<Vec3d, Boolean>> delayed = new TickTimerList<>(false);
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
                                    if (item.value.getRight()) {
                                        BlackOut.mc.player.addVelocity(item.value.getLeft());
                                    } else {
                                        BlackOut.mc
                                                .player
                                                .setVelocityClient(
                                                        item.value.getLeft().x,
                                                        item.value.getLeft().y,
                                                        item.value.getLeft().z
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
    public void onVelocity(PacketEvent.Receive.Post event) {
        if (event.packet instanceof EntityVelocityUpdateS2CPacket packet) {
            if (BlackOut.mc.player == null || BlackOut.mc.player.getId() != packet.getEntityId()) {
                return;
            }

            switch (this.mode.get()) {
                case Simple:
                    int x = (int) (packet.getVelocityX() - BlackOut.mc.player.getVelocity().x * 8000.0);
                    int y = (int) (packet.getVelocityY() - BlackOut.mc.player.getVelocity().y * 8000.0);
                    int z = (int) (packet.getVelocityZ() - BlackOut.mc.player.getVelocity().z * 8000.0);
                    double random = ThreadLocalRandom.current().nextDouble();
                    if (this.hChance.get() >= random) {
                        ((IEntityVelocityUpdateS2CPacket) packet)
                                .blackout_Client$setX((int) (x * this.horizontal.get() + BlackOut.mc.player.getVelocity().x * 8000.0));
                        ((IEntityVelocityUpdateS2CPacket) packet)
                                .blackout_Client$setZ((int) (z * this.horizontal.get() + BlackOut.mc.player.getVelocity().z * 8000.0));
                    }

                    if (this.vChance.get() >= random) {
                        ((IEntityVelocityUpdateS2CPacket) packet)
                                .blackout_Client$setY((int) (y * this.vertical.get() + BlackOut.mc.player.getVelocity().y * 8000.0));
                    }
                    break;
                case Delayed:
                    this.delayed
                            .add(
                                    new Pair<>(new Vec3d(packet.getVelocityX() / 8000.0, packet.getVelocityY() / 8000.0, packet.getVelocityZ() / 8000.0), false),
                                    this.getDelay()
                            );
                    event.setCancelled(true);
                    break;
                case Grim:
                    if (this.chance.get() >= ThreadLocalRandom.current().nextDouble()) {
                        this.grimCancel(event, false);
                    }
            }
        }

        if (event.packet instanceof ExplosionS2CPacket packet && this.explosions.get()) {
            switch (this.mode.get()) {
                case Simple:
                    if (this.hChance.get() >= ThreadLocalRandom.current().nextDouble()) {
                        ((IExplosionS2CPacket) packet).blackout_Client$multiplyXZ(this.horizontal.get().floatValue());
                    }

                    if (this.vChance.get() >= ThreadLocalRandom.current().nextDouble()) {
                        ((IExplosionS2CPacket) packet).blackout_Client$multiplyY(this.vertical.get().floatValue());
                    }
                    break;
                case Delayed:
                    if (this.delayExplosion.get()) {
                        this.delayed.add(new Pair<>(new Vec3d(packet.getPlayerVelocityX(), packet.getPlayerVelocityY(), packet.getPlayerVelocityZ()), true), this.getDelay());
                        event.setCancelled(true);
                    }
                    break;
                case Grim:
                    if (this.chance.get() >= ThreadLocalRandom.current().nextDouble()) {
                        this.grimCancel(event, true);
                    }
            }
        }
    }

    @Event
    public void onTickPre(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (this.entityPush.get() == PushMode.Accelerate) {
                if (Managers.ROTATION.move) {
                    BlackOut.mc
                            .world
                            .getOtherEntities(
                                    BlackOut.mc.player,
                                    BlackOut.mc.player.getBoundingBox().expand(1.0),
                                    entity -> this.validForCollisions(entity) && !BlackOut.mc.player.isConnectedThroughVehicle(entity)
                            )
                            .forEach(entity -> {
                                double distX = entity.getX() - BlackOut.mc.player.getX();
                                double distZ = entity.getZ() - BlackOut.mc.player.getZ();
                                double maxDist = MathHelper.absMax(distX, distZ);
                                if (!(maxDist < 0.01F)) {
                                    maxDist = Math.sqrt(maxDist);
                                    distX /= maxDist;
                                    distZ /= maxDist;
                                    double d = Math.min(1.0 / maxDist, 1.0);
                                    distX *= d;
                                    distZ *= d;
                                    double speed = Math.sqrt(distX * distX + distZ * distZ) * this.acceleration.get() * 0.05;
                                    double yaw = Math.toRadians(Managers.ROTATION.moveYaw + 90.0F);
                                    BlackOut.mc.player.addVelocity(Math.cos(yaw) * speed, 0.0, Math.sin(yaw) * speed);
                                }
                            });
                }
            }
        }
    }

    private void sendGrimPackets() {
        Vec3d vec = Managers.PACKET.pos;
        Managers.PACKET
                .sendInstantly(
                        new PlayerMoveC2SPacket.Full(
                                vec.getX(), vec.getY(), vec.getZ(), Managers.ROTATION.prevYaw, Managers.ROTATION.prevPitch, Managers.PACKET.isOnGround()
                        )
                );
        BlockPos pos = new BlockPos((int) Math.floor(vec.x), (int) Math.floor(vec.y) + 1, (int) Math.floor(vec.z));
        Managers.PACKET.sendInstantly(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN, 0));
    }

    private void grimCancel(PacketEvent.Receive.Post event, boolean explosion) {
        if (!this.single.get()) {
            this.sendGrimPackets();
            event.setCancelled(true);
        } else if (!this.grim) {
            if (!explosion) {
                this.grim = true;
            }

            event.setCancelled(true);
        }
    }

    private boolean validForCollisions(Entity entity) {
        return switch (entity) {
            case FakePlayerEntity fakePlayerEntity -> false;
            case BoatEntity boatEntity -> true;
            case MinecartEntity minecartEntity -> true;
            default -> entity instanceof LivingEntity && !(entity instanceof ArmorStandEntity);
        };
    }

    private int getDelay() {
        return MathHelper.lerp((float) ThreadLocalRandom.current().nextDouble(), this.minDelay.get(), this.maxDelay.get());
    }

    public enum Mode {
        Simple,
        Delayed,
        Grim
    }

    public enum PushMode {
        Accelerate,
        Ignore,
        Disabled
    }
}
