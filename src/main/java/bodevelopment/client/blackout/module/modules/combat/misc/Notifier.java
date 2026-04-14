package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.util.PlayerUtils;
import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PopEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.SoundUtils;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.List;

public class Notifier extends Module {
    private static Notifier INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgWeakness = this.addGroup("Weakness");
    private final SettingGroup sgVisualRange = this.addGroup("Visual Range");
    private final SettingGroup sgPops = this.addGroup("Pops");

    public final Setting<Mode> mode = this.sgGeneral.enumSetting("Notify mode", Mode.Hud,
            "Where the alerts appear. 'Hud' uses the client's notification system, 'Chat' sends messages only you can see.");

    private final Setting<Boolean> pops = this.sgPops.booleanSetting("Pop Counter", true,
            "Tracks and displays how many totems a player has used.");
    private final Setting<Boolean> iOwn = this.sgPops.booleanSetting("Ignore Own", true,
            "Toggle to stop receiving notifications when YOU pop a totem.");
    private final Setting<Boolean> iFriends = this.sgPops.booleanSetting("Ignore Friends", true,
            "Toggle to stop receiving notifications when your FRIENDS pop a totem.");

    private final Setting<Boolean> visualRange = this.sgVisualRange.booleanSetting("Visual Range", true,
            "Notifies you when a player enters or leaves your render distance.");
    private final Setting<Boolean> vrIgnoreFriends = this.sgVisualRange.booleanSetting("Ignore Friends", true,
            "Toggle to stop receiving visual range notifications for your friends.");
    private final Setting<Boolean> vrSound = this.sgVisualRange.booleanSetting("Sound Notification", true,
            "Plays a custom sound when a player enters or leaves visual range.");

    private final Setting<Boolean> weakness = this.sgWeakness.booleanSetting("Weakness", true,
            "Alerts you when you are affected by the Weakness effect.");
    private final Setting<Boolean> single = this.sgWeakness.booleanSetting("Single", true,
            "Only sends one notification when you get weakness, rather than spamming.");
    private final Setting<Double> delay = this.sgWeakness.doubleSetting("Delay", 5.0, 0.0, 100.0, 1.0,
            "The time (in seconds) between repeated weakness alerts if 'Single' is off.");

    private double timer = 0.0;
    private boolean last = false;
    private final List<AbstractClientPlayer> knownPlayers = new ArrayList<>();

    public Notifier() {
        super("Notifier", "Notifies you about events like effects or totems.", SubCategory.MISC_COMBAT, true);
        INSTANCE = this;
    }

    public static Notifier getInstance() {
        return INSTANCE;
    }

    @Event
    public void onPop(PopEvent event) {
        if (this.pops.get()) {
            if (!this.iOwn.get() || !event.player.equals(BlackOut.mc.player)) {
                if (!this.iFriends.get() || !Managers.FRIENDS.isFriend(event.player)) {
                    this.sendNotification(this.getPopString(event.player, event.number));
                }
            }
        }
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (PlayerUtils.isInGame()) {
            if (this.weakness.get()) {
                if (!BlackOut.mc.player.hasEffect(MobEffects.WEAKNESS)) {
                    if (this.last) {
                        this.last = false;
                        this.sendNotification("You no longer have weakness!");
                    }
                } else {
                    if (this.single.get()) {
                        if (!this.last) {
                            this.last = true;
                            this.sendNotification("You have weakness!");
                        }
                    } else if (this.timer > 0.0) {
                        this.timer--;
                    } else {
                        this.timer = this.delay.get();
                        this.last = true;
                        this.sendNotification("You have weakness!");
                    }
                }
            }
            if (this.visualRange.get() && BlackOut.mc.level != null) {
                List<AbstractClientPlayer> currentPlayers = BlackOut.mc.level.players();

                for (AbstractClientPlayer player : currentPlayers) {
                    if (player == BlackOut.mc.player) continue;

                    if (!this.knownPlayers.contains(player)) {
                        this.knownPlayers.add(player);
                        if (this.vrIgnoreFriends.get() && Managers.FRIENDS.isFriend(player)) continue;
                        this.sendNotification(player.getGameProfile().getName() + " has entered visual range!");
                        this.playVrSound("vrenter");
                    }
                }

                this.knownPlayers.removeIf(player -> {
                    if (!currentPlayers.contains(player)) {
                        if (!(this.vrIgnoreFriends.get() && Managers.FRIENDS.isFriend(player))) {
                            this.sendNotification(player.getGameProfile().getName() + " has left visual range!");
                            this.playVrSound("vrleave");
                        }
                        return true;
                    }
                    return false;
                });
            }

        } else {
            this.knownPlayers.clear();
        }
    }

    private void playVrSound(String soundName) {
        if (!this.vrSound.get() || BlackOut.mc.player == null) return;
        SoundUtils.play(1.0f, 0.8f, soundName);
    }

    private String getPopString(AbstractClientPlayer player, int pops) {
        return player.getName().getString() + " has popped their " + pops + this.getSuffix(pops) + " totem!";
    }

    private String getSuffix(int i) {
        if (i >= 11 && i <= 13) {
            return "th";
        } else {
            return switch (i % 10) {
                case 1 -> "st";
                case 2 -> "nd";
                case 3 -> "rd";
                default -> "th";
            };
        }
    }

    private void sendNotification(String info) {
        switch (this.mode.get()) {
            case Hud:
                Managers.NOTIFICATIONS.addNotification(info, this.getDisplayName(), 2.0, Notifications.Type.Info);
                break;
            case Chat:
                this.sendMessage(info);
        }
    }

    public enum Mode {
        Chat,
        Hud
    }
}