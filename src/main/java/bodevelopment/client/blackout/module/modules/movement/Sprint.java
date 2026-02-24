package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class Sprint extends Module {
    private static Sprint INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<SprintMode> sprintMode = this.sgGeneral.enumSetting("Mode", SprintMode.Vanilla, "How to sprint");
    public final Setting<Boolean> hungerCheck = this.sgGeneral.booleanSetting("HungerCheck", true, "Do we check if we have enough hunger to sprint");

    public Sprint() {
        super("Sprint", "Makes you sprint", SubCategory.MOVEMENT, true);
        INSTANCE = this;
    }

    public static Sprint getInstance() {
        return INSTANCE;
    }

    @Override
    public void onDisable() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            BlackOut.mc.player.setSprinting(false);
        }
    }

    @Override
    public String getInfo() {
        return this.sprintMode.get().name();
    }

    @Event
    public void onTick(TickEvent.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null && !LongJump.getInstance().enabled) {
            if (this.hungerCheck.get() && BlackOut.mc.player.getHungerManager().getFoodLevel() < 6) {
                BlackOut.mc.player.setSprinting(false);
            } else {
                if (this.shouldSprint()) {
                    BlackOut.mc.player.setSprinting(true);
                }
            }
        }
    }

    public boolean shouldSprint() {
        return switch (this.sprintMode.get()) {
            case Vanilla -> BlackOut.mc.player.input.hasForwardMovement();
            case Omni ->
                    BlackOut.mc.player.getVelocity().getX() != 0.0 || BlackOut.mc.player.getVelocity().getZ() != 0.0;
            case Rage -> true;
        };
    }

    public enum SprintMode {
        Vanilla,
        Omni,
        Rage
    }
}
