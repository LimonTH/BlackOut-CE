package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.OnlyDev;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.misc.Timer;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.MovementUtils;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

// TODO: NEED PATCHES
@OnlyDev
public class Speed extends Module {
    private static Speed INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgPause = this.addGroup("Pause");

    public final Setting<SpeedMode> mode = this.sgGeneral.enumSetting("Algorithm Mode", SpeedMode.Instant, "The movement logic used to modify player velocity.");
    private final Setting<Boolean> strict = this.sgGeneral.booleanSetting("Strict Friction", true, "Utilizes precise momentum calculations to prevent flagging on anti-cheats.", () -> this.mode.get() == SpeedMode.NCPOld);
    private final Setting<Boolean> ncpSpeed = this.sgGeneral.booleanSetting("NCP Optimization", true, "Overrides speed to the maximum value permitted by the NoCheatPlus movement processor.", () -> this.mode.get() != SpeedMode.NCPOld && this.mode.get() != SpeedMode.Verus && this.mode.get() != SpeedMode.Vulcan);
    private final Setting<Double> speed = this.sgGeneral.doubleSetting("Velocity Limit", 0.3, 0.0, 1.0, 0.01, "The target horizontal speed in blocks per tick.", () -> this.mode.get() != SpeedMode.NCPOld && this.mode.get() != SpeedMode.Verus && this.mode.get() != SpeedMode.Vulcan && !this.ncpSpeed.get());
    private final Setting<Integer> accelerationTicks = this.sgGeneral.intSetting("Ramp-up Time", 3, 0, 10, 1, "The number of ticks required to reach maximum velocity.", () -> this.mode.get() == SpeedMode.Instant);
    public final Setting<Double> vanillaSpeed = this.sgGeneral.doubleSetting("Vanilla Multiplier", 1.0, 1.0, 2.0, 0.01, "A direct multiplier for the player's base movement speed.", () -> this.mode.get() == SpeedMode.Vanilla);
    private final Setting<Double> speedMulti = this.sgGeneral.doubleSetting("Momentum Factor", 1.3, 0.0, 2.0, 0.01, "The multiplier applied to the base jumping speed.", () -> this.mode.get() == SpeedMode.NCPOld);
    private final Setting<Boolean> useTimer = this.sgGeneral.booleanSetting("Tick Acceleration", true, "Synchronizes the client timer to maximize packet output.", () -> this.mode.get() != SpeedMode.Verus);
    private final Setting<Double> stepBoost = this.sgGeneral.doubleSetting("Transition Boost", 0.0, 0.0, 0.5, 0.01, "Adds additional velocity when ascending blocks via Step.");
    private final Setting<Double> boostDecay = this.sgGeneral.doubleSetting("Boost Smoothing", 0.5, 0.0, 1.0, 0.01, "How quickly the Transition Boost bleeds off over time.", () -> this.stepBoost.get() > 0.0);
    private final Setting<Double> stepBoostCooldown = this.sgGeneral.doubleSetting("Boost Cooldown", 0.0, 0.0, 1.0, 0.01, "The minimum delay between consecutive Transition Boosts.", () -> this.stepBoost.get() > 0.0);
    private final Setting<Boolean> instantStop = this.sgGeneral.booleanSetting("Zero Inertia", false, "Immediately halts all horizontal momentum when the movement keys are released.");

    public final Setting<LiquidMode> pauseWater = this.sgPause.enumSetting("Water Interaction", LiquidMode.Touching, "Defines how the module behaves when in contact with water.");
    public final Setting<LiquidMode> pauseLava = this.sgPause.enumSetting("Lava Interaction", LiquidMode.Touching, "Defines how the module behaves when in contact with lava.");
    private final Setting<Boolean> pauseSneak = this.sgPause.booleanSetting("Pause on Sneak", true, "Temporarily disables the module while the sneak key is held.");
    private final Setting<Boolean> pauseElytra = this.sgPause.booleanSetting("Pause on Flight", true, "Temporarily disables the module while using an Elytra.");
    private final Setting<Boolean> pauseFly = this.sgPause.booleanSetting("Pause on Creative Fly", true, "Temporarily disables the module while in creative flight mode.");

    private Vec3d prevMovement = Vec3d.ZERO;
    private double velocity = 0.0;
    private double yaw = 0.0;
    private boolean setTimer = false;
    private double boost = 0.0;
    private long prevBoost = 0L;

    public Speed() {
        super("Speed", "Modifies the player's horizontal velocity and jumping mechanics to achieve travel speeds beyond vanilla limits.", SubCategory.MOVEMENT, true);
        INSTANCE = this;
    }

