package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.util.PlayerUtils;
import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.module.modules.misc.Timer;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import bodevelopment.client.blackout.util.MovementUtils;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableDouble;

public class LongJump extends Module {
    private static LongJump INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgDamageBoost = this.addGroup("Damage Boost");

    private final Setting<Double> jumpPower = this.sgGeneral.doubleSetting("Jump Strength", 0.424, 0.38, 0.44, 0.001, "The initial upward vertical velocity applied during the jump.");
    private final Setting<Double> boost = this.sgGeneral.doubleSetting("Launch Speed", 1.0, 0.0, 3.0, 0.01, "The base horizontal velocity applied at the start of the jump.");
    private final Setting<Double> timer = this.sgGeneral.doubleSetting("Timer Override", 1.0, 0.05, 10.0, 0.05, "Modifies the game tick rate to extend the jump duration or speed.");
    private final Setting<Boolean> effects = this.sgGeneral.booleanSetting("Status Scaling", true, "Scales the jump distance based on active status effects like Speed or Slowness.");
    private final Setting<Double> friction = this.sgGeneral.doubleSetting("Air Friction", 0.93, 0.8, 1.0, 0.001, "The multiplier applied to horizontal velocity while in the air.");
    private final Setting<Integer> chargeTicks = this.sgGeneral.intSetting("Preparation Ticks", 5, 0, 20, 1, "The amount of time the player stays on the ground to build momentum before launching.");
    private final Setting<Double> chargeMotion = this.sgGeneral.doubleSetting("Preparation Motion", 0.05, 0.0, 1.0, 0.01, "The movement speed during the preparation phase.");
    private final Setting<Boolean> chargeSprint = this.sgGeneral.booleanSetting("Force Sprinting", true, "Ensures the player is sprinting during the preparation phase.");
    private final Setting<Integer> jumps = this.sgGeneral.intSetting("Jump Limit", 1, 0, 20, 1, "The number of consecutive long jumps to perform before disabling.");
    private final Setting<Double> boostMulti = this.sgGeneral.doubleSetting("Ascent Multiplier", 1.6, 0.0, 5.0, 0.05, "Multiplies the horizontal velocity at the moment of jumping.");
    private final Setting<Double> boostDiv = this.sgGeneral.doubleSetting("Descent Dampener", 1.6, 0.0, 5.0, 0.05, "Reduces horizontal velocity after the initial jump phase to prevent anti-cheat flags.");
    private final Setting<Double> effectMultiplier = this.sgGeneral.doubleSetting("Effect Intensity", 1.0, 0.0, 2.0, 0.02, "Adjusts how much status effects influence the total distance.");
    private final Setting<Boolean> ncpSpeed = this.sgGeneral.booleanSetting("Standard Min Speed", true, "Limits minimum air speed to standard NCP-safe values (0.2873).");
    private final Setting<Double> minSpeed = this.sgGeneral.doubleSetting("Custom Min Speed", 0.3, 0.0, 1.0, 0.01, "The minimum horizontal velocity maintained while in the air.", () -> !this.ncpSpeed.get());

    private final Setting<Boolean> damageBoost = this.sgDamageBoost.booleanSetting("Explosive Scaling", false, "Converts incoming damage (explosions/velocity) into additional jump distance.");
    private final Setting<Boolean> stackingBoost = this.sgDamageBoost.booleanSetting("Additive Stacking", false, "Allows multiple instances of damage to add together for a larger boost.");
    private final Setting<Boolean> directionalBoost = this.sgDamageBoost.booleanSetting("Vector Filtering", true, "Only applies damage boosts that align with the player's current movement direction.");
    private final Setting<Double> boostFactor = this.sgDamageBoost.doubleSetting("Damage Multiplier", 1.0, 0.0, 5.0, 0.05, "The factor by which incoming velocity is multiplied.");
    private final Setting<Double> boostTime = this.sgDamageBoost.doubleSetting("Boost Window", 0.5, 0.0, 2.0, 0.02, "The duration in seconds that a damage boost remains active.");
    private final Setting<Double> maxDamageBoost = this.sgDamageBoost.doubleSetting("Velocity Cap", 0.5, 0.0, 5.0, 0.05, "The maximum horizontal speed gain allowed from a damage boost.");

    private final TimerList<Double> boosts = new TimerList<>(true);
    private int phase = 0;
    private Vec3 prevMovement = new Vec3(0.0, 0.0, 0.0);
    private int ticks = 0;
    private int jumped = 0;
    private boolean changedTimer = false;
    private long prevRubberband = 0L;

    public LongJump() {
        super("Long Jump", "Combines high horizontal velocity with specialized jump logic to cover large gaps and bypass anti-cheat checks.", SubCategory.MOVEMENT, true);
        INSTANCE = this;
    }

