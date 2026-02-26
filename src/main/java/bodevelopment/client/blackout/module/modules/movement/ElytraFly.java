package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ElytraFly extends Module {
    private static ElytraFly INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgSpeed = this.addGroup("Speed");
    private final SettingGroup sgRotation = this.addGroup("Rotation");
    private final SettingGroup sgAutomation = this.addGroup("Automation");

    public final Setting<Mode> mode = this.sgGeneral.enumSetting("Flight Model", Mode.Control, "The mathematical logic used to calculate aerial velocity and trajectory.");
    private final Setting<Boolean> autoStop = this.sgGeneral.booleanSetting("Durability Guard", true, "Emergency deactivation to prevent elytra breakage when durability is critical.");
    private final Setting<Integer> minDurability = this.sgGeneral.intSetting("Structural Threshold", 5, 1, 50, 1, "The minimum durability value required to maintain flight operations.", autoStop::get);
    private final Setting<Integer> bounceDelay = this.sgGeneral.intSetting("Deployment Delay", 1, 0, 20, 1, "Tick interval between ground contact and re-initiating fall-flying state.", () -> mode.get() == Mode.Bounce);

    private final Setting<Double> horizontalSpeed = this.sgSpeed.doubleSetting("Lateral Velocity", 1.0, 0.0, 5.0, 0.05, "Maximum travel speed along the X and Z axes.", () -> mode.get() != Mode.Glide);
    private final Setting<Double> verticalSpeed = this.sgSpeed.doubleSetting("Vertical Thrust", 1.0, 0.0, 5.0, 0.05, "Ascent and descent velocity applied during manual maneuvering.", () -> mode.get() == Mode.Control || mode.get() == Mode.Wasp || mode.get() == Mode.Rotation);
    private final Setting<Double> passiveDescent = this.sgSpeed.doubleSetting("Glide Gravity", 0.01, 0.0, 1.0, 0.005, "Constant downward velocity applied to simulate aerodynamic drag and maintain flight state.", () -> mode.get() == Mode.Control || mode.get() == Mode.Wasp);
    private final Setting<Boolean> liftSensing = this.sgSpeed.booleanSetting("Aerodynamic Lift", true, "Dynamically modulates vertical descent based on current camera pitch.", () -> mode.get() == Mode.Wasp);
    private final Setting<Integer> cruiseAltitude = this.sgSpeed.intSetting("Target Altitude", 180, 0, 320, 1, "The desired Y-level maintained by the automated gliding algorithm.", () -> mode.get() == Mode.Glide);
    private final Setting<Integer> stallThreshold = this.sgSpeed.intSetting("Stall Velocity", 14, 5, 40, 1, "The speed floor that triggers a kinetic dive to regain momentum.", () -> mode.get() == Mode.Glide);
    private final Setting<Integer> momentumThreshold = this.sgSpeed.intSetting("climb Velocity", 27, 10, 60, 1, "The speed ceiling that triggers an ascent to convert kinetic energy into altitude.", () -> mode.get() == Mode.Glide);

    private final Setting<Double> pitchLerp = this.sgRotation.doubleSetting("Angular Interpolation", 0.1, 0.01, 1.0, 0.01, "The smoothness of pitch transitions when calculating speed-based orientation.", () -> mode.get() == Mode.Bounce);
    private final Setting<Double> slowPitch = this.sgRotation.doubleSetting("Stall Orientation", 50.0, 0.0, 90.0, 0.5, "The pitch angle targeted during low-velocity flight phases.", () -> mode.get() == Mode.Bounce);
    private final Setting<Double> fastPitch = this.sgRotation.doubleSetting("Momentum Orientation", 35.0, 0.0, 90.0, 0.5, "The pitch angle targeted during high-velocity flight phases.", () -> mode.get() == Mode.Bounce);
    private final Setting<Integer> diveAngle = this.sgRotation.intSetting("Dive Pitch", 38, 0, 90, 1, "Downward angular target during the acceleration phase of gliding.", () -> mode.get() == Mode.Glide);
    private final Setting<Integer> climbAngle = this.sgRotation.intSetting("Ascent Pitch", 40, 0, 90, 1, "Upward angular target during the altitude-gain phase of gliding.", () -> mode.get() == Mode.Glide);
    private final Setting<Boolean> lockPitch = this.sgRotation.booleanSetting("Constraint Axis", true, "Prevents manual pitch modification to maintain optimal flight angles.", () -> mode.get() == Mode.Rotation);
    private final Setting<Double> fixedPitchValue = this.sgRotation.doubleSetting("Static Angle", -3.0, -90.0, 90.0, 0.5, "The specific pitch degree enforced when axis locking is active.", () -> mode.get() == Mode.Rotation && lockPitch.get());
    private final Setting<Boolean> idleWobble = this.sgRotation.booleanSetting("Procedural Oscillation", true, "Applies a gentle sinusoidal movement to simulate hovering while stationary.", () -> mode.get() == Mode.Rotation);

    private final Setting<Boolean> autoRocket = this.sgAutomation.booleanSetting("Kinetic Injection", true, "Automatically deploys firework rockets to maintain flight momentum.", () -> mode.get() == Mode.Glide);
    private final Setting<Integer> rocketCooldown = this.sgAutomation.intSetting("Propulsion Interval", 3500, 500, 10000, 100, "The minimum time in milliseconds between automated rocket deployments.", () -> (autoRocket.get() && mode.get() == Mode.Glide));
    private final Setting<Boolean> deepScan = this.sgAutomation.booleanSetting("Inventory Retrieval", true, "Allows the module to pull rockets from the main inventory into the hotbar.", () -> (autoRocket.get() && mode.get() == Mode.Glide));

    private boolean moving;
    private float yaw, lerpedPitch;
    private int sinceFalling, sinceJump;
    private boolean sus;

    private enum GlideState { DIVE, CRUISE, CLIMB }
    private GlideState glideState = GlideState.CRUISE;
    private boolean climbingToTarget = false;
    private double cruisePhase = 0;
    private double currentSpeedAvg = 0;
    private final double[] speedSamples = new double[25];
    private int speedSampleIndex = 0;
    private long lastRocketTime, lastHoverTime;
    private boolean hoverB = true;
    private double lastX, lastZ;

    public ElytraFly() {
        super("Elytra Fly", "A comprehensive aeronautical suite offering various modes for manual control, kinetic gliding, and automated travel.", SubCategory.MOVEMENT, true);
        INSTANCE = this;
    }

    public static ElytraFly getInstance() { return INSTANCE; }

    @Override
    public String getInfo() {
        return mode.get().name() + (mode.get() == Mode.Glide ? String.format(" [%.1f]", currentSpeedAvg) : "");
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player == null) return;

        if (autoStop.get()) {
            ItemStack chest = BlackOut.mc.player.getInventory().getArmorStack(2);
            if (chest.getItem() == Items.ELYTRA && (chest.getMaxDamage() - chest.getDamage()) < minDurability.get()) {
                this.disable();
                return;
            }
        }

        calculateAverageSpeed();

        if (this.mode.get() == Mode.Bounce) {
            if (!BlackOut.mc.options.jumpKey.isPressed()) this.sus = false;
            BlackOut.mc.player.setSprinting(true);

            if (this.sinceFalling <= 1 && BlackOut.mc.player.isOnGround()) {
                BlackOut.mc.player.jump();
                this.sinceJump = 0;
                if (BlackOut.mc.options.jumpKey.isPressed()) this.sus = true;
            } else if (this.sinceJump > this.bounceDelay.get() && BlackOut.mc.player.checkFallFlying()) {
                Managers.PACKET.sendInstantly(new ClientCommandC2SPacket(BlackOut.mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }

            this.sinceJump++;
            this.sinceFalling = BlackOut.mc.player.isFallFlying() ? 0 : this.sinceFalling + 1;
        }
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        if (BlackOut.mc.player == null) return;

        switch (this.mode.get()) {
            case Wasp -> this.waspTick(event);
            case Control -> this.controlTick(event);
            case Glide -> this.handleGlide(event);
            case Rotation -> this.handleRotation(event);
            case Bounce -> this.handleBounceMove(event);
        }
    }

    private void handleBounceMove(MoveEvent.Pre event) {
        if (BlackOut.mc.player.isFallFlying() && !this.sus) {
            float target = getPitch();
            lerpedPitch = (float) MathHelper.lerp(pitchLerp.get(), lerpedPitch, target);
            BlackOut.mc.player.setPitch(lerpedPitch);
        }
    }

    private void handleGlide(MoveEvent.Pre event) {
        if (!BlackOut.mc.player.isFallFlying()) return;
        double playerY = BlackOut.mc.player.getY();
        long currentTime = System.currentTimeMillis();
        if (!climbingToTarget && playerY < cruiseAltitude.get() - 60) climbingToTarget = true;

        GlideState state;
        if (climbingToTarget) {
            state = GlideState.CLIMB;
            if (autoRocket.get() && currentTime - lastRocketTime >= rocketCooldown.get()) {
                if (useRocket()) lastRocketTime = currentTime;
            }
            if (playerY >= cruiseAltitude.get()) climbingToTarget = false;
        } else {
            if (currentSpeedAvg <= stallThreshold.get()) glideState = GlideState.DIVE;
            else if (currentSpeedAvg >= momentumThreshold.get()) glideState = GlideState.CRUISE;
            state = glideState;
        }

        float targetPitch;
        if (state == GlideState.DIVE) targetPitch = diveAngle.get().floatValue();
        else if (state == GlideState.CLIMB) targetPitch = -climbAngle.get().floatValue();
        else {
            cruisePhase += 0.019;
            double tri = 2.0 * Math.abs(2.0 * (cruisePhase - Math.floor(cruisePhase + 0.5))) - 1.0;
            targetPitch = (float) -(4.0 + (8.0 * (0.5 * (tri + 1.0))));
        }
        BlackOut.mc.player.setPitch(MathHelper.stepTowards(BlackOut.mc.player.getPitch(), targetPitch, 5));
    }

    private void handleRotation(MoveEvent.Pre event) {
        if (!BlackOut.mc.player.isFallFlying()) return;
        Vec3d dir = getNamiControlDir();
        if (dir != null) {
            float targetYaw = (float) Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90f;
            BlackOut.mc.player.setYaw(targetYaw);
            if (Math.abs(dir.y) > 0.5) BlackOut.mc.player.setPitch(dir.y > 0 ? -90f : 90f);
            else if (lockPitch.get()) BlackOut.mc.player.setPitch(fixedPitchValue.get().floatValue());
            event.set(this, dir.x * horizontalSpeed.get(), dir.y * verticalSpeed.get(), dir.z * horizontalSpeed.get());
        } else if (idleWobble.get()) {
            if (System.currentTimeMillis() - lastHoverTime > 500) {
                hoverB = !hoverB; lastHoverTime = System.currentTimeMillis();
            }
            BlackOut.mc.player.setYaw(BlackOut.mc.player.getYaw() + (hoverB ? 0.4f : -0.4f));
            if (lockPitch.get()) BlackOut.mc.player.setPitch(fixedPitchValue.get().floatValue());
            event.set(this, 0, -0.0001, 0);
        }
        BlackOut.mc.player.setVelocity(0, 0, 0);
    }

    private void calculateAverageSpeed() {
        double dx = BlackOut.mc.player.getX() - lastX;
        double dz = BlackOut.mc.player.getZ() - lastZ;
        double instantSpeed = Math.sqrt(dx * dx + dz * dz) * 20.0;
        speedSamples[speedSampleIndex] = instantSpeed;
        speedSampleIndex = (speedSampleIndex + 1) % speedSamples.length;
        double sum = 0;
        for (double s : speedSamples) sum += s;
        currentSpeedAvg = sum / speedSamples.length;
        lastX = BlackOut.mc.player.getX();
        lastZ = BlackOut.mc.player.getZ();
    }

    private boolean useRocket() {
        int slot = -1;
        for (int i = 0; i < (deepScan.get() ? 36 : 9); i++) {
            if (BlackOut.mc.player.getInventory().getStack(i).isOf(Items.FIREWORK_ROCKET)) { slot = i; break; }
        }
        if (slot == -1) return false;
        int old = BlackOut.mc.player.getInventory().selectedSlot;
        if (slot < 9) {
            Managers.PACKET.sendInstantly(new UpdateSelectedSlotC2SPacket(slot));
            BlackOut.mc.interactionManager.interactItem(BlackOut.mc.player, Hand.MAIN_HAND);
            Managers.PACKET.sendInstantly(new UpdateSelectedSlotC2SPacket(old));
        } else {
            BlackOut.mc.interactionManager.clickSlot(0, slot, old, SlotActionType.SWAP, BlackOut.mc.player);
            BlackOut.mc.interactionManager.interactItem(BlackOut.mc.player, Hand.MAIN_HAND);
            BlackOut.mc.interactionManager.clickSlot(0, slot, old, SlotActionType.SWAP, BlackOut.mc.player);
        }
        return true;
    }

    public float getPitch() {
        return this.sus ? BlackOut.mc.player.getPitch() : MathHelper.clampedLerp(
                slowPitch.get().floatValue(), fastPitch.get().floatValue(), (float) BlackOut.mc.player.getVelocity().horizontalLength()
        );
    }

    public boolean isBouncing() {
        return this.mode.get() == Mode.Bounce && (BlackOut.mc.player.isFallFlying() || this.sinceFalling < 5);
    }

    private Vec3d getNamiControlDir() {
        float f = BlackOut.mc.player.input.movementForward;
        float s = BlackOut.mc.player.input.movementSideways;
        if (f == 0 && s == 0 && !BlackOut.mc.options.jumpKey.isPressed() && !BlackOut.mc.options.sneakKey.isPressed()) return null;
        if (BlackOut.mc.options.jumpKey.isPressed()) return new Vec3d(0, 1, 0);
        if (BlackOut.mc.options.sneakKey.isPressed()) return new Vec3d(0, -1, 0);
        float yawRad = (float) Math.toRadians(BlackOut.mc.player.getYaw());
        return new Vec3d(-Math.sin(yawRad) * f + -Math.sin(yawRad + 1.57) * s, 0, Math.cos(yawRad) * f + Math.cos(yawRad + 1.57) * s).normalize();
    }

    public void waspTick(MoveEvent.Pre event) {
        if (!BlackOut.mc.player.isFallFlying()) return;
        updateControlMovement();
        double x = moving ? Math.cos(Math.toRadians(yaw + 90)) * horizontalSpeed.get() : 0;
        double z = moving ? Math.sin(Math.toRadians(yaw + 90)) * horizontalSpeed.get() : 0;
        double y = BlackOut.mc.options.jumpKey.isPressed() ? verticalSpeed.get() : (BlackOut.mc.options.sneakKey.isPressed() ? -verticalSpeed.get() : -passiveDescent.get());
        if (liftSensing.get()) y *= Math.abs(Math.sin(Math.toRadians(BlackOut.mc.player.getPitch())));
        event.set(this, x, y, z);
        BlackOut.mc.player.setVelocity(0, 0, 0);
    }

    public void controlTick(MoveEvent.Pre event) {
        if (!BlackOut.mc.player.isFallFlying()) return;
        updateControlMovement();
        double x = moving ? Math.cos(Math.toRadians(yaw + 90)) * horizontalSpeed.get() : 0;
        double z = moving ? Math.sin(Math.toRadians(yaw + 90)) * horizontalSpeed.get() : 0;
        double y = BlackOut.mc.options.jumpKey.isPressed() ? verticalSpeed.get() : (BlackOut.mc.options.sneakKey.isPressed() ? -verticalSpeed.get() : -passiveDescent.get());
        event.set(this, x, y, z);
        BlackOut.mc.player.setVelocity(0, 0, 0);
    }

    private void updateControlMovement() {
        float f = BlackOut.mc.player.input.movementForward;
        float s = BlackOut.mc.player.input.movementSideways;
        float y = BlackOut.mc.player.getYaw();
        if (f > 0) { moving = true; y += s > 0 ? -45 : (s < 0 ? 45 : 0); }
        else if (f < 0) { moving = true; y += s > 0 ? -135 : (s < 0 ? 135 : 180); }
        else { moving = s != 0; y += s > 0 ? -90 : (s < 0 ? 90 : 0); }
        this.yaw = y;
    }

    public enum Mode { Wasp, Control, Bounce, Glide, Rotation }
}