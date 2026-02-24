package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.MovementUtils;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PacketFly extends Module {
    private static PacketFly INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgFly = this.addGroup("Fly");
    private final SettingGroup sgPhase = this.addGroup("Phase");
    private final Setting<Boolean> onGroundSpoof = this.sgGeneral.booleanSetting("On Ground Spoof", false, "Spoofs on ground.");
    private final Setting<Boolean> onGround = this.sgGeneral.booleanSetting("On Ground", false, "Should we tell the server that you are on ground.");
    private final Setting<BoundsMode> boundsMode = this.sgGeneral.enumSetting("Bounds Mode", BoundsMode.Add, "Spoofs on ground.");
    private final Setting<Boolean> setXZ = this.sgGeneral
            .booleanSetting("Set XZ", false, "Doesn't move horizontally and vertically in the same packet.", () -> this.boundsMode.get() == BoundsMode.Set);
    public final Setting<Integer> xzBound = this.sgGeneral
            .intSetting("XZ Bound", 0, -1337, 1337, 1, "Bounds offset horizontally.", () -> this.boundsMode.get() == BoundsMode.Add || this.setXZ.get());
    private final Setting<Boolean> setY = this.sgGeneral
            .booleanSetting("Set Y", true, "Doesn't move horizontally and vertically in the same packet.", () -> this.boundsMode.get() == BoundsMode.Set);
    public final Setting<Integer> yBound = this.sgGeneral
            .intSetting("Y Bound", -87, -1337, 1337, 1, "Bounds offset vertically.", () -> this.boundsMode.get() == BoundsMode.Add || this.setY.get());
    private final Setting<Boolean> strictVertical = this.sgGeneral.booleanSetting("Strict Vertical", false, "Doesn't move horizontally and vertically in the same packet.");
    private final Setting<Boolean> syncPacket = this.sgGeneral.booleanSetting("Sync Packet", false, ".");
    private final Setting<Boolean> noClip = this.sgGeneral.booleanSetting("No Clip", true, ".");
    private final Setting<Boolean> resync = this.sgGeneral.booleanSetting("Resync", true, ".");
    private final Setting<Double> packets = this.sgFly.doubleSetting("Fly Packets", 1.0, 0.0, 10.0, 0.1, "How many packets to send every movement tick.");
    private final Setting<Double> flySpeed = this.sgFly.doubleSetting("Fly Speed", 0.2873, 0.0, 1.0, 0.001, "Distance to travel each packet.");
    private final Setting<Boolean> fastVertical = this.sgFly.booleanSetting("Fast Vertical Fly", false, "Sends multiple packets every movement tick while going up.");
    private final Setting<Double> downSpeed = this.sgFly.doubleSetting("Fly Down Speed", 0.062, 0.0, 10.0, 0.01, "How fast to fly down.");
    private final Setting<Double> upSpeed = this.sgFly.doubleSetting("Fly Up Speed", 0.062, 0.0, 10.0, 0.01, "How fast to fly up.");
    private final Setting<Boolean> flyEffects = this.sgFly.booleanSetting("Fly Effects", true, ".");
    private final Setting<Boolean> flyRotate = this.sgFly.booleanSetting("Fly Rotate", true, "Allows rotating while phasing.");
    private final Setting<Boolean> stillFlyRotate = this.sgFly.booleanSetting("Still Fly Rotate", true, ".", this.flyRotate::get);
    private final Setting<Boolean> antiKick = this.sgFly.booleanSetting("Anti-Kick", false, "Slowly falls down.");
    private final Setting<Double> antiKickAmount = this.sgFly
            .doubleSetting("Anti-Kick Multiplier", 1.0, 0.0, 10.0, 1.0, "Fall speed multiplier for antikick (0.04 blocks * multiplier).", this.antiKick::get);
    private final Setting<Integer> antiKickDelay = this.sgFly
            .intSetting("Anti-Kick Delay", 10, 1, 100, 1, "Tick delay between moving anti kick packets.", this.antiKick::get);
    private final Setting<Double> phasePackets = this.sgPhase.doubleSetting("Phase Packets", 1.0, 0.0, 10.0, 0.1, "How many packets to send every movement tick.");
    private final Setting<Double> phaseSpeed = this.sgPhase.doubleSetting("Phase Speed", 0.062, 0.0, 1.0, 0.001, "Distance to travel each packet.");
    private final Setting<Boolean> phaseFastVertical = this.sgPhase
            .booleanSetting("Fast Vertical Phase", false, "Sends multiple packets every movement tick while going up.");
    private final Setting<Double> phaseDownSpeed = this.sgPhase.doubleSetting("Phase Down Speed", 0.062, 0.0, 10.0, 0.01, "How fast to phase down.");
    private final Setting<Double> phaseUpSpeed = this.sgPhase.doubleSetting("Phase Up Speed", 0.062, 0.0, 10.0, 0.01, "How fast to phase up.");
    private final Setting<Boolean> phaseEffects = this.sgPhase.booleanSetting("Phase Effects", false, ".");
    private final Setting<Boolean> phaseRotate = this.sgPhase.booleanSetting("Phase Rotate", true, "Allows rotating while phasing.");
    private final Setting<Boolean> stillPhaseRotate = this.sgPhase.booleanSetting("Still Phase Rotate", true, ".", this.phaseRotate::get);
    private final Setting<Boolean> sneakPhase = this.sgPhase.booleanSetting("Sneak Phase", true, ".");
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final List<PlayerMoveC2SPacket> validPackets = new ArrayList<>();
    public boolean moving = false;
    private int ticks = 0;
    private int sent = 0;
    private int rur = 0;
    private double packetsToSend = 0.0;
    private String info = null;
    private Vec3d offset = Vec3d.ZERO;
    private boolean sneaked = false;

    public PacketFly() {
        super("PacketFly", "Flies using packets", SubCategory.MOVEMENT, true);
        INSTANCE = this;
    }

    public static PacketFly getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        this.ticks = 0;
    }

    @Override
    public boolean shouldSkipListeners() {
        return false;
    }

    @Event
    public void onTick(TickEvent.Pre e) {
        if (this.enabled) {
            this.ticks--;
            this.rur++;
            if (this.rur % 20 == 0) {
                this.info = "Packets: " + this.sent;
                this.sent = 0;
            }
        }
    }

    @Override
    public void onDisable() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null && this.resync.get()) {
            Vec3d pos = Managers.PACKET.pos;
            this.sendPacket(
                    new PlayerMoveC2SPacket.Full(pos.x, pos.y + 1.0, pos.z, Managers.ROTATION.prevYaw + 5.0F, Managers.ROTATION.prevPitch, false)
            );
        }
    }

    @Event
    public void onMove(MoveEvent.Pre e) {
        if (this.enabled && BlackOut.mc.player != null && BlackOut.mc.world != null) {
            boolean phasing = this.isPhasing();
            boolean semiPhasing = this.isSemiPhase();
            if (this.noClip.get()) {
                BlackOut.mc.player.noClip = true;
            }

            this.packetsToSend = this.packetsToSend + this.packets(semiPhasing);
            boolean shouldAntiKick = this.antiKick.get() && this.ticks <= 0 && !phasing && !this.onGround();
            double yaw = this.getYaw();
            double motion = this.getSpeed(semiPhasing);
            double x = 0.0;
            double y = 0.0;
            double z = 0.0;
            if (this.jumping()) {
                y = (semiPhasing ? this.phaseUpSpeed : this.upSpeed).get();
            } else if (this.sneaking()) {
                y = -(semiPhasing ? this.phaseDownSpeed : this.downSpeed).get();
            }

            if (this.jumping()) {
                y = semiPhasing ? this.phaseUpSpeed.get() : this.upSpeed.get();
            } else if (this.sneaking()) {
                y = semiPhasing ? -this.phaseDownSpeed.get() : -this.downSpeed.get();
            }

            if (y != 0.0 && this.strictVertical.get()) {
                this.moving = false;
            }

            if (this.moving) {
                x = Math.cos(Math.toRadians(yaw + 90.0)) * motion;
                z = Math.sin(Math.toRadians(yaw + 90.0)) * motion;
            } else {
                if (semiPhasing && !this.phaseFastVertical.get()) {
                    this.packetsToSend = Math.min(this.packetsToSend, 1.0);
                }

                if (!semiPhasing && !this.fastVertical.get()) {
                    this.packetsToSend = Math.min(this.packetsToSend, 1.0);
                }
            }

            this.offset = new Vec3d(0.0, 0.0, 0.0);
            boolean antiKickSent = false;

            while (this.packetsToSend > 0.0) {
                double yOffset;
                if (shouldAntiKick && y >= 0.0 && !antiKickSent) {
                    this.ticks = this.antiKickDelay.get();
                    yOffset = this.antiKickAmount.get() * -0.04;
                    antiKickSent = true;
                } else {
                    yOffset = y;
                }

                this.offset = this.offset.add(x, yOffset, z);
                this.send(this.offset.add(BlackOut.mc.player.getPos()), this.getBounds(), this.getOnGround(), semiPhasing);
                this.packetsToSend--;
                if (x == 0.0 && z == 0.0 && y == 0.0) {
                    break;
                }
            }

            this.doPhase();
            e.set(this, this.offset.x, this.offset.y, this.offset.z);
            this.packetsToSend = Math.min(this.packetsToSend, 1.0);
        }
    }

    private double getSpeed(boolean phasing) {
        Setting<Double> speed = phasing ? this.phaseSpeed : this.flySpeed;
        Setting<Boolean> effects = phasing ? this.phaseEffects : this.flyEffects;
        return effects.get() ? MovementUtils.getSpeed(speed.get()) : speed.get();
    }

    @Event
    public void onSend(PacketEvent.Send event) {
        if (this.enabled) {
            if (event.packet instanceof PlayerMoveC2SPacket) {
                if (!this.validPackets.contains(event.packet)) {
                    event.setCancelled(true);
                } else {
                    this.sent++;
                }
            } else {
                this.sent++;
            }
        }
    }

    private void doPhase() {
        if (this.sneakPhase.get()) {
            if (!this.jumping()) {
                if (this.sneaked) {
                    this.sneaked = false;
                    this.endSneak();
                }
            } else {
                Box standBox = this.boxFor(EntityPose.STANDING, BlackOut.mc.player.getPos())
                        .contract(0.0625, 0.0625, 0.0625)
                        .offset(0.0, this.offset.y * 2.0, 0.0);
                Box movedBox = this.boxFor(EntityPose.STANDING, BlackOut.mc.player.getPos())
                        .contract(0.0625, 0.0, 0.0625)
                        .offset(0.0, this.offset.y * 3.0, 0.0);
                boolean standIn = this.in(standBox);
                boolean movedIn = this.in(movedBox);
                if (this.sneaking()) {
                    if (standIn) {
                        this.endSneak();
                    }
                } else if (movedIn) {
                    this.startSneak();
                }
            }
        }
    }

    private Box boxFor(EntityPose pose, Vec3d vec3d) {
        return PlayerEntity.POSE_DIMENSIONS.getOrDefault(pose, PlayerEntity.STANDING_DIMENSIONS).getBoxAt(vec3d);
    }

    private boolean in(Box box) {
        return OLEPOSSUtils.inside(BlackOut.mc.player, box);
    }

    private void startSneak() {
        this.sneaked = true;
        BlackOut.mc.player.setSneaking(true);
        BlackOut.mc.options.sneakKey.setPressed(true);
    }

    private void endSneak() {
        BlackOut.mc.player.setSneaking(false);
        BlackOut.mc.options.sneakKey.setPressed(false);
    }

    @Override
    public String getInfo() {
        return this.info;
    }

    private boolean onGround() {
        return BlackOut.mc.player.isOnGround()
                || BlackOut.mc.player.getBlockY() - BlackOut.mc.player.getY() == 0.0
                && OLEPOSSUtils.collidable(BlackOut.mc.player.getBlockPos().down());
    }

    private double packets(boolean semiPhasing) {
        return (semiPhasing ? this.phasePackets : this.packets).get();
    }

    private Vec3d getBounds() {
        double yaw = this.random.nextDouble(0.0, Math.PI * 2);
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        switch (this.boundsMode.get()) {
            case Add:
                x = BlackOut.mc.player.getX() + (Math.cos(yaw) * this.xzBound.get());
                y = BlackOut.mc.player.getY() + this.yBound.get();
                z = BlackOut.mc.player.getZ() + Math.sin(yaw) * this.xzBound.get();
                break;
            case Set:
                if (this.setXZ.get()) {
                    x = Math.cos(yaw) * this.xzBound.get();
                    z = Math.sin(yaw) * this.xzBound.get();
                } else {
                    x = BlackOut.mc.player.getX();
                    z = BlackOut.mc.player.getZ();
                }

                if (this.setY.get()) {
                    y = this.yBound.get();
                } else {
                    y = BlackOut.mc.player.getY();
                }
        }

        return new Vec3d(x, y, z);
    }

    private boolean getOnGround() {
        return this.onGroundSpoof.get() ? this.onGround.get() : BlackOut.mc.player.isOnGround();
    }

    private boolean isPhasing() {
        return OLEPOSSUtils.inside(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox().contract(0.0625, 0.0, 0.0625));
    }

    private boolean isSemiPhase() {
        return OLEPOSSUtils.inside(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox().expand(0.01, 0.0, 0.01));
    }

    private boolean jumping() {
        return BlackOut.mc.options.jumpKey.isPressed();
    }

    private boolean sneaking() {
        return BlackOut.mc.options.sneakKey.isPressed();
    }

    private void send(Vec3d pos, Vec3d bounds, boolean onGround, boolean phasing) {
        PlayerMoveC2SPacket normal = this.getPacket(pos, onGround, phasing);
        PlayerMoveC2SPacket.PositionAndOnGround bound = new PlayerMoveC2SPacket.PositionAndOnGround(bounds.x, bounds.y, bounds.z, onGround);
        this.validPackets.add(normal);
        this.sendPacket(normal);
        this.validPackets.add(bound);
        this.sendPacket(bound);
        this.sendPacket(Managers.PACKET.incrementedPacket(pos));
        if (this.syncPacket.get()) {
            Managers.PACKET.sendPositionSync(pos, Managers.ROTATION.prevYaw, Managers.ROTATION.prevPitch);
        }
    }

    private PlayerMoveC2SPacket getPacket(Vec3d pos, boolean onGround, boolean phasing) {
        if (!this.shouldRotate(phasing ? this.phaseRotate : this.flyRotate, phasing ? this.stillPhaseRotate : this.stillFlyRotate)) {
            return this.onlyMove(pos, onGround);
        } else {
            Managers.ROTATION.updateNext();
            if (!Managers.ROTATION.rotated()) {
                return this.onlyMove(pos, onGround);
            } else {
                float yaw = Managers.ROTATION.nextYaw;
                float pitch = Managers.ROTATION.nextPitch;
                return new PlayerMoveC2SPacket.Full(pos.x, pos.y, pos.z, yaw, pitch, onGround);
            }
        }
    }

    private boolean shouldRotate(Setting<Boolean> rot, Setting<Boolean> still) {
        return rot.get() && (!still.get() || this.offset.length() < 0.01);
    }

    private PlayerMoveC2SPacket.PositionAndOnGround onlyMove(Vec3d vec3d, boolean ong) {
        return new PlayerMoveC2SPacket.PositionAndOnGround(vec3d.x, vec3d.y, vec3d.z, ong);
    }

    private double getYaw() {
        double f = BlackOut.mc.player.input.movementForward;
        double s = BlackOut.mc.player.input.movementSideways;
        double yaw = BlackOut.mc.player.getYaw();
        if (f > 0.0) {
            this.moving = true;
            yaw += s > 0.0 ? -45.0 : (s < 0.0 ? 45.0 : 0.0);
        } else if (f < 0.0) {
            this.moving = true;
            yaw += s > 0.0 ? -135.0 : (s < 0.0 ? 135.0 : 180.0);
        } else {
            this.moving = s != 0.0;
            yaw += s > 0.0 ? -90.0 : (s < 0.0 ? 90.0 : 0.0);
        }

        return yaw;
    }

    public enum BoundsMode {
        Add,
        Set
    }
}
