package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.HoleType;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.interfaces.mixin.IVec3d;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.module.modules.misc.Timer;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.Hole;
import bodevelopment.client.blackout.util.HoleUtils;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import bodevelopment.client.blackout.util.RotationUtils;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class HoleSnap extends Module {
    private static HoleSnap INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgSpeed = this.addGroup("Speed");
    private final SettingGroup sgHole = this.addGroup("Hole");

    private final Setting<Boolean> jump = this.sgGeneral.booleanSetting("Auto Jump", false, "Automatically jumps while traveling toward the hole to avoid getting stuck on small elevation changes.");
    private final Setting<Integer> jumpCooldown = this.sgGeneral.intSetting("Jump Cooldown", 5, 0, 20, 1, "The tick delay between automated jumps.", this.jump::get);
    private final Setting<Boolean> step = this.sgGeneral.booleanSetting("Step Integration", false, "Allows the module to utilize the Step feature for instant vertical movement.");
    private final Setting<Boolean> fastFall = this.sgGeneral.booleanSetting("Fast Fall Integration", false, "Enables faster vertical descent once positioned directly above the target hole.");
    private final Setting<Double> range = this.sgGeneral.doubleSetting("Horizontal Search Range", 3.0, 0.0, 5.0, 0.1, "The maximum horizontal distance to search for a valid hole.");
    private final Setting<Double> downRange = this.sgGeneral.doubleSetting("Vertical Search Range", 3.0, 0.0, 5.0, 0.1, "The maximum depth to search for a valid hole.");
    private final Setting<Integer> maxCollisions = this.sgGeneral.intSetting("Collision Limit", 15, 0, 100, 1, "Automatically disables the module after this many failed movement attempts.");
    private final Setting<Integer> maxRubberbands = this.sgGeneral.intSetting("Rubberband Limit", 1, 0, 100, 1, "Automatically disables the module if the server resets your position this many times.");

    private final Setting<Double> speed = this.sgSpeed.doubleSetting("Travel Speed", 0.2873, 0.0, 1.0, 0.01, "The movement velocity used to reach the hole.");
    private final Setting<Boolean> boost = this.sgSpeed.booleanSetting("Initial Boost", false, "Applies a temporary speed increase at the start of the snap.");
    private final Setting<Double> boostSpeed = this.sgSpeed.doubleSetting("Boost Velocity", 0.5, 0.0, 1.0, 0.01, "The velocity applied during the boost phase.", this.boost::get);
    private final Setting<Integer> boostTicks = this.sgSpeed.intSetting("Boost Duration", 3, 0, 10, 1, "How many ticks the initial boost lasts.", this.boost::get);
    private final Setting<Double> timer = this.sgSpeed.doubleSetting("Timer Override", 1.0, 1.0, 10.0, 0.04, "Increases the game speed during the snapping process for faster completion.");

    private final Setting<Boolean> singleTarget = this.sgHole.booleanSetting("Lock Target", false, "Locks onto the first detected hole and stops searching for others.");
    private final Setting<Integer> depth = this.sgHole.intSetting("Min Hole Depth", 3, 1, 5, 1, "The minimum number of surrounding blocks required for a position to be considered a hole.");
    private final Setting<Boolean> singleHoles = this.sgHole.booleanSetting("Target 1x1", true, "Includes standard 1x1 holes in the search.");
    private final Setting<Boolean> doubleHoles = this.sgHole.booleanSetting("Target 1x2", true, "Includes elongated 1x2 holes in the search.");
    private final Setting<Boolean> quadHoles = this.sgHole.booleanSetting("Target 2x2", true, "Includes large 2x2 holes in the search.");

    private Hole singleHole;
    private int collisions;
    private int rubberbands;
    private int ticks;
    private int boostLeft = 0;

    public HoleSnap() {
        super("Hole Snap", "Automatically maneuvers the player into the nearest safe hole for defensive positioning.", SubCategory.MOVEMENT, true);
        INSTANCE = this;
    }

    public static HoleSnap getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        this.singleHole = this.findHole();
        this.rubberbands = 0;
        this.ticks = 0;
        this.boostLeft = this.boost.get() ? this.boostTicks.get() : 0;
    }

    @Override
    public void onDisable() {
        Timer.reset();
    }

    @Event
    public void onPacket(PacketEvent.Receive.Pre event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket
                && this.maxRubberbands.get() > 0
                && ++this.rubberbands >= this.maxRubberbands.get()
                && this.maxRubberbands.get() > 0) {
            this.disable(this.getDisplayName() + " disabled, rubberbanded " + this.rubberbands + " times", 2, Notifications.Type.Alert);
        }
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null && event.xzValue <= 9) {
            Hole hole = this.singleTarget.get() ? this.singleHole : this.findHole();
            if (hole != null && !this.singleBlocked()) {
                Timer.set(this.timer.get().floatValue());
                double yaw = Math.cos(Math.toRadians(this.getAngle(hole.middle) + 90.0F));
                double pit = Math.sin(Math.toRadians(this.getAngle(hole.middle) + 90.0F));
                if (BlackOut.mc.player.getX() != hole.middle.x || BlackOut.mc.player.getZ() != hole.middle.z) {
                    double x = this.getSpeed() * yaw;
                    double dX = hole.middle.x - BlackOut.mc.player.getX();
                    double z = this.getSpeed() * pit;
                    double dZ = hole.middle.z - BlackOut.mc.player.getZ();
                    if (OLEPOSSUtils.inside(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox().offset(x, 0.0, z))) {
                        this.collisions++;
                        if (this.collisions >= this.maxCollisions.get() && this.maxCollisions.get() > 0) {
                            this.disable(this.getDisplayName() + " disabled, collided " + this.collisions + " times", 2, Notifications.Type.Alert);
                        }
                    } else {
                        this.collisions = 0;
                    }

                    if (this.ticks > 0) {
                        this.ticks--;
                    } else if (OLEPOSSUtils.inside(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox().offset(0.0, -0.05, 0.0)) && this.jump.get()) {
                        this.ticks = this.jumpCooldown.get();
                        event.setY(this, 0.42);
                    }

                    this.boostLeft--;
                    event.setXZ(this, Math.abs(x) < Math.abs(dX) ? x : dX, Math.abs(z) < Math.abs(dZ) ? z : dZ);
                } else if (BlackOut.mc.player.getY() <= hole.middle.y) {
                    this.disable(this.getDisplayName() + " disabled, in hole");
                    ((IVec3d) event.movement).blackout_Client$setXZ(0.0, 0.0);
                } else if (OLEPOSSUtils.inside(BlackOut.mc.player, BlackOut.mc.player.getBoundingBox().offset(0.0, -0.05, 0.0))) {
                    this.disable(this.getDisplayName() + " hole unreachable, disabling", 2, Notifications.Type.Alert);
                } else {
                    event.setXZ(this, 0.0, 0.0);
                }
            } else {
                this.disable("No hole was found disabling " + this.getDisplayName(), 2, Notifications.Type.Alert);
            }
        }
    }

    private boolean singleBlocked() {
        if (!this.singleTarget.get()) {
            return false;
        } else {
            for (BlockPos pos : this.singleHole.positions) {
                if (OLEPOSSUtils.collidable(pos)) {
                    return true;
                }
            }

            return false;
        }
    }

    private Hole findHole() {
        Hole closest = null;
        int r = (int) Math.ceil(this.range.get());

        for (int x = -r; x <= r; x++) {
            for (int y = (int) (-Math.ceil(this.downRange.get())); y < 1; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = OLEPOSSUtils.roundedPos().add(x, y, z);
                    Hole hole = HoleUtils.getHole(pos, this.singleHoles.get(), this.doubleHoles.get(), this.quadHoles.get(), this.depth.get(), true);
                    if (hole.type != HoleType.NotHole) {
                        if (y == 0 && this.inHole(hole)) {
                            return hole;
                        }

                        if (closest == null
                                || hole.middle.subtract(BlackOut.mc.player.getPos()).horizontalLengthSquared()
                                < closest.middle.subtract(BlackOut.mc.player.getPos()).horizontalLengthSquared()) {
                            closest = hole;
                        }
                    }
                }
            }
        }

        return closest;
    }

    private boolean inHole(Hole hole) {
        for (BlockPos pos : hole.positions) {
            if (BlackOut.mc.player.getBlockPos().equals(pos)) {
                return true;
            }
        }

        return false;
    }

    private float getAngle(Vec3d pos) {
        return (float) RotationUtils.getYaw(pos);
    }

    private double getSpeed() {
        return this.boostLeft > 0 ? this.boostSpeed.get() : this.speed.get();
    }

    public boolean shouldStep() {
        return this.enabled && this.step.get();
    }

    public boolean shouldFastFall() {
        return this.enabled && this.fastFall.get();
    }
}
