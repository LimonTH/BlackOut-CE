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
    public final Setting<SmoothMode> smooth = this.sgGeneral.enumSetting("Smoothness", SmoothMode.Exponent, ".");
    public final Setting<Integer> packets = this.sgGeneral.intSetting("Packets", 20, 0, 100, 1, "How many packets to store for later use.");
    public final Setting<Double> timer = this.sgGeneral.doubleSetting("Timer", 2.0, 0.0, 10.0, 0.1, "How many packets to send every movement tick.");
    private final SettingGroup sgCharge = this.addGroup("Charge");
    public final Setting<ChargeMode> chargeMode = this.sgCharge.enumSetting("Charge Mode", ChargeMode.Strict, ".");
    public final Setting<Double> chargeSpeed = this.sgCharge.doubleSetting("Charge Speed", 1.0, 0.0, 5.0, 0.05, ".");
    private final Setting<Boolean> step = this.sgGeneral.booleanSetting("Use Step", false, ".");
    public double unSent = 0.0;
    private boolean lastMoving = false;
    private boolean shouldResetTimer = false;

    public TickShift() {
        super("Tick Shift", "Stores packets when standing still and uses them when you start moving.", SubCategory.MOVEMENT, true);
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
