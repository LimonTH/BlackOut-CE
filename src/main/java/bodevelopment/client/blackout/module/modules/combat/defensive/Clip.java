package bodevelopment.client.blackout.module.modules.combat.defensive;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.movement.PacketFly;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.Pair;
import bodevelopment.client.blackout.util.MovementUtils;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Clip extends Module {
    private static Clip INSTANCE;

    public final SettingGroup sgGeneral = this.addGroup("General");
    public final SettingGroup sgBounds = this.addGroup("Bounds");

    public final Setting<Boolean> stopRotation = this.sgGeneral.booleanSetting("Stop Rotation", true,
            "Disables player rotations while clipping to prevent desync from the corner.");
    private final Setting<OffsetMode> offset = this.sgGeneral.enumSetting("Offset", OffsetMode.NODamage,
            "The depth of the clip. 'Damage' is deeper but might trigger some anti-cheats.");
    private final Setting<Integer> movementDelay = this.sgGeneral.intSetting("Movement Delay", 10, 0, 20, 1,
            "Ticks to wait between micro-movements into the corner.");
    private final Setting<Double> movement = this.sgGeneral.doubleSetting("Movement", 0.06, 0.0, 0.1, 0.001,
            "The distance (in blocks) to move towards the target corner per step.");
    private final Setting<Boolean> pauseMove = this.sgGeneral.booleanSetting("Pause Move", true,
            "Stops clipping if you are manually trying to move using WASD.");
    private final Setting<Integer> stillTicks = this.sgGeneral.intSetting("Still Ticks", 5, 0, 50, 1,
            "Amount of time to remain stationary before starting the wall-clipping process.");

    private final Setting<Boolean> bounds = this.sgBounds.booleanSetting("Bounds", true,
            "Sends out-of-bounds packets to trick the server into accepting your glitched position.");
    private final Setting<Boolean> spamBounds = this.sgBounds.booleanSetting("Spam Bounds", false,
            "Sends boundary packets every tick for more aggressive clipping (higher flag risk).");
    private final Setting<PacketFly.BoundsMode> boundsMode = this.sgBounds.enumSetting("Bounds Mode", PacketFly.BoundsMode.Add,
            "The mathematical method for calculating out-of-bounds packets.");
    private final Setting<Boolean> setXZ = this.sgBounds.booleanSetting("Set XZ", false,
            "Hard-sets the horizontal coordinates for bounds packets.", () -> this.boundsMode.get() == PacketFly.BoundsMode.Set);
    public final Setting<Integer> xzBound = this.sgBounds.intSetting("XZ Bound", 0, -1337, 1337, 1,
            "Horizontal distance for the 'Bounds' packets.", () -> this.boundsMode.get() == PacketFly.BoundsMode.Add || this.setXZ.get());
    private final Setting<Boolean> setY = this.sgBounds.booleanSetting("Set Y", true,
            "Hard-sets the vertical coordinate for bounds packets.", () -> this.boundsMode.get() == PacketFly.BoundsMode.Set);
    public final Setting<Integer> yBound = this.sgBounds.intSetting("Y Bound", -87, -1337, 1337, 1,
            "Vertical distance for the 'Bounds' packets.", () -> this.boundsMode.get() == PacketFly.BoundsMode.Add || this.setY.get());

    public boolean shouldCancel = false;
    public int noRotateTime = 0;
    private double cornerX = 0.0;
    private double cornerZ = 0.0;
    private int ticksStill = 0;
    private int timer = 0;
    private boolean shouldBounds = false;

    public Clip() {
        super("Clip", "Precisely adjusts your position in corners to minimize explosion damage.", SubCategory.DEFENSIVE, true);
        INSTANCE = this;
    }

    public static Clip getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        this.timer = 0;
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        this.shouldCancel = false;
        this.noRotateTime--;
        double currentX = BlackOut.mc.player.getX() - BlackOut.mc.player.getBlockX();
        double currentZ = BlackOut.mc.player.getZ() - BlackOut.mc.player.getBlockZ();
        if (!this.pauseMove.get() || BlackOut.mc.player.input.movementForward == 0.0F && BlackOut.mc.player.input.movementSideways == 0.0F) {
            if (!this.findCorner(
                    currentX,
                    currentZ,
                    new BlockPos(BlackOut.mc.player.getBlockX(), (int) Math.round(BlackOut.mc.player.getY()), BlackOut.mc.player.getBlockZ())
            )) {
                double targetX = MathHelper.lerp(this.cornerX, 0.3, 0.7);
                double targetZ = MathHelper.lerp(this.cornerZ, 0.3, 0.7);
                double centerX = currentX - 0.5;
                double centerZ = currentZ - 0.5;
                if (Math.abs(centerX) >= 0.19999
                        && Math.signum(centerX) == Math.signum(targetX - 0.5)
                        && Math.abs(centerZ) >= 0.19999
                        && Math.signum(centerZ) == Math.signum(targetZ - 0.5)) {
                    this.wallMove(event, targetX, targetZ, currentX, currentZ);
                } else {
                    this.outMove(event, targetX, targetZ, currentX, currentZ);
                }
            }
        }
    }

    @Event
    public void onTickPost(TickEvent.Post event) {
        if (this.shouldBounds) {
            this.shouldBounds = false;
            if (this.bounds.get()) {
                this.sendBounds();
            }
        }
    }

    private void wallMove(MoveEvent.Pre event, double targetX, double targetZ, double currentX, double currentZ) {
        if (this.ticksStill-- > 0) {
            this.noRotateTime = 5;
            event.setXZ(this, 0.0, 0.0);
        } else {
            double depth = this.offset.get().depth;
            targetX -= Math.signum(0.5 - targetX) * depth;
            targetZ -= Math.signum(0.5 - targetZ) * depth;
            double yaw = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(targetZ - currentZ, targetX - currentX)) - 90.0);
            double x = Math.cos(Math.toRadians(yaw + 90.0));
            double z = Math.sin(Math.toRadians(yaw + 90.0));
            double dx = currentX - targetX;
            double dz = currentZ - targetZ;
            double dist = Math.min(Math.sqrt(dx * dx + dz * dz), this.movement.get());
            double mx = dist * x;
            double mz = dist * z;
            if (dist > 0.001) {
                this.noRotateTime = 5;
                if (--this.timer < 0) {
                    this.timer = this.movementDelay.get();
                    double ox = BlackOut.mc.player.getBlockX() + currentX + mx;
                    double oz = BlackOut.mc.player.getBlockZ() + currentZ + mz;
                    BlackOut.mc.player.setPos(ox, BlackOut.mc.player.getY(), oz);
                    this.sendPos(ox, oz, true);
                    event.setXZ(this, 0.0, 0.0);
                    this.shouldBounds = true;
                    this.shouldCancel = true;
                } else {
                    this.shouldBounds = this.spamBounds.get();
                }
            } else {
                event.setXZ(this, 0.0, 0.0);
            }
        }
    }

    private void outMove(MoveEvent.Pre event, double targetX, double targetZ, double currentX, double currentZ) {
        this.ticksStill = this.stillTicks.get();
        double yaw = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(targetZ - currentZ, targetX - currentX)) - 90.0);
        double x = Math.cos(Math.toRadians(yaw + 90.0));
        double z = Math.sin(Math.toRadians(yaw + 90.0));
        double dx = currentX - targetX;
        double dz = currentZ - targetZ;
        double dist = Math.min(Math.sqrt(dx * dx + dz * dz), MovementUtils.getSpeed(0.2873));
        event.setXZ(this, dist * x, dist * z);
    }

    private void sendBounds() {
        Vec3d bounds = this.getBounds();
        this.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(bounds.x, bounds.y, bounds.z, Managers.PACKET.isOnGround()));
    }

    private Vec3d getBounds() {
        double yaw = ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0;
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        switch (this.boundsMode.get()) {
            case Add:
                x = BlackOut.mc.player.getX() + Math.cos(yaw) * this.xzBound.get();
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

    private void sendPos(double x, double z, boolean onGround) {
        this.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, BlackOut.mc.player.getY(), z, onGround));
    }

    private boolean findCorner(double x, double z, BlockPos pos) {
        boolean minX = !OLEPOSSUtils.replaceable(pos.offset(Direction.WEST));
        boolean minZ = !OLEPOSSUtils.replaceable(pos.offset(Direction.NORTH));
        boolean maxX = !OLEPOSSUtils.replaceable(pos.offset(Direction.EAST));
        boolean maxZ = !OLEPOSSUtils.replaceable(pos.offset(Direction.SOUTH));
        List<Pair<Double, Double>> corners = new ArrayList<>();
        if (minX) {
            if (minZ) {
                corners.add(new Pair<>(0.0, 0.0));
            }

            if (maxZ) {
                corners.add(new Pair<>(0.0, 1.0));
            }
        }

        if (maxX) {
            if (minZ) {
                corners.add(new Pair<>(1.0, 0.0));
            }

            if (maxZ) {
                corners.add(new Pair<>(1.0, 1.0));
            }
        }

        if (corners.isEmpty()) {
            return true;
        } else {
            double distC = 1000.0;

            for (Pair<Double, Double> pair : corners) {
                double dx = x - pair.getLeft();
                double dz = z - pair.getRight();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist < distC) {
                    distC = dist;
                    this.set(pair.getLeft(), pair.getRight());
                }
            }

            return false;
        }
    }

    private void set(double x, double z) {
        this.cornerX = x;
        this.cornerZ = z;
    }

    public enum Mode {
        Move,
        Manual
    }

    public enum OffsetMode {
        Damage(0.0624),
        NODamage(0.059),
        Semi(0.03),
        NewVer(1.0E-8);

        private final double depth;

        OffsetMode(double depth) {
            this.depth = depth;
        }
    }
}