    public static LongJump getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        this.phase = 0;
        this.jumped = 0;
    }

    @Override
    public boolean shouldSkipListeners() {
        return false;
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (PlayerUtils.isInGame()) {
            if (this.timer.get() != 1.0 && this.enabled) {
                Timer.set(this.timer.get().floatValue());
                this.changedTimer = true;
            }

            this.prevMovement = BlackOut.mc
                    .player
                    .position()
                    .subtract(BlackOut.mc.player.xo, BlackOut.mc.player.yo, BlackOut.mc.player.zo);
            if (BlackOut.mc.player.isInWater()) {
                this.disable(this.getDisplayName() + " disabled, touching water");
            }
        }
    }

    @Event
    public void onPacket(PacketEvent.Receive.Pre event) {
        if (event.packet instanceof ClientboundPlayerPositionPacket) {
            if (this.enabled) {
                this.disable(this.getDisplayName() + " was disabled to prevent rubberbanding", 4, Notifications.Type.Alert);
            }

            this.prevRubberband = System.currentTimeMillis();
            this.boosts.clear();
        } else if (this.damageBoost.get()) {
            if (System.currentTimeMillis() - this.prevRubberband >= 1000L) {
                Vec3 vel = null;

                if (event.packet instanceof ClientboundSetEntityMotionPacket packet) {
                    if (BlackOut.mc.player == null || BlackOut.mc.player.getId() != packet.getId()) {
                        return;
                    }

                    vel = new Vec3(packet.getXa(), packet.getYa(), packet.getZa())
                            .subtract(BlackOut.mc.player.getDeltaMovement());
                }

                if (event.packet instanceof ClientboundExplodePacket packet) {
                    if (packet.playerKnockback().isPresent()) {
                        vel = packet.playerKnockback().get();
                    }
                }

                if (vel != null) {
                    double hLen = this.prevMovement.horizontalDistance();
                    double boost;

                    if (this.directionalBoost.get() && hLen > 0.0001) {
                        double x = this.prevMovement.x / hLen;
                        double z = this.prevMovement.z / hLen;

                        double dot = vel.x * x + vel.z * z;
                        boost = Math.max(dot, 0.0);
                    } else {
                        boost = vel.horizontalDistance();
                    }

                    if (boost > 0) {
                        this.boosts.add(this.limitBoost(boost * this.boostFactor.get()), this.boostTime.get());
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        if (this.changedTimer) {
            Timer.reset();
            this.changedTimer = false;
        }
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        if (this.enabled && PlayerUtils.isInGame()) {
            if (this.phase == 0 && BlackOut.mc.player.onGround()) {
                this.phase = 1;
                this.ticks = this.chargeTicks.get();
            }

            if (this.phase == 1) {
                if (--this.ticks < 0) {
                    this.phase = 2;
                } else {
                    event.setXZ(
                            this,
                            MovementUtils.xMovement(this.chargeMotion.get(), BlackOut.mc.player.getYRot()),
                            MovementUtils.zMovement(this.chargeMotion.get(), BlackOut.mc.player.getYRot())
                    );
                    if (this.chargeSprint.get()) {
                        BlackOut.mc.player.setSprinting(true);
                    }
                }
            }

            if (this.phase >= 2) {
                if (this.phase == 2) {
                    double speed = this.effects.get() ? MovementUtils.getSpeed(this.boost.get(), this.effectMultiplier.get()) : this.boost.get();
                    speed += this.getBoost();
                    speed *= this.boostMulti.get();
                    event.set(
                            this,
                            MovementUtils.xMovement(speed, BlackOut.mc.player.getYRot()),
                            this.jumpPower.get().floatValue(),
                            MovementUtils.zMovement(speed, BlackOut.mc.player.getYRot())
                    );
                    this.jumped++;
                    this.phase = 3;
                } else {
                    if (BlackOut.mc.player.onGround()) {
                        if (this.jumped >= this.jumps.get() && this.jumps.get() != 0) {
                            this.disable(this.getDisplayName() + " was disabled due to landing");
                        } else {
                            this.phase = 0;
                        }
                    }

                    double velocity = this.prevMovement.horizontalDistance() * this.friction.get();
                    if (this.phase == 3) {
                        velocity /= this.boostDiv.get();
                        this.phase = 4;
                    }

                    velocity = Math.max(velocity, this.getMinSpeed());
                    double yaw = Math.toRadians(BlackOut.mc.player.getYRot() + 90.0F);
                    event.setXZ(this, velocity * Math.cos(yaw), velocity * Math.sin(yaw));
                }
            }
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

    private double getMinSpeed() {
        return this.ncpSpeed.get() ? 0.2873 : this.minSpeed.get();
    }
}
