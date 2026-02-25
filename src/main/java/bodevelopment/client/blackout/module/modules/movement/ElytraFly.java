package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.OnlyDev;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.math.MathHelper;

// TODO: NEED PATCHES
@OnlyDev
public class ElytraFly extends Module {
    private static ElytraFly INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgSpeed = this.addGroup("Speed");

    public final Setting<Mode> mode = this.sgGeneral.enumSetting("Flight Mode", Mode.Control, "The logic used to handle Elytra movement.");

    private final Setting<Integer> bounceDelay = this.sgSpeed.intSetting("Activation Delay", 1, 0, 20, 1, "The number of ticks to wait before re-deploying the Elytra during a bounce jump.", () -> this.mode.get() == Mode.Bounce);
    private final Setting<Double> slowPitch = this.sgSpeed.doubleSetting("Min Speed Pitch", 50.0, 0.0, 90.0, 1.0, "The camera pitch used when moving at lower velocities in Bounce mode.", () -> this.mode.get() == Mode.Bounce);
    private final Setting<Double> fastPitch = this.sgSpeed.doubleSetting("Max Speed Pitch", 35.0, 0.0, 90.0, 1.0, "The camera pitch used when moving at higher velocities in Bounce mode.", () -> this.mode.get() == Mode.Bounce);
    private final Setting<Double> horizontal = this.sgSpeed.doubleSetting("Lateral Speed", 1.0, 0.0, 5.0, 0.1, "The horizontal movement velocity while flying.", () -> this.mode.get() == Mode.Wasp);
    private final Setting<Double> up = this.sgSpeed.doubleSetting("Ascent Speed", 1.0, 0.0, 5.0, 0.1, "The vertical velocity when ascending.", () -> this.mode.get() == Mode.Wasp);
    private final Setting<Double> speed = this.sgSpeed.doubleSetting("Base Speed", 1.0, 0.0, 5.0, 0.1, "The constant travel speed while maintaining level flight.", () -> this.mode.get() == Mode.Control);
    private final Setting<Double> upMultiplier = this.sgSpeed.doubleSetting("Ascent Multiplier", 1.0, 0.0, 5.0, 0.1, "Increases the vertical thrust applied when pulling up.", () -> this.mode.get() == Mode.Control);
    private final Setting<Double> down = this.sgSpeed.doubleSetting("Descent Speed", 1.0, 0.0, 5.0, 0.1, "The vertical velocity when fast-descending via the sneak key.", () -> this.mode.get() == Mode.Control);
    private final Setting<Boolean> smartFall = this.sgSpeed.booleanSetting("Directional Gravity", true, "Only applies downward gravity when looking below the horizon.", () -> this.mode.get() == Mode.Wasp);
    private final Setting<Double> fallSpeed = this.sgSpeed.doubleSetting("Glide Descent", 0.01, 0.0, 1.0, 0.1, "The constant rate of descent while gliding forward.", () -> this.mode.get() == Mode.Control);

    private boolean moving;
    private float yaw;
    private float pitch;
    private float p;
    private double velocity;
    private int sinceFalling;
    private int sinceJump;
    private boolean sus;

    public ElytraFly() {
        super("Elytra Fly", "Provides various methods of powered flight and improved control while using an Elytra.", SubCategory.MOVEMENT, true);
        INSTANCE = this;
    }

    public static ElytraFly getInstance() {
        return INSTANCE;
    }

    @Override
    public String getInfo() {
        return this.mode.get() == Mode.Bounce
                ? String.format("%.1f, %.1f", this.getPitch(), BlackOut.mc.player.getVelocity().horizontalLength() * 20.0)
                : this.mode.get().name();
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        switch (this.mode.get()) {
            case Wasp:
                this.waspTick(event);
                break;
            case Control:
                this.controlTick(event);
        }
    }

