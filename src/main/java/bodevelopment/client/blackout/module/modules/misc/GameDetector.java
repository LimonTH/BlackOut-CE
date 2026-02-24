package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.GameJoinEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.module.modules.combat.offensive.Aura;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.InvUtils;
import bodevelopment.client.blackout.util.SoundUtils;
import net.minecraft.item.Items;

public class GameDetector extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Boolean> reEnable = this.sgGeneral.booleanSetting("Re Enable on game start", false, ".");
    public final Setting<Boolean> disable = this.sgGeneral.booleanSetting("Disable on game end", true, ".");
    private final SettingGroup sgDetect = this.addGroup("Detect");
    public final Setting<Boolean> capabilities = this.sgDetect.booleanSetting("Capabilities", true, ".");
    public final Setting<Boolean> compass = this.sgDetect.booleanSetting("Compass", true, ".");
    public final Setting<Boolean> slime = this.sgDetect.booleanSetting("Slime", true, ".");
    public final Setting<Boolean> test = this.sgDetect.booleanSetting("test", true, ".");
    public boolean gameStarted = false;
    private boolean prevState = false;
    private boolean disabledAura = false;
    private boolean disabledStealer = false;
    private boolean disabledManager = false;

    public GameDetector() {
        super("Game Detector", "Detects when a game is in progress and toggles modules based on it", SubCategory.MISC, true);
    }

    @Override
    public String getInfo() {
        return this.gameStarted ? "Started" : "Waiting for start";
    }

    @Override
    public void onDisable() {
        this.gameStarted = false;
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (BlackOut.mc.player.age >= 20) {
                if (this.gameStarted != this.prevState) {
                    this.toggleModules(this.gameStarted);
                    this.prevState = this.gameStarted;
                }

                this.gameStarted = !this.capabilities.get()
                        || !BlackOut.mc.player.getAbilities().allowFlying
                        && !BlackOut.mc.player.getAbilities().flying
                        && !BlackOut.mc.player.getAbilities().invulnerable;
                if (this.compass.get() && InvUtils.count(true, false, stack -> stack.getItem() == Items.COMPASS) > 0) {
                    this.gameStarted = false;
                }

                if (this.slime.get() && InvUtils.count(true, false, stack -> stack.getItem() == Items.SLIME_BALL) > 0) {
                    this.gameStarted = false;
                }

                if (this.test.get()) {
                    this.gameStarted = false;
                }
            }
        }
    }

    private void toggleModules(boolean enable) {
        Aura auraModule = Aura.getInstance();
        Stealer stealerModule = Stealer.getInstance();
        Manager managerModule = Manager.getInstance();
        if (enable && this.reEnable.get()) {
            if (this.disabledAura) {
                auraModule.silentEnable();
            }

            if (this.disabledStealer) {
                stealerModule.silentEnable();
            }

            if (this.disabledManager) {
                managerModule.silentEnable();
            }

            this.sendNotification(this.getDisplayName() + " enabled some modules", "Game start detected");
        } else if (this.disable.get()) {
            if (auraModule.enabled) {
                auraModule.silentDisable();
                this.disabledAura = true;
            }

            if (stealerModule.enabled) {
                stealerModule.silentDisable();
                this.disabledStealer = true;
            }

            if (managerModule.enabled) {
                managerModule.silentDisable();
                this.disabledManager = true;
            }

            this.sendNotification(this.getDisplayName() + " disabled some modules", "Game end detected");
        }
    }

    private void sendNotification(String message, String bigText) {
        Notifications notifications = Notifications.getInstance();
        if (notifications.chatNotifications.get()) {
            this.sendMessage(this.getDisplayName() + " " + message);
        }

        Managers.NOTIFICATIONS.addNotification(message == null ? "Disabled " + this.getDisplayName() : message, bigText, 5.0, Notifications.Type.Info);
        if (notifications.sound.get()) {
            SoundUtils.play(1.0F, 1.0F, "disable");
        }
    }

    @Event
    public void onGameJoin(GameJoinEvent event) {
        this.gameStarted = false;
    }

    public boolean shouldHibernate() {
        return this.enabled && this.gameStarted;
    }
}
