package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.event.events.ModuleEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.module.setting.Setting;

import java.util.Stack;

public class SafeMyFPS extends Module {
    private final Setting<Integer> minFps = this.sgModule.intSetting("Min FPS", 5, 1, 20, 1, "Disables last enabled module if FPS drops below this.");
    private final Setting<Double> delay = this.sgModule.doubleSetting("Trigger Delay", 1.0, 0.0, 10.0, 0.1, "How long (seconds) FPS must stay low.");

    private final Stack<Module> history = new Stack<>();
    private long lowFpsTime = -1;

    public SafeMyFPS() {
        super("Safe FPS", "Emergency switch to save your FPS.", SubCategory.MISC, true);
    }

    @Event
    public void onModuleEnable(ModuleEvent.Enable event) {
        if (event.module != this) {
            history.remove(event.module);
            history.push(event.module);
        }
    }

    @Event
    public void onModuleDisable(ModuleEvent.Disable event) {
        history.remove(event.module);
    }

    @Event
    public void onTick(TickEvent.Post event) {
        if (BlackOut.mc.world == null) return;
        boolean focused = BlackOut.mc.isWindowFocused();
        if (!focused) {
            lowFpsTime = -1;
            return;
        }
        int currentFps = BlackOut.mc.getCurrentFps();
        if (currentFps < minFps.get()) {
            if (BlackOut.mc.options.getMaxFps().getValue() <= minFps.get()) {
                lowFpsTime = -1;
                return;
            }

            if (lowFpsTime == -1) {
                lowFpsTime = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - lowFpsTime > delay.get() * 1000) {
                executePanic();
            }
        } else {
            lowFpsTime = -1;
        }
    }

    private void executePanic() {
        if (history.isEmpty()) return;
        Module culprit = history.pop();

        if (culprit != null && culprit.enabled) {
            culprit.disable("FPS Disabled " + culprit.getDisplayName(), 3, Notifications.Type.Alert);

            lowFpsTime = -1;
        }
    }

    @Override
    public void onDisable() {
        history.clear();
        lowFpsTime = -1;
    }
}