    @Event
    public void onTick(TickEvent.Post event) {
        if (this.mode.get() == Mode.Bounce && BlackOut.mc.player != null) {
            BlackOut.mc.player.setSprinting(true);
        }
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (this.mode.get() == Mode.Bounce && BlackOut.mc.player != null) {
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

    public boolean isBouncing() {
        return this.mode.get() == Mode.Bounce && (BlackOut.mc.player.isFallFlying() || this.sinceFalling < 5);
    }

    public float getPitch() {
        return this.sus
                ? BlackOut.mc.player.getPitch()
                : MathHelper.clampedLerp(
                this.slowPitch.get().floatValue(), this.fastPitch.get().floatValue(), (float) BlackOut.mc.player.getVelocity().length()
        );
    }

    public void waspTick(MoveEvent.Pre event) {
        if (BlackOut.mc.player != null) {
            if (BlackOut.mc.player.isFallFlying()) {
                this.updateWaspMovement();
                this.pitch = BlackOut.mc.player.getPitch();
                double cos = Math.cos(Math.toRadians(this.yaw + 90.0F));
                double sin = Math.sin(Math.toRadians(this.yaw + 90.0F));
                double x = this.moving ? cos * this.horizontal.get() : 0.0;
                double y = -this.fallSpeed.get();
                double z = this.moving ? sin * this.horizontal.get() : 0.0;
                if (this.smartFall.get()) {
                    y *= Math.abs(Math.sin(Math.toRadians(this.pitch)));
                }

                if (BlackOut.mc.options.sneakKey.isPressed() && !BlackOut.mc.options.jumpKey.isPressed()) {
                    y = -this.down.get();
                }

                if (!BlackOut.mc.options.sneakKey.isPressed() && BlackOut.mc.options.jumpKey.isPressed()) {
                    y = this.up.get();
                }

                event.set(this, x, y, z);
                BlackOut.mc.player.setVelocity(0.0, 0.0, 0.0);
            }
        }
    }

    private void updateWaspMovement() {
        float yaw = BlackOut.mc.player.getYaw();
        float f = BlackOut.mc.player.input.movementForward;
        float s = BlackOut.mc.player.input.movementSideways;
        if (f > 0.0F) {
            this.moving = true;
            yaw += s > 0.0F ? -45.0F : (s < 0.0F ? 45.0F : 0.0F);
        } else if (f < 0.0F) {
            this.moving = true;
            yaw += s > 0.0F ? -135.0F : (s < 0.0F ? 135.0F : 180.0F);
        } else {
            this.moving = s != 0.0F;
            yaw += s > 0.0F ? -90.0F : (s < 0.0F ? 90.0F : 0.0F);
        }

        this.yaw = yaw;
    }

    public void controlTick(MoveEvent.Pre event) {
        if (BlackOut.mc.player != null) {
            if (BlackOut.mc.player.isFallFlying()) {
                this.updateControlMovement();
                this.pitch = 0.0F;
                boolean movingUp = false;
                if (!BlackOut.mc.options.sneakKey.isPressed() && BlackOut.mc.options.jumpKey.isPressed() && this.velocity > this.speed.get() * 0.4) {
                    this.p = (float) Math.min(this.p + 0.1 * (1.0F - this.p) * (1.0F - this.p) * (1.0F - this.p), 1.0);
                    this.pitch = Math.max(Math.max(this.p, 0.0F) * -90.0F, -90.0F);
                    movingUp = true;
                    this.moving = false;
                } else {
                    this.velocity = this.speed.get();
                    this.p = -0.2F;
                }

                this.velocity = this.moving ? this.speed.get() : Math.min(this.velocity + Math.sin(Math.toRadians(this.pitch)) * 0.08, this.speed.get());
                double cos = Math.cos(Math.toRadians(this.yaw + 90.0F));
                double sin = Math.sin(Math.toRadians(this.yaw + 90.0F));
                double x = this.moving && !movingUp ? cos * this.speed.get() : (movingUp ? this.velocity * Math.cos(Math.toRadians(this.pitch)) * cos : 0.0);
                double y = this.pitch < 0.0F
                        ? this.velocity * this.upMultiplier.get() * -Math.sin(Math.toRadians(this.pitch)) * this.velocity
                        : -this.fallSpeed.get();
                double z = this.moving && !movingUp ? sin * this.speed.get() : (movingUp ? this.velocity * Math.cos(Math.toRadians(this.pitch)) * sin : 0.0);
                y *= Math.abs(Math.sin(Math.toRadians(movingUp ? this.pitch : BlackOut.mc.player.getPitch())));
                if (BlackOut.mc.options.sneakKey.isPressed() && !BlackOut.mc.options.jumpKey.isPressed()) {
                    y = -this.down.get();
                }

                event.set(this, x, y, z);
                BlackOut.mc.player.setVelocity(0.0, 0.0, 0.0);
            }
        }
    }

    private void updateControlMovement() {
        float yaw = BlackOut.mc.player.getYaw();
        float f = BlackOut.mc.player.input.movementForward;
        float s = BlackOut.mc.player.input.movementSideways;
        if (f > 0.0F) {
            this.moving = true;
            yaw += s > 0.0F ? -45.0F : (s < 0.0F ? 45.0F : 0.0F);
        } else if (f < 0.0F) {
            this.moving = true;
            yaw += s > 0.0F ? -135.0F : (s < 0.0F ? 135.0F : 180.0F);
        } else {
            this.moving = s != 0.0F;
            yaw += s > 0.0F ? -90.0F : (s < 0.0F ? 90.0F : 0.0F);
        }

        this.yaw = yaw;
    }

    public enum Mode {
        Wasp,
        Control,
        Bounce
    }
}
