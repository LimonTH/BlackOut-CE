package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.misc.Timer;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class TickShift extends Module {
    private static TickShift INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgCharge = this.addGroup("Charge");

    public final Setting<SmoothMode> smooth = this.sgGeneral.enumSetting("Interpolation Mode", SmoothMode.Exponent, "The mathematical curve used to transition the timer back to its base value.");
    public final Setting<Integer> packets = this.sgGeneral.intSetting("Packet Capacity", 20, 0, 100, 1, "The maximum number of game ticks that can be stored while stationary.");
    public final Setting<Double> timer = this.sgGeneral.doubleSetting("Shift Intensity", 2.0, 0.0, 10.0, 0.1, "The clock speed multiplier applied when discharging stored packets.");
    private final Setting<Boolean> step = this.sgGeneral.booleanSetting("Synchronize Step", false, "Allows the Step module to utilize the accelerated timer for faster elevation changes.");

    public final Setting<ChargeMode> chargeMode = this.sgCharge.enumSetting("Accumulation Logic", ChargeMode.Strict, "Defines the conditions required to begin storing game ticks.");
    public final Setting<Double> chargeSpeed = this.sgCharge.doubleSetting("Charge Velocity", 1.0, 0.0, 5.0, 0.05, "The rate at which the packet buffer is filled.");

    public double unSent = 0.0;
    private boolean lastMoving = false;
    private boolean shouldResetTimer = false;

    public TickShift() {
        super("Tick Shift", "Accumulates unused game ticks while stationary and discharges them upon movement to achieve a burst of speed.", SubCategory.MOVEMENT, true);
        INSTANCE = this;
    }

    public static TickShift getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        this.unSent = 0.0;
    }

    @Override
    public void onDisable() {
        if (this.shouldResetTimer) {
            Timer.reset();
        }

        this.shouldResetTimer = false;
    }

    @Override
    public String getInfo() {
        return String.valueOf(this.unSent);
    }

    @Event
    public void onTick(TickEvent.Post e) {
        if (BlackOut.mc.player != null) {
            if (this.unSent > 0.0 && this.lastMoving) {
                Timer.set(this.getTimer());
                this.lastMoving = false;
                this.shouldResetTimer = true;
            } else if (this.shouldResetTimer) {
                Timer.reset();
                this.shouldResetTimer = false;
            }
        }
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        if (event.movement.length() > 0.0 && (!(event.movement.length() > 0.0784) || !(event.movement.length() < 0.0785))) {
            this.unSent = Math.max(0.0, this.unSent - 1.0);
            this.lastMoving = true;
        }
    }

    private float getTimer() {
        if (this.smooth.get() == SmoothMode.Disabled) {
            return this.timer.get().floatValue();
        } else {
            double progress = 1.0 - this.unSent / this.packets.get();
            if (this.smooth.get() == SmoothMode.Exponent) {
                progress *= progress * progress * progress * progress;
            }

            return (float) (1.0 + (this.timer.get() - 1.0) * (1.0 - progress));
        }
    }

    public boolean canCharge(boolean sent, boolean move) {
        return switch (this.chargeMode.get()) {
            case Strict -> !sent;
            case Semi -> !sent || !move;
        };
    }

    public boolean shouldStep() {
        return this.enabled && this.step.get() && this.shouldResetTimer;
    }

    public enum ChargeMode {
        Strict,
        Semi
    }

    public enum SmoothMode {
        Disabled,
        Normal,
        Exponent
    }
}
