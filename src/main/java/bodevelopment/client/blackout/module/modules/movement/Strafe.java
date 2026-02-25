package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.misc.Timer;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import bodevelopment.client.blackout.util.ChatUtils;
import bodevelopment.client.blackout.util.FileUtils;
import bodevelopment.client.blackout.util.MovementUtils;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.mutable.MutableDouble;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Strafe extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgFastFall = this.addGroup("Fast Fall");
    private final SettingGroup sgTimer = this.addGroup("Timer");
    private final SettingGroup sgLimit = this.addGroup("Speed Limits");
    private final SettingGroup sgFriction = this.addGroup("Friction");
    private final SettingGroup sgDamageBoost = this.addGroup("Damage Boost");
    private final SettingGroup sgPause = this.addGroup("Pause");

    public final Setting<Speed.LiquidMode> pauseWater = this.sgPause.enumSetting("Water Interaction", Speed.LiquidMode.Touching, "Behavioral state when in contact with water.");
    public final Setting<Speed.LiquidMode> pauseLava = this.sgPause.enumSetting("Lava Interaction", Speed.LiquidMode.Touching, "Behavioral state when in contact with lava.");

    private final Setting<Boolean> useOffsets = this.sgGeneral.booleanSetting("Coordinate Offsets", false, "Applies pre-defined movement values from the external strafe-offsets.txt configuration.");
    private final Setting<Boolean> offsetEffects = this.sgGeneral.booleanSetting("Scale Offsets", true, "Multiplies configured offsets based on active status effects like Speed or Slowness.", this.useOffsets::get);
    private final Setting<Boolean> strictCollisions = this.sgGeneral.booleanSetting("Collision Reset", true, "Immediately recalculates velocity vectors upon making contact with a horizontal block face.");
    private final Setting<Boolean> onlyOG = this.sgGeneral.booleanSetting("Ground Restricted", false, "Only applies strafing logic while the player is making contact with the ground.");
    private final Setting<Double> jumpPower = this.sgGeneral.doubleSetting("Launch Force", 0.42, 0.0, 1.0, 0.01, "The initial vertical velocity applied during an automated jump.");
    private final Setting<Double> boost = this.sgGeneral.doubleSetting("Base Impulse", 0.36, 0.0, 1.0, 0.01, "The core horizontal velocity added during a jump maneuver.");
    private final Setting<Boolean> resetBoost = this.sgGeneral.booleanSetting("Momentum Reset", true, "Clears existing horizontal velocity before applying a new jump boost.");
    private final Setting<Double> boostMulti = this.sgGeneral.doubleSetting("Jump Multiplier", 1.6, 0.0, 5.0, 0.05, "The factor by which horizontal speed is increased at the peak of a jump.");
    private final Setting<Double> boostDiv = this.sgGeneral.doubleSetting("Landing Friction", 1.6, 0.0, 5.0, 0.05, "The factor by which speed is reduced upon the first airborne tick to stabilize movement.");
    private final Setting<Double> effectMultiplier = this.sgGeneral.doubleSetting("Status Scaling", 1.0, 0.0, 2.0, 0.02, "Adjusts how significantly movement potions impact the final velocity.");
    private final Setting<Boolean> instantStop = this.sgGeneral.booleanSetting("Zero Inertia", false, "Forces all horizontal momentum to zero when movement inputs are released.");

    private final Setting<Boolean> fastFall = this.sgFastFall.booleanSetting("Downward Acceleration", false, "Artificially increases falling speed after a set airborne duration.");
    private final Setting<Integer> jumpTicks = this.sgFastFall.intSetting("Airborne Threshold", 3, 0, 20, 1, "The number of ticks spent in the air before Fast Fall initiates.");
    private final Setting<Integer> fallSpeed = this.sgFastFall.intSetting("Terminal Velocity", 0, 0, 10, 1, "The downward force applied during a fast fall.");
    private final Setting<Boolean> spoofOG = this.sgFastFall.booleanSetting("Ground Spoofing", false, "Transmits a false on-ground state to the server to reset fall distance or momentum.");
    private final Setting<Boolean> stopFall = this.sgFastFall.booleanSetting("Vertical Brake", false, "Halts horizontal movement entirely during a fast fall execution.");

    private final Setting<Boolean> useTimer = this.sgTimer.booleanSetting("Tick Manipulation", false, "Modifies the client clock speed to optimize movement packet throughput.");
    private final Setting<Boolean> ncpTimer = this.sgTimer.booleanSetting("Start NCP Clock", true, "Uses the standard 1.088 NoCheatPlus timer value at the start of movement.", this.useTimer::get);
    private final Setting<Double> timer = this.sgTimer.doubleSetting("Initial Clock", 1.08, 0.5, 2.0, 0.01, "The custom starting timer value.", () -> this.useTimer.get() && !this.ncpTimer.get());
    private final Setting<Boolean> endNcpTimer = this.sgTimer.booleanSetting("End NCP Clock", true, "Transitions to the 1.088 timer value at the end of the movement cycle.", this.useTimer::get);
    private final Setting<Double> endTimer = this.sgTimer.doubleSetting("Terminal Clock", 1.08, 0.5, 2.0, 0.01, "The custom ending timer value.", () -> this.useTimer.get() && !this.endNcpTimer.get());
    private final Setting<Integer> timerStartProgress = this.sgTimer.intSetting("Timer Lead-In", 0, 0, 40, 1, "The tick progress required to start the timer transition.");
    private final Setting<Integer> timerEndProgress = this.sgTimer.intSetting("Timer Lead-Out", 10, 0, 40, 1, "The tick progress at which the timer transition completes.");

    private final Setting<Boolean> ncpSpeed = this.sgLimit.booleanSetting("NCP Floor", true, "Enforces the minimum velocity required to bypass NCP strafe checks.");
    private final Setting<Double> minSpeed = this.sgLimit.doubleSetting("Minimum Velocity", 0.3, 0.0, 1.0, 0.01, "The lowest horizontal speed the module will allow.", () -> !this.ncpSpeed.get());
    private final Setting<Double> maxSpeed = this.sgLimit.doubleSetting("Velocity Ceiling", 0.0, 0.0, 5.0, 0.05, "The absolute maximum horizontal speed allowed (0 for unlimited).");
    private final Setting<Boolean> effectMaxSpeed = this.sgLimit.booleanSetting("Status Cap", true, "Applies speed status effects to the maximum velocity limit.", () -> this.maxSpeed.get() > 0.0);
    private final Setting<Double> maxDamageBoost = this.sgLimit.doubleSetting("Kinetic Cap", 0.5, 0.0, 5.0, 0.05, "The maximum velocity gain permitted from external damage or explosions.");

    private final Setting<Boolean> vanillaFriction = this.sgFriction.booleanSetting("Standard Friction", true, "Uses the default Minecraft physics for momentum decay.");
    private final Setting<Double> startFriction = this.sgFriction.doubleSetting("Initial Momentum", 0.98, 0.9, 1.0, 0.001, "The friction coefficient at the start of the strafe cycle.", () -> !this.vanillaFriction.get());
    private final Setting<Double> endFriction = this.sgFriction.doubleSetting("Terminal Momentum", 0.98, 0.9, 1.0, 0.001, "The friction coefficient at the end of the strafe cycle.", () -> !this.vanillaFriction.get());
    private final Setting<Integer> startProgress = this.sgFriction.intSetting("Friction Lead-In", 0, 0, 40, 1, "Ticks required to begin interpolating friction.", () -> !this.vanillaFriction.get());
    private final Setting<Integer> endProgress = this.sgFriction.intSetting("Friction Lead-Out", 10, 0, 40, 1, "Ticks at which interpolation to Terminal Momentum is complete.", () -> !this.vanillaFriction.get());

    private final Setting<Boolean> damageBoost = this.sgDamageBoost.booleanSetting("Kinetic Amplification", false, "Converts incoming damage knockback into forward horizontal velocity.");
    private final Setting<Boolean> addBoost = this.sgDamageBoost.booleanSetting("Additive Impulse", false, "Sums damage boost with current velocity rather than taking the maximum value.");
    private final Setting<Boolean> stackingBoost = this.sgDamageBoost.booleanSetting("Impulse Stacking", false, "Allows multiple instances of damage to accumulate velocity simultaneously.");
    private final Setting<Boolean> directionalBoost = this.sgDamageBoost.booleanSetting("Vector Filtering", true, "Only applies damage boosts that align with the player's current movement vector.");
    private final Setting<Double> boostFactor = this.sgDamageBoost.doubleSetting("Impulse Multiplier", 1.0, 0.0, 5.0, 0.05, "The strength of the velocity gain from incoming damage.");
    private final Setting<Double> boostTime = this.sgDamageBoost.doubleSetting("Impulse Duration", 0.5, 0.0, 2.0, 0.02, "The time window during which a damage boost remains active.");
    private final Setting<Integer> latencyTicks = this.sgDamageBoost.intSetting("Velocity Buffer", 0, 0, 10, 1, "Compensates for network latency by using historical velocity data for boost calculation.");

    private final Setting<Boolean> pauseSneak = this.sgPause.booleanSetting("Inhibit on Sneak", true, "Suspends movement logic while sneaking.");
    private final Setting<Boolean> pauseElytra = this.sgPause.booleanSetting("Inhibit on Flight", true, "Suspends movement logic while flying with an Elytra.");
    private final Setting<Boolean> pauseFly = this.sgPause.booleanSetting("Inhibit on Creative", true, "Suspends movement logic while in creative flight.");

    private final List<Offset> offsets = new ArrayList<>();
    private final TimerList<Double> boosts = new TimerList<>(true);
    private final List<Vec3d> velocities = new ArrayList<>();
    private double velocity = 0.0;
    private double yaw = 0.0;
    private int og = 0;
    private Vec3d prevMovement = Vec3d.ZERO;
    private boolean vanillaBoosted = false;
    private long prevRubberband = 0L;
    private boolean waiting = false;
    private boolean setTimer = false;
    private double boostAmount = 0.0;
    public Strafe() {
        super("Strafe", "Enhances airborne and ground-based movement by manipulating air friction, jump impulses, and kinetic damage feedback.", SubCategory.MOVEMENT, true);
    }

    @Override
    public void onEnable() {
        this.offsets.clear();
        this.velocity = this.prevMovement.horizontalLength();
        if (this.useOffsets.get()) {
            File file = FileUtils.getFile("strafe-offsets.txt");
            this.waiting = true;

            try {
                if (file.exists()) {
                    FileUtils.readString(file).lines().forEach(line -> {
                        String[] c = line.split(", ");
                        this.offsets.add(new Offset(Float.parseFloat(c[0]), Float.parseFloat(c[1]), c[0].equals("-") ? null : Boolean.parseBoolean(c[2])));
                    });
                }
            } catch (Exception e) {
                ChatUtils.addMessage("error in reading strafe-offsets.txt");
            }
        }
    }

    @Override
    public void onDisable() {
        if (this.useTimer.get()) {
            Timer.reset();
        }
    }

    @Override
    public boolean shouldSkipListeners() {
        return false;
    }

    @Event
    public void onTick(TickEvent.Post event) {
        if (BlackOut.mc.player != null || BlackOut.mc.world != null) {
            this.prevMovement = BlackOut.mc
                    .player
                    .getPos()
                    .subtract(BlackOut.mc.player.prevX, BlackOut.mc.player.prevY, BlackOut.mc.player.prevZ);
            this.velocities.addFirst(this.prevMovement);
            OLEPOSSUtils.limitList(this.velocities, 10);
            if (this.strictCollisions.get()) {
                this.velocity = this.prevMovement.horizontalLength() - this.boostAmount;
            }
        }
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null && this.enabled && !this.isPaused()) {
            if (this.strictCollisions.get() && BlackOut.mc.player.horizontalCollision) {
                this.boosts.clear();
            }

            if (!this.onlyOG.get() || BlackOut.mc.player.isOnGround()) {
                this.yaw = Managers.ROTATION.moveYaw + 90.0F;
            }

            if (Managers.ROTATION.move) {
                if (this.useTimer.get()) {
                    Timer.set(this.getTimer());
                    this.setTimer = true;
                }

                this.move(event);
            } else {
                if (this.setTimer) {
                    Timer.reset();
                    this.setTimer = false;
                    this.boosts.clear();
                }

                if (this.instantStop.get()) {
                    event.setXZ(this, 0.0, 0.0);
                }
            }
        } else {
            this.boosts.clear();
        }
    }

    @Event
    public void onReceive(PacketEvent.Receive.Pre event) {
        if (this.damageBoost.get() && this.enabled) {
            if (event.packet instanceof PlayerPositionLookS2CPacket) {
                this.velocity = 0.0;
                this.prevRubberband = System.currentTimeMillis();
                this.boosts.clear();
            } else if (System.currentTimeMillis() - this.prevRubberband >= 1000L) {
                Vec3d vel = null;
                if (event.packet instanceof EntityVelocityUpdateS2CPacket packet) {
                    if (BlackOut.mc.player == null || BlackOut.mc.player.getId() != packet.getEntityId()) {
                        return;
                    }

                    vel = new Vec3d(packet.getVelocityX() / 8000.0F, packet.getVelocityY() / 8000.0F, packet.getVelocityZ() / 8000.0F)
                            .subtract(this.getVelocity());
                }

                if (event.packet instanceof ExplosionS2CPacket packet) {
                    vel = new Vec3d(packet.getPlayerVelocityX(), packet.getPlayerVelocityY(), packet.getPlayerVelocityZ());
                }

                if (vel != null) {
                    double x = this.prevMovement.x / this.prevMovement.horizontalLength();
                    double z = this.prevMovement.z / this.prevMovement.horizontalLength();
                    double boost;
                    if (this.directionalBoost.get()) {
                        double velX = Math.max(vel.x * x, 0.0);
                        double velZ = Math.max(vel.z * z, 0.0);
                        boost = Math.sqrt(velX * velX + velZ * velZ);
                    } else {
                        boost = vel.length();
                    }

                    this.boosts.add(this.limitBoost(boost * this.boostFactor.get()), this.boostTime.get());
                }
            }
        }
    }

    private Vec3d getVelocity() {
        return !this.velocities.isEmpty() && this.latencyTicks.get() != 0
                ? this.velocities.get(Math.min(this.velocities.size(), this.latencyTicks.get()) - 1)
                : BlackOut.mc.player.getVelocity();
    }

    private void move(MoveEvent.Pre event) {
        this.updateVelocity();
        this.og++;
        if (BlackOut.mc.player.isOnGround()) {
            this.waiting = false;
        }

        if (!this.updateOffsets(event)) {
            this.updateJump(event);
            if (!this.updateFastFall(event)) {
                this.setXZ(event);
            }
        }
    }

    private void updateVelocity() {
        this.velocity = Math.max(this.velocity * this.getFriction(), this.getMinSpeed());
        this.velocity = this.limitMax(this.velocity);
    }

    private boolean updateOffsets(MoveEvent.Pre event) {
        if (!this.waiting && this.useOffsets.get() && this.og < this.offsets.size()) {
            if (BlackOut.mc.player.isOnGround()) {
                this.og = 0;
            }

            Offset offset = this.offsets.get(this.og);
            if (offset.og != null) {
                Managers.PACKET.spoofOG(offset.og);
            }

            this.velocity = offset.xz;
            if (this.offsetEffects.get()) {
                this.velocity = MovementUtils.getSpeed(this.velocity, this.effectMultiplier.get());
            }

            double rad = Math.toRadians(this.yaw);
            double x = Math.cos(rad) * this.velocity;
            double z = Math.sin(rad) * this.velocity;
            event.set(this, x, offset.y, z);
            return true;
        } else {
            return false;
        }
    }

    private boolean updateFastFall(MoveEvent.Pre event) {
        if (!this.fastFall.get()) {
            return false;
        } else {
            if (this.og == this.jumpTicks.get() - 1 && this.spoofOG.get()) {
                Managers.PACKET.spoofOG(true);
            }

            if (this.og != this.jumpTicks.get()) {
                return false;
            } else {
                event.setY(this, -this.fallSpeed.get());
                if (this.stopFall.get()) {
                    event.setXZ(this, 0.0, 0.0);
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    private void updateJump(MoveEvent.Pre event) {
        if (BlackOut.mc.player.isOnGround() && Step.getInstance().canStrafeJump(this.stepVelocity(event.movement.y))) {
            event.setY(this, this.jumpPower.get().floatValue());
            this.og = 0;
            this.vanillaBoosted = true;
            this.velocity = this.getNewJumpVelocity();
        } else if (this.og == 1 && this.vanillaBoosted) {
            this.vanillaBoosted = false;
            this.velocity = this.velocity / this.boostDiv.get();
        }
    }

    private Vec3d stepVelocity(double y) {
        double movement;
        if (this.addBoost.get()) {
            movement = this.velocity + this.boostAmount;
        } else {
            movement = Math.max(this.velocity, this.boostAmount);
        }

        double x = Math.cos(Math.toRadians(this.yaw)) * movement;
        double z = Math.sin(Math.toRadians(this.yaw)) * movement;
        return new Vec3d(x, y, z);
    }

    private double getNewJumpVelocity() {
        double newVel = MovementUtils.getSpeed(this.boost.get(), this.effectMultiplier.get());
        if (!this.resetBoost.get() && !(newVel > this.velocity)) {
            this.vanillaBoosted = false;
            return this.velocity;
        } else {
            return newVel * this.boostMulti.get();
        }
    }

    private void setXZ(MoveEvent.Pre event) {
        this.boostAmount = this.getBoost();
        Step step = Step.getInstance();
        if (!step.enabled || step.sinceStep > 1) {
            double movement;
            if (this.addBoost.get()) {
                movement = this.velocity + this.boostAmount;
            } else {
                movement = Math.max(this.velocity, this.boostAmount);
            }

            double x = Math.cos(Math.toRadians(this.yaw)) * movement;
            double z = Math.sin(Math.toRadians(this.yaw)) * movement;
            event.setXZ(this, x, z);
        }
    }

    private double getBoost() {
        MutableDouble mutableDouble = new MutableDouble(0.0);
        this.boosts.forEach(timer -> {
            double newVal;
            if (this.stackingBoost.get()) {
                newVal = this.limitBoost(mutableDouble.getValue() + timer.value);
            } else {
                newVal = Math.max(this.limitBoost(timer.value), mutableDouble.getValue());
            }

            mutableDouble.setValue(newVal);
        });
        return mutableDouble.getValue();
    }

    private double limitBoost(double boost) {
        return this.maxDamageBoost.get() <= 0.0 ? boost : Math.min(boost, this.maxDamageBoost.get());
    }

    private double limitMax(double speed) {
        return this.maxSpeed.get() > 0.0 ? Math.min(speed, this.effectMaxSpeed.get() ? MovementUtils.getSpeed(this.maxSpeed.get()) : this.maxSpeed.get()) : speed;
    }

    private double getMinSpeed() {
        double speed = this.ncpSpeed.get() ? 0.2873 : this.minSpeed.get();
        return MovementUtils.getSpeed(speed, this.effectMultiplier.get());
    }

    private float getTimer() {
        float start = this.ncpTimer.get() ? 1.088F : this.timer.get().floatValue();
        float end = this.endNcpTimer.get() ? 1.088F : this.endTimer.get().floatValue();
        return MathHelper.clampedLerp(
                start, end, MathHelper.getLerpProgress(this.og, this.timerStartProgress.get(), this.timerEndProgress.get())
        );
    }

    private double getFriction() {
        if (this.vanillaFriction.get()) {
            return this.getVanillaFriction();
        } else {
            double l = OLEPOSSUtils.safeDivide(this.og - this.startProgress.get(), this.endProgress.get() - this.startProgress.get());
            return MathHelper.clampedLerp(this.startFriction.get(), this.endFriction.get(), l);
        }
    }

    private double getVanillaFriction() {
        BlockPos blockPos = BlackOut.mc.player.getVelocityAffectingPos();
        double slipperiness = BlackOut.mc.world.getBlockState(blockPos).getBlock().getSlipperiness();
        return BlackOut.mc.player.isOnGround() ? 0.21600002F / (slipperiness * slipperiness * slipperiness) : 0.98;
    }

    private boolean isPaused() {
        if (this.pauseSneak.get() && BlackOut.mc.player.isSneaking()) {
            return true;
        } else if (this.pauseElytra.get() && BlackOut.mc.player.isFallFlying()) {
            return true;
        } else if (this.pauseFly.get() && BlackOut.mc.player.getAbilities().flying) {
            return true;
        } else {
            switch (this.pauseWater.get()) {
                case Touching:
                    if (BlackOut.mc.player.isTouchingWater()) {
                        return true;
                    }
                    break;
                case Submerged:
                    if (BlackOut.mc.player.isSubmergedIn(FluidTags.WATER)) {
                        return true;
                    }
                    break;
                case Both:
                    if (BlackOut.mc.player.isTouchingWater() || BlackOut.mc.player.isSubmergedIn(FluidTags.WATER)) {
                        return true;
                    }
            }
            return switch (this.pauseLava.get()) {
                case Touching -> BlackOut.mc.player.isInLava();
                case Submerged -> BlackOut.mc.player.isSubmergedIn(FluidTags.LAVA);
                case Both -> BlackOut.mc.player.isInLava() || BlackOut.mc.player.isSubmergedIn(FluidTags.LAVA);
                default -> false;
            };
        }
    }

    private record Offset(float xz, float y, Boolean og) {
    }
}
