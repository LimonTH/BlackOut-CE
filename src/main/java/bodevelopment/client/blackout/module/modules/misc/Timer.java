package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class Timer extends Module {
    private static Timer INSTANCE;

    public final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<TimerMode> mode = this.sgGeneral.enumSetting("Timer Mode", TimerMode.Time, "Determines the synchronization method for the speed increase.");
    public final Setting<Double> timeMulti = this.sgGeneral.doubleSetting("Time Multiplier", 1.0, 0.05, 10.0, 0.04, "Modifies the client-side tick rate to speed up or slow down the game world.", () -> this.mode.get() == TimerMode.Time);
    public final Setting<Double> physicsMulti = this.sgGeneral.doubleSetting("Physics Multiplier", 1.0, 0.0, 10.0, 0.04, "Triggers extra physics updates per tick to simulate increased movement speed.", () -> this.mode.get() == TimerMode.Physics);

    private static float packets = 0.0F;
    private static float speed = -1.0F;

    public Timer() {
        super("Timer", "Manipulates the client-side tick rate or physics frequency to accelerate character actions and movement.", SubCategory.MISC, true);
        INSTANCE = this;
    }

    public static Timer getInstance() {
        return INSTANCE;
    }

    public static void set(float newSpeed) {
        speed = newSpeed;
    }

    public static void reset() {
        speed = -69.0F;
    }

    @Override
    public String getInfo() {
        return this.mode.get().name();
    }

    @Override
    public boolean shouldSkipListeners() {
        return BlackOut.mc.world == null;
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && this.enabled && this.mode.get() == TimerMode.Physics) {
            for (packets = packets + (this.physicsMulti.get().floatValue() - 1.0F); packets > 0.0F; packets--) {
                BlackOut.mc.player.tickMovement();
            }
        }
    }

    public float getTickTime() {
        return 1000.0F / getInstance().getTPS();
    }

    public float getTPS() {
        if (this.mode.get() == TimerMode.Physics) {
            return 20.0F;
        } else if (speed <= 0.0F) {
            return this.enabled ? this.timeMulti.get().floatValue() * 20.0F : 20.0F;
        } else {
            return speed * 20.0F;
        }
    }

    public enum TimerMode {
        Physics,
        Time
    }
}