    public static Speed getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        if (BlackOut.mc.player != null) {
            this.velocity = this.prevMovement.horizontalLength();
            this.yaw = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(this.prevMovement.z, this.prevMovement.x)));
        }
    }

    @Override
    public void onDisable() {
        Timer.reset();
    }

    @Override
    public String getInfo() {
        return this.mode.get().name();
    }

    @Override
    public boolean shouldSkipListeners() {
        return false;
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null || BlackOut.mc.world != null) {
            this.prevMovement = BlackOut.mc
                    .player
                    .getPos()
                    .subtract(BlackOut.mc.player.prevX, BlackOut.mc.player.prevY, BlackOut.mc.player.prevZ);
            if (this.enabled && BlackOut.mc.player.isOnGround() && (this.mode.get() == SpeedMode.Vulcan || this.mode.get() == SpeedMode.Verus)) {
                BlackOut.mc.player.jump();
            }
        }
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null && this.enabled && !this.isPaused() && this.mode.get() != SpeedMode.Vanilla) {
            if (this.mode.get() != SpeedMode.Vulcan || BlackOut.mc.player.isOnGround()) {
                this.yaw = Managers.ROTATION.moveYaw + 90.0F;
            }

            this.boost = this.boost * this.boostDecay.get();
            if (this.prevMovement.y > 0.6
                    && BlackOut.mc.player.isOnGround()
                    && System.currentTimeMillis() - this.prevBoost > this.stepBoostCooldown.get() * 1000.0) {
                this.boost = MovementUtils.getSpeed(this.stepBoost.get());
                this.prevBoost = System.currentTimeMillis();
            }

            if (Managers.ROTATION.move) {
                if (this.useTimer.get()) {
                    Timer.set(this.getTimer());
                    this.setTimer = true;
                }

                this.velocity = this.getNewVelocity();
                this.setMovement(event, Math.toRadians(this.yaw));
            } else {
                if (this.setTimer) {
                    Timer.reset();
                    this.setTimer = false;
                }

                if (this.instantStop.get()) {
                    event.setXZ(this, 0.0, 0.0);
                }
            }
        }
    }

    private float getTimer() {
        return switch (this.mode.get()) {
            case NCPOld, Instant, Vanilla -> 1.088F;
            case Verus -> 1.0F;
            case Vulcan -> 1.0420911F;
        };
    }

    @SuppressWarnings("fallthrough")
    private void setMovement(MoveEvent.Pre event, double yaw) {
        switch (this.mode.get()) {
            case NCPOld:
                if (BlackOut.mc.player.isOnGround()) {
                    event.setY(this, 0.3995);
                    this.velocity = MovementUtils.getSpeed(0.2873 * this.speedMulti.get());
                }
                // Fall through
            case Instant:
            case Vanilla:
            case Verus:
            case Vulcan:
            default:
                event.setXZ(this, (this.velocity + this.boost) * Math.cos(yaw), (this.velocity + this.boost) * Math.sin(yaw));
        }
    }

    private double getNewVelocity() {
        double baseSpeed = MovementUtils.getSpeed(this.getSpeed());

        return switch (this.mode.get()) {
            case NCPOld -> {
                if (BlackOut.mc.player.isOnGround()) {
                    yield baseSpeed * this.speedMulti.get();
                }
                // Используем strict friction для обхода NCP
                yield this.prevVelocity() * (this.strict.get() ? 0.91 : 0.98);
            }
            case Instant -> OLEPOSSUtils.approach(
                    this.prevMovement.horizontalLength(),
                    baseSpeed,
                    baseSpeed / Math.max(1, this.accelerationTicks.get())
            );
            case Vanilla -> baseSpeed * vanillaSpeed.get();
            case Verus -> BlackOut.mc.player.isOnGround() ? 0.612 : 0.355; // Verus чуть быстрее
            case Vulcan -> BlackOut.mc.player.isOnGround() ? 0.485 : 0.325;
        };
    }

    private double prevVelocity() {
        return this.strict.get() ? this.prevMovement.horizontalLength() : this.velocity;
    }

    private double getSpeed() {
        return this.ncpSpeed.get() ? 0.2873 : this.speed.get();
    }

    private boolean isPaused() {
        if (this.pauseSneak.get() && BlackOut.mc.player.isSneaking()) return true;
        if (this.pauseElytra.get() && BlackOut.mc.player.isFallFlying()) return true;
        if (this.pauseFly.get() && BlackOut.mc.player.getAbilities().flying) return true;

        boolean inWater = switch (this.pauseWater.get()) {
            case Touching -> BlackOut.mc.player.isTouchingWater();
            case Submerged -> BlackOut.mc.player.isSubmergedIn(FluidTags.WATER);
            case Both -> BlackOut.mc.player.isTouchingWater() || BlackOut.mc.player.isSubmergedIn(FluidTags.WATER);
            case Disabled -> false;
        };
        if (inWater) return true;

        return switch (this.pauseLava.get()) {
            case Touching -> BlackOut.mc.player.isInLava();
            case Submerged -> BlackOut.mc.player.isSubmergedIn(FluidTags.LAVA);
            case Both -> BlackOut.mc.player.isInLava() || BlackOut.mc.player.isSubmergedIn(FluidTags.LAVA);
            case Disabled -> false;
        };
    }

    public enum LiquidMode {
        Disabled,
        Submerged,
        Touching,
        Both
    }

    public enum SpeedMode {
        NCPOld,
        Instant,
        Vanilla,
        Verus,
        Vulcan
    }
}
