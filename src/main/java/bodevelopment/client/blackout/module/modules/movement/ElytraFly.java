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

/**
 * @author BlackOut / Fixed by Gemini
 */
public class ElytraFly extends Module {
    private static ElytraFly INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgRotation = this.addGroup("Rotation");
    private final SettingGroup sgSpeed = this.addGroup("Speed");
    private final SettingGroup sgAutomation = this.addGroup("Automation");

    public final Setting<Mode> mode = this.sgGeneral.enumSetting("Flight Mode", Mode.Control, "Movement logic model.");
    private final Setting<Integer> bounceDelay = this.sgGeneral.intSetting("Deployment Delay", 1, 0, 20, 1, "Ticks between ground-air transitions.", () -> mode.get() == Mode.Bounce);
    private final Setting<Boolean> smartFall = this.sgGeneral.booleanSetting("Drag Simulation", true, "Mimics lift based on pitch.", () -> mode.get() == Mode.Wasp);
    private final Setting<Boolean> autoStop = this.sgGeneral.booleanSetting("Durability Guard", true, "Disables flight if elytra is low.");
    private final Setting<Integer> minDurability = this.sgGeneral.intSetting("Min Durability", 5, 1, 50, 1, "Safety threshold.", autoStop::get);

    private final Setting<Double> slowPitch = this.sgRotation.doubleSetting("Stall Pitch", 50.0, 0.0, 90.0, 1.0, "Pitch for low speed.", () -> mode.get() == Mode.Bounce);
    private final Setting<Double> fastPitch = this.sgRotation.doubleSetting("Velocity Pitch", 35.0, 0.0, 90.0, 1.0, "Pitch for high speed.", () -> mode.get() == Mode.Bounce);
    private final Setting<Integer> climbPitchN = this.sgRotation.intSetting("Ascent Angle", 40, 0, 90, 1, "Pitch during climb.", () -> mode.get() == Mode.Glide);
    private final Setting<Integer> divePitchN = this.sgRotation.intSetting("Descent Angle", 38, 0, 90, 1, "Pitch during dive.", () -> mode.get() == Mode.Glide);
    private final Setting<Boolean> lockPitch = this.sgRotation.booleanSetting("Pitch Constraint", true, "Fixes pitch at -3 degrees.", () -> mode.get() == Mode.Rotation);
    private final Setting<Boolean> hover = this.sgRotation.booleanSetting("Idle Oscillation", true, "Hover wobble effect.", () -> mode.get() == Mode.Rotation);

    private final Setting<Double> horizontal = this.sgSpeed.doubleSetting("Lateral Velocity", 1.0, 0.0, 5.0, 0.1, "Wasp mode speed.", () -> mode.get() == Mode.Wasp);
    private final Setting<Double> up = this.sgSpeed.doubleSetting("Ascent Velocity", 1.0, 0.0, 5.0, 0.1, "Climb speed.", () -> mode.get() == Mode.Wasp || mode.get() == Mode.Rotation || mode.get() == Mode.Control);
    private final Setting<Double> speed = this.sgSpeed.doubleSetting("Target Velocity", 1.0, 0.0, 5.0, 0.1, "Travel speed.", () -> mode.get() == Mode.Control || mode.get() == Mode.Rotation || mode.get() == Mode.Bounce);
    private final Setting<Double> fallSpeed = this.sgSpeed.doubleSetting("Passive Descent", 0.01, 0.0, 1.0, 0.1, "Minimal down speed.", () -> mode.get() == Mode.Control || mode.get() == Mode.Wasp);
    private final Setting<Integer> targetY = this.sgSpeed.intSetting("Cruise Altitude", 180, 0, 320, 1, "Target Y-level.", () -> mode.get() == Mode.Glide);
    private final Setting<Integer> vLow = this.sgSpeed.intSetting("Stall Threshold", 14, 5, 40, 1, "Speed to start dive.", () -> mode.get() == Mode.Glide);
    private final Setting<Integer> vHigh = this.sgSpeed.intSetting("Momentum Threshold", 27, 10, 60, 1, "Speed to start climb.", () -> mode.get() == Mode.Glide);
    private final Setting<Integer> hoverSpeed = this.sgSpeed.intSetting("Oscillation Freq", 10, 2, 20, 1, "Ticks between hover shakes.", () -> mode.get() == Mode.Rotation && hover.get());

    private final Setting<Boolean> allowRockets = this.sgAutomation.booleanSetting("Auto Rocket", true, "Uses fireworks in Glide.");
    private final Setting<Integer> rocketDelay = this.sgAutomation.intSetting("Rocket Delay", 3500, 500, 10000, 100, "Cooldown between rockets.", allowRockets::get);
    private final Setting<Boolean> rocketInventory = this.sgAutomation.booleanSetting("Search Inventory", true, "Swap rockets from inventory.");

