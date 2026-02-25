package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.OLEPOSSUtils;

public class AntiVoid extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Mode> mode = this.sgGeneral.enumSetting("Mode", Mode.Motion, "The method used to prevent falling into the void.");
    private final Setting<Double> d = this.sgGeneral.doubleSetting("Activation Distance", 2.5, 0.0, 10.0, 0.5, "The fall distance required before the anti-void logic triggers.");
    private final Setting<Boolean> voidCheck = this.sgGeneral.booleanSetting("Void Check", true, "Only activates if there are no solid blocks directly beneath the player.");

    private double prevOG = 0.0;

    public AntiVoid() {
        super("Anti Void", "Prevents the player from falling into the void by manipulating vertical movement or position.", SubCategory.MOVEMENT, true);
    }

    @Override
    public String getInfo() {
        return this.mode.get().name();
    }

    @Event
    public void onMove(MoveEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (BlackOut.mc.player.isOnGround()) {
                this.prevOG = BlackOut.mc.player.getY();
            }

            if (!(BlackOut.mc.player.fallDistance < this.d.get())) {
                if (!this.voidCheck.get() || this.aboveVoid()) {
                    switch (this.mode.get()) {
                        case Motion:
                            event.setY(this, 0.42);
                            break;
                        case Freeze:
                            event.setY(this, 0.0);
                            break;
                        case Position:
                            BlackOut.mc
                                    .player
                                    .setPosition(BlackOut.mc.player.getX(), BlackOut.mc.player.getY() + 1.0, BlackOut.mc.player.getZ());
                    }

                    BlackOut.mc.player.fallDistance = 0.0F;
                    Managers.NOTIFICATIONS.addNotification("Attempted to save you from the void!", this.getDisplayName(), 2.0, Notifications.Type.Info);
                }
            }
        }
    }

    public boolean aboveVoid() {
        for (int i = 1; i < 30.0 - Math.ceil(this.prevOG) + BlackOut.mc.player.getBlockY(); i++) {
            if (OLEPOSSUtils.collidable(BlackOut.mc.player.getBlockPos().down(i))) {
                return false;
            }
        }

        return true;
    }

    public enum Mode {
        Motion,
        Freeze,
        Position
    }
}
