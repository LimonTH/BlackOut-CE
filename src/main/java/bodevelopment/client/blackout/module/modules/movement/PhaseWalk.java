package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.misc.Timer;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.MovementPrediction;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class PhaseWalk extends Module {
    private static PhaseWalk INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Boolean> useTimer = this.sgGeneral.booleanSetting("Timer Synchronization", false, "Engages the client-side timer to accelerate packet throughput during phasing maneuvers.");
    public final Setting<Double> timer = this.sgGeneral.doubleSetting("Timer Multiplier", 1.5, 0.0, 10.0, 0.1, "The factor by which game speed is increased.", this.useTimer::get);
    private final Setting<Boolean> stopRotation = this.sgGeneral.booleanSetting("Rotation Lock", true, "Prevents look-direction updates during sensitive phase transitions to avoid server-side setbacks.");
    private final Setting<Integer> preTicks = this.sgGeneral.intSetting("Transition Lead", 1, 0, 20, 1, "The number of ticks to wait for rotations to stabilize before initiating movement.", this.stopRotation::get);
    private final Setting<Integer> postTicks = this.sgGeneral.intSetting("Transition Recovery", 1, 0, 20, 1, "The number of ticks to remain locked after movement to ensure the server processes the phase.", this.stopRotation::get);
    private final Setting<Boolean> syncPacket = this.sgGeneral.booleanSetting("Force Sync", true, "Transmits a coordinate verification packet to force the server to acknowledge the new position.");
    private final Setting<Boolean> phasedCheck = this.sgGeneral.booleanSetting("Collision Validation", true, "Only applies phase logic when the player's bounding box is actively intersecting a block.");
    private final Setting<Boolean> onGroundCheck = this.sgGeneral.booleanSetting("Ground Validation", true, "Restricts phasing movement to when the player is making contact with the ground.");
    private final Setting<Boolean> descend = this.sgGeneral.booleanSetting("Auto Descend", true, "Enables specialized vertical phasing through floors when sneaking.");
    private final Setting<Boolean> ascend = this.sgGeneral.booleanSetting("Auto Ascend", true, "Enables specialized vertical phasing through ceilings when jumping.");
    private final Setting<Boolean> stopWait = this.sgGeneral.booleanSetting("Static Wait", true, "Completely halts horizontal velocity during the rotation lead-in phase.");
    private final Setting<Boolean> resync = this.sgGeneral.booleanSetting("Position Resync", true, "Sends a final boundary-reset packet upon disabling to prevent rubberbands.");

    private boolean waitingForPhase = false;
    private int sincePhase = 0;
    private int sinceRotation = 0;
    private boolean ignore = false;
    private boolean setTimer = false;
    private boolean descending = false;
    private boolean ascending = false;
    private double ascendY = 0.0;
    private int ascendProgress = 0;
    private int timerLeft = 0;
    private boolean prevActive = false;
    private boolean rubberbanded = false;

    public PhaseWalk() {
        super("Phase Walk", "Allows the player to travel through solid blocks by synchronizing small movement increments with boundary-reset packets.", SubCategory.MOVEMENT, true);
        INSTANCE = this;
    }

    public static PhaseWalk getInstance() {
        return INSTANCE;
    }

    @Override
    public void onDisable() {
        if (this.setTimer) {
            this.setTimer = false;
            Timer.reset();
        }

        if (BlackOut.mc.player != null && BlackOut.mc.world != null && this.prevActive && this.resync.get()) {
            this.timerLeft = 0;
            this.sincePhase = 0;
            Vec3d pos = Managers.PACKET.pos;
            this.sendPacket(
                    new PlayerMoveC2SPacket.Full(pos.x, pos.y + 1.0, pos.z, Managers.ROTATION.prevYaw + 5.0F, Managers.ROTATION.prevPitch, false)
            );
        }
    }

    @Event
    public void onSent(PacketEvent.Sent event) {
        if (event.packet instanceof PlayerMoveC2SPacket packet && packet.changesLook() && !this.ignore) {
            this.sinceRotation = 0;
        }
    }

    @Event
    public void onReceive(PacketEvent.Received event) {
        this.descending = false;
        this.ascending = false;
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        this.move(event);
        if (this.useTimer.get() && --this.timerLeft >= 0) {
            this.setTimer = true;
            Timer.set(this.timer.get().floatValue());
        } else if (this.setTimer) {
            this.setTimer = false;
            Timer.reset();
        }
    }

    private void move(MoveEvent.Pre event) {
        this.sincePhase++;
        this.sinceRotation++;
        if (!PacketFly.getInstance().enabled) {
            this.waitingForPhase = false;
            boolean phasing = OLEPOSSUtils.inside(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox().contract(0.0625));
            if (!phasing) {
                if (this.prevActive && this.rubberbanded && this.resync.get()) {
                    this.timerLeft = 0;
                    this.sincePhase = 0;
                    Vec3d pos = Managers.PACKET.pos;
                    this.sendPacket(
                            new PlayerMoveC2SPacket.Full(pos.x, pos.y + 1.0, pos.z, Managers.ROTATION.prevYaw + 5.0F, Managers.ROTATION.prevPitch, false)
                    );
                }

                this.rubberbanded = false;
            }

            this.prevActive = phasing;
            if (!this.preMovement(event)) {
                if (this.stopRotation.get()) {
                    this.waitingForPhase = true;
                    if (this.sinceRotation < this.preTicks.get()) {
                        if (this.stopWait.get()) {
                            BlackOut.mc.player.setVelocity(new Vec3d(0.0, BlackOut.mc.player.getVelocity().y, 0.0));
                            event.setXZ(this, 0.0, 0.0);
                        }

                        return;
                    }

                    this.waitingForPhase = false;
                    this.sincePhase = 0;
                }

                this.handleMovement(event);
            }
        }
    }

    private boolean preMovement(MoveEvent.Pre event) {
        this.handleInput();
        if (this.descending) {
            return this.preDescend(event);
        } else {
            return this.ascending ? this.preAscend(event) : this.preXZ(event);
        }
    }

    private void handleMovement(MoveEvent.Pre event) {
        if (this.descending) {
            this.handleDescend(event);
        } else if (this.ascending) {
            this.handleAscend(event);
        } else {
            this.handleXZ(event);
        }
    }

    private void handleInput() {
        if (!this.descending && !this.ascending) {
            if (this.canVertical()) {
                if (BlackOut.mc.options.sneakKey.isPressed() && this.descend.get()) {
                    this.descending = true;
                } else if (BlackOut.mc.options.jumpKey.isPressed() && this.ascend.get()) {
                    this.ascending = true;
                    this.ascendProgress = 0;
                    this.ascendY = BlackOut.mc.player.getY();
                }
            }
        }
    }

    private boolean preDescend(MoveEvent.Pre event) {
        return false;
    }

    private boolean preAscend(MoveEvent.Pre event) {
        return false;
    }

    private boolean canVertical() {
        return this.phased() && this.isOnGround();
    }

    private boolean preXZ(MoveEvent.Pre event) {
        Vec3d vec = MovementPrediction.predict(BlackOut.mc.player);
        Vec3d vec2 = BlackOut.mc.player.getPos().add(event.movement);
        double d = vec.subtract(vec2).horizontalLength();
        double d2 = vec.subtract(BlackOut.mc.player.getPos()).horizontalLength();
        if (this.phasedCheck.get() && !this.phased()) {
            return true;
        } else {
            return this.onGroundCheck.get() && !this.isOnGround() || d <= 0.01 || d2 >= 0.05;
        }
    }

    private void handleDescend(MoveEvent.Pre event) {
        Vec3d vec3 = BlackOut.mc.player.getPos().add(0.0, -0.0253, 0.0);
        this.sendBounds(vec3);
        BlackOut.mc.player.setPosition(vec3);
        event.set(this, 0.0, 0.0, 0.0);
        this.descending = false;
    }

    private void handleAscend(MoveEvent.Pre event) {
        double offset = Math.min(this.ascendProgress * 0.06, 1.0);
        Vec3d vec = BlackOut.mc.player.getPos().withAxis(Direction.Axis.Y, this.ascendY + offset);
        this.sendBounds(vec);
        BlackOut.mc.player.setPosition(vec);
        event.set(this, 0.0, 0.0, 0.0);
        this.ascendProgress++;
        if (offset >= 1.0) {
            this.ascending = false;
        }
    }

    private void handleXZ(MoveEvent.Pre event) {
        double ratio = event.movement.horizontalLength() / 0.06;
        double x = event.movement.x / ratio;
        double z = event.movement.z / ratio;
        Vec3d vec3 = BlackOut.mc
                .player
                .getPos()
                .add(x, event.movement.y, z)
                .withAxis(Direction.Axis.Y, BlackOut.mc.player.getY());
        this.sendBounds(vec3);
        BlackOut.mc.player.setPosition(vec3);
        event.setXZ(this, 0.0, 0.0);
    }

    private boolean phased() {
        return OLEPOSSUtils.inside(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox().contract(0.07, 0.1, 0.07));
    }

    private boolean isOnGround() {
        return BlackOut.mc.player.isOnGround();
    }

    private void sendBounds(Vec3d to) {
        this.rubberbanded = true;
        this.timerLeft = 5;
        Managers.PACKET.sendInstantly(new PlayerMoveC2SPacket.PositionAndOnGround(to.x, to.y, to.z, Managers.PACKET.isOnGround()));
        Managers.PACKET.sendInstantly(new PlayerMoveC2SPacket.PositionAndOnGround(to.x, to.y - 87.0, to.z, Managers.PACKET.isOnGround()));
        Managers.PACKET.sendInstantly(Managers.PACKET.incrementedPacket(to));
        this.ignore = true;
        if (this.syncPacket.get()) {
            Managers.PACKET.sendPositionSync(to, Managers.ROTATION.prevYaw, Managers.ROTATION.prevPitch);
        }

        this.ignore = false;
    }

    public boolean shouldStopRotation() {
        if (!this.stopRotation.get()) {
            return false;
        } else {
            return this.waitingForPhase || this.sincePhase <= this.postTicks.get();
        }
    }
}