    private boolean moving;
    private float yaw;
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
        super("Elytra Fly", "Ultimate hybrid flight system.", SubCategory.MOVEMENT, true);
        INSTANCE = this;
    }

    public static ElytraFly getInstance() { return INSTANCE; }

    @Override
    public String getInfo() {
        return mode.get().name() + (mode.get() == Mode.Glide ? String.format(" [%.0f]", currentSpeedAvg) : "");
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
            if (!BlackOut.mc.options.jumpKey.isPressed()) {
                this.sus = false;
            }

            BlackOut.mc.player.setSprinting(true);

            if (this.sinceFalling <= 1 && BlackOut.mc.player.isOnGround()) {
                BlackOut.mc.player.jump();
                this.sinceJump = 0;
                if (BlackOut.mc.options.jumpKey.isPressed()) {
                    this.sus = true;
                }
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
            BlackOut.mc.player.setPitch(this.getPitch());
        }
    }

    private void handleGlide(MoveEvent.Pre event) {
        if (!BlackOut.mc.player.isFallFlying()) return;
        double playerY = BlackOut.mc.player.getY();
        long currentTime = System.currentTimeMillis();
        if (!climbingToTarget && playerY < targetY.get() - 60) climbingToTarget = true;

        GlideState state;
        if (climbingToTarget) {
            state = GlideState.CLIMB;
            if (allowRockets.get() && currentTime - lastRocketTime >= rocketDelay.get()) {
                if (useRocket()) lastRocketTime = currentTime;
            }
            if (playerY >= targetY.get()) climbingToTarget = false;
        } else {
            int low = vLow.get();
            int high = Math.max(vHigh.get(), low + 2);
            if (currentSpeedAvg <= low) glideState = GlideState.DIVE;
            else if (currentSpeedAvg >= high) glideState = GlideState.CRUISE;
            state = glideState;
        }

        float targetPitch;
        if (state == GlideState.DIVE) targetPitch = divePitchN.get().floatValue();
        else if (state == GlideState.CLIMB) targetPitch = -climbPitchN.get().floatValue();
        else {
            cruisePhase += 0.019;
            if (cruisePhase > 1.0) cruisePhase -= 1.0;
            double tri = 2.0 * Math.abs(2.0 * (cruisePhase - Math.floor(cruisePhase + 0.5))) - 1.0;
            targetPitch = (float) -(4.0 + (12.0 - 4.0) * (0.5 * (tri + 1.0)));
        }
        BlackOut.mc.player.setPitch(MathHelper.stepTowards(BlackOut.mc.player.getPitch(), targetPitch, 10));
    }

    private void handleRotation(MoveEvent.Pre event) {
        if (!BlackOut.mc.player.isFallFlying()) return;
        Vec3d dir = getNamiControlDir();
        if (dir != null) {
            float targetYaw = (float) Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90f;
            BlackOut.mc.player.setYaw(targetYaw);
            if (Math.abs(dir.y) > 0.5) BlackOut.mc.player.setPitch(dir.y > 0 ? -90f : 90f);
            else if (lockPitch.get()) BlackOut.mc.player.setPitch(-3f);
            event.set(this, dir.x * speed.get(), dir.y * up.get(), dir.z * speed.get());
        } else if (hover.get()) {
            if (System.currentTimeMillis() - lastHoverTime > (hoverSpeed.get() * 50)) {
                hoverB = !hoverB; lastHoverTime = System.currentTimeMillis();
            }
            BlackOut.mc.player.setYaw(BlackOut.mc.player.getYaw() + (hoverB ? 0.4f : -0.4f));
            BlackOut.mc.player.setPitch(-3f);
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
        for (int i = 0; i < (rocketInventory.get() ? 36 : 9); i++) {
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
                this.slowPitch.get().floatValue(), this.fastPitch.get().floatValue(), (float) BlackOut.mc.player.getVelocity().horizontalLength()
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
        double x = moving ? Math.cos(Math.toRadians(yaw + 90)) * horizontal.get() : 0;
        double z = moving ? Math.sin(Math.toRadians(yaw + 90)) * horizontal.get() : 0;
        double y = BlackOut.mc.options.jumpKey.isPressed() ? up.get() : (BlackOut.mc.options.sneakKey.isPressed() ? -up.get() : -fallSpeed.get());
        if (smartFall.get()) y *= Math.abs(Math.sin(Math.toRadians(BlackOut.mc.player.getPitch())));
        event.set(this, x, y, z);
        BlackOut.mc.player.setVelocity(0, 0, 0);
    }

    public void controlTick(MoveEvent.Pre event) {
        if (!BlackOut.mc.player.isFallFlying()) return;
        updateControlMovement();
        double x = moving ? Math.cos(Math.toRadians(yaw + 90)) * speed.get() : 0;
        double z = moving ? Math.sin(Math.toRadians(yaw + 90)) * speed.get() : 0;
        double y = BlackOut.mc.options.jumpKey.isPressed() ? up.get() : (BlackOut.mc.options.sneakKey.isPressed() ? -up.get() : -fallSpeed.get());
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