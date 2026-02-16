package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.interfaces.functional.EpicInterface;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.module.modules.client.settings.ExtrapolationSettings;
import bodevelopment.client.blackout.randomstuff.MotionData;
import bodevelopment.client.blackout.randomstuff.timers.TickTimerList;
import bodevelopment.client.blackout.util.*;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ExtrapolationManager extends Manager {
    private Map<PlayerEntity, ExtrapolationData> dataMap = new ConcurrentHashMap<>();

    private static MotionData getMotion(ExtrapolationData data) {
        return HorizontalExtrapolation.getMotion(data).y(data.motions.isEmpty() ? 0.0 : gravityMod(gravityMod(data.motions.getFirst().y)));
    }

    private static double gravityMod(double y) {
        return (y - 0.08) * 0.98;
    }

    @Override
    public void init() {
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
    }

    @Event
    public void onTick(TickEvent.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            Map<PlayerEntity, ExtrapolationData> newMap = new ConcurrentHashMap<>();

            for (PlayerEntity player : BlackOut.mc.world.getPlayers()) {
                ExtrapolationData data = this.getFromMap(player);
                if (data != null) {
                    data.update();
                    newMap.put(player, data);
                } else {
                    newMap.put(player, new ExtrapolationData(player));
                }
            }

            this.dataMap = newMap;
            this.dataMap.forEach((playerx, datax) -> {
                datax.setTicksSince(Math.min(datax.getTicksSince() + 1, 8));
                if (!(playerx instanceof OtherClientPlayerEntity)) {
                    datax.handleMotion(playerx.getPos().subtract(playerx.prevX, playerx.prevY, playerx.prevZ), datax.getEntity());
                }
            });
        }
    }

    @Event
    public void onReceive(PacketEvent.Receive.Pre event) {
        if (event.packet instanceof EntityAnimationS2CPacket packet && (packet.getAnimationId() == 0 || packet.getAnimationId() == 3)) {
            this.dataMap.forEach((player, data) -> {
                if (packet.getEntityId() == player.getId()) {
                    data.stopLag();
                }
            });
        }
    }

    private ExtrapolationData getFromMap(PlayerEntity player) {
        for (Entry<PlayerEntity, ExtrapolationData> data : this.dataMap.entrySet()) {
            if (data.getKey() == player) {
                return data.getValue();
            }
        }

        return null;
    }

    public void tick(PlayerEntity player, Vec3d motion) {
        ExtrapolationData data = this.getFromMap(player);
        if (data != null) {
            data.handleMotion(motion, data.getEntity());
        }
    }

    public Map<PlayerEntity, ExtrapolationData> getDataMap() {
        return this.dataMap;
    }

    public void extrapolateMap(Map<Entity, Box> old, EpicInterface<Entity, Integer> extrapolation) {
        old.clear();
        this.dataMap.forEach((player, data) -> {
            Box box = data.extrapolate(player, extrapolation.get(player));
            old.put(player, box);
        });
    }

    public Box extrapolate(Entity entity, int ticks) {
        if (entity instanceof PlayerEntity player) {
            ExtrapolationData data = this.getFromMap(player);
            return data == null ? entity.getBoundingBox() : data.extrapolate(player, ticks);
        } else {
            return entity.getBoundingBox();
        }
    }

    public static class ExtrapolationData {
        public final List<Vec3d> motions = new ArrayList<>();
        private final List<Boolean> onGrounds = new ArrayList<>();
        private final TickTimerList<Double> step = new TickTimerList<>(false);
        private final TickTimerList<Double> reverseStep = new TickTimerList<>(false);
        private final Entity entity;
        private boolean goingToJump = false;
        private double stepHeight = 0.0;
        private double reverseHeight = 0.0;
        private double jumpHeight = 0.42;
        private MotionData motionData = MotionData.of(new Vec3d(0.0, 0.0, 0.0));
        private Vec3d prevPos;
        private int ticksSince = 0;
        private int stillFor = 0;
        private boolean prevOffGround = false;
        private boolean movedUp = false;
        private boolean movedDown = false;
        private double prevYaw = 0.0;
        private double prevPitch = 0.0;

        public ExtrapolationData(Entity entity) {
            this.entity = entity;
            this.motions.add(new Vec3d(0.0, 0.0, 0.0));
        }

        private void update() {
            this.step.update();
            this.reverseStep.update();
            Vec3d currentPos = this.entity.getPos();
            if (this.rotated() && !this.entity.isOnGround()) {
                this.stopLag();
            } else if (this.prevPos != null && this.prevPos.squaredDistanceTo(currentPos) < 0.01) {
                this.stillFor++;
            }

            this.prevPos = currentPos;
            this.prevYaw = this.entity.getYaw();
            this.prevPitch = this.entity.getPitch();
            this.onGrounds.addFirst(Simulator.isOnGround(this.getEntity(), this.getEntity().getBoundingBox()));
            OLEPOSSUtils.limitList(this.onGrounds, 3);
            boolean offGround = this.isOffGround();
            if (offGround && !this.prevOffGround) {
                this.movedDown = this.motions.getFirst().y > 0.0;
                this.movedUp = false;
            }

            this.prevOffGround = offGround;
            this.goingToJump = offGround && this.movedUp && this.movedDown;
            this.stepHeight = 0.0;
            this.step.forEach(height -> {
                if (height > this.stepHeight) {
                    this.stepHeight = height;
                }
            });
            this.reverseHeight = 0.0;
            this.reverseStep.forEach(height -> {
                if (height > this.reverseHeight) {
                    this.reverseHeight = height;
                }
            });
            ExtrapolationSettings settings = ExtrapolationSettings.getInstance();
            this.stepHeight = Math.max(this.stepHeight, settings.minStep.get());
            this.reverseHeight = Math.max(this.reverseHeight, settings.minReverseStep.get());
            this.motionData = ExtrapolationManager.getMotion(this);
            if (this.motionData.reset) {
                this.motions.clear();
            }
        }

        private void stopLag() {
            this.stillFor = ExtrapolationSettings.getInstance().maxLag.get();
        }

        private boolean isOffGround() {
            return this.onGrounds.stream().anyMatch(b -> !b);
        }

        private void handleMotion(Vec3d motion, Entity entity) {
            if (motion.y > 0.0) {
                this.movedUp = true;
            }

            if (motion.y < 0.0 && !entity.isOnGround()) {
                this.movedDown = true;
            }

            this.motionData = ExtrapolationManager.getMotion(this);
            if (!(motion.lengthSquared() < 1.0E-4) || this.stillFor >= ExtrapolationSettings.getInstance().maxLag.get() || entity == BlackOut.mc.player) {
                this.stillFor = 0;
                if (this.handleMotion2(motion, entity)) {
                    this.setTicksSince(0);
                } else {
                    double yVel = MovementPrediction.approximateYVelocity(motion.y, this.getTicksSince(), 1000);

                    for (int startTicks = this.getTicksSince(); this.getTicksSince() > 0; yVel = ExtrapolationManager.gravityMod(yVel)) {
                        this.setTicksSince(this.getTicksSince() - 1);
                        this.addMotion(motion.withAxis(Direction.Axis.Y, yVel).multiply(1.0F / startTicks, 1.0, 1.0F / startTicks));
                    }
                }
            }
        }

        private boolean rotated() {
            return Math.abs(RotationUtils.yawAngle(this.entity.getYaw(), this.prevYaw)) > 5.0 || Math.abs(this.entity.getPitch() - this.prevPitch) > 5.0;
        }

        private boolean handleMotion2(Vec3d motion, Entity entity) {
            if (motion.horizontalLengthSquared() > 3.0) {
                this.addMotion(new Vec3d(0.0, 0.0, 0.0));
                return true;
            } else {
                if (motion.y >= 0.45 && motion.y <= 4.0 && Simulator.isOnGround(entity, entity.getBoundingBox())) {
                    if (SettingUtils.stepPredict()) {
                        this.step.add(motion.y, SettingUtils.stepTicks());
                        this.motions.clear();
                        this.addMotion(new Vec3d(motion.x, 0.0, motion.z));
                        return true;
                    }
                } else if (motion.y <= -0.45 && motion.y >= -6.0 && Simulator.isOnGround(entity, entity.getBoundingBox())) {
                    if (SettingUtils.reverseStepPredict()) {
                        this.reverseStep.add(-motion.y, SettingUtils.reverseStepTicks());
                        this.motions.clear();
                        this.addMotion(new Vec3d(motion.x, 0.0, motion.z));
                        return true;
                    }
                } else if (motion.y > 0.35 && motion.y < 0.45 && !Simulator.isOnGround(entity, entity.getBoundingBox())) {
                    this.jumpHeight = motion.y;
                }

                return false;
            }
        }

        public Box extrapolate(Entity entity, int ticks) {
            return this.extrapolate(entity, ticks, null);
        }

        public Box extrapolate(Entity entity, int ticks, Consumer<Box> consumer) {
            if (ticks == 0) {
                return entity.getBoundingBox();
            } else {
                SimulationContext context = new SimulationContext(entity, this.getExtTicks(ticks), this.jumpHeight, this.motionData.motion, consumer, (c, i) -> {
                    double prevYaw = this.motionYaw(c.motionX, c.motionZ);
                    double motionLength = Math.sqrt(c.motionX * c.motionX + c.motionZ * c.motionZ);
                    double yaw = Math.toRadians(prevYaw + this.motionData.yawDiff + 90.0);
                    this.motionData.yawDiff *= 0.8;
                    c.motionX = Math.cos(yaw) * motionLength;
                    c.motionZ = Math.sin(yaw) * motionLength;
                });
                context.jump = this.goingToJump;
                context.reverseStep = this.reverseHeight;
                context.step = this.stepHeight;
                return Simulator.extrapolate(context);
            }
        }

        private int getExtTicks(int ticks) {
            if (ExtrapolationSettings.getInstance().extraExtrapolation.get() && this.entity != BlackOut.mc.player) {
                int extra;
                if (this.stillFor < ExtrapolationSettings.getInstance().maxLag.get()) {
                    extra = this.stillFor;
                } else {
                    extra = 0;
                }

                return ticks + extra;
            } else {
                return ticks;
            }
        }

        private double motionYaw(double x, double z) {
            return MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(-x, z)));
        }

        private void addMotion(Vec3d motion) {
            if (motion.length() > 0.0 && this.motions.size() > 1 && (this.collided() || this.entity.horizontalCollision)) {
                motion = this.motions.get(1);
            }

            this.motions.addFirst(motion);
            OLEPOSSUtils.limitList(this.motions, 5);
        }

        private boolean collided() {
            Box box = this.entity.getBoundingBox();
            Box newBox = new Box(
                    this.prevPos.x - box.getLengthX() / 2.0,
                    this.prevPos.y,
                    this.prevPos.z - box.getLengthZ() / 2.0,
                    this.prevPos.x + box.getLengthX() / 2.0,
                    this.prevPos.y + box.getLengthY(),
                    this.prevPos.z + box.getLengthZ() / 2.0
            );
            return !BlackOut.mc.world.getEntityCollisions(this.entity, newBox.stretch(this.motions.get(1))).isEmpty();
        }

        public Entity getEntity() {
            return this.entity;
        }

        public int getTicksSince() {
            return this.ticksSince;
        }

        public void setTicksSince(int ticksSince) {
            this.ticksSince = ticksSince;
        }
    }
}
