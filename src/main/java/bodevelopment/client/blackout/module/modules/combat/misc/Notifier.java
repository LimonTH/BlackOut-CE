package bodevelopment.client.blackout.module.modules.combat.misc;

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
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;

public class Notifier extends Module {
    private static Notifier INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Mode> mode = this.sgGeneral.enumSetting("Notify mode", Mode.Hud, "How to notify you");
    private final SettingGroup sgPops = this.addGroup("Pops");
    private final SettingGroup sgWeakness = this.addGroup("Weakness");
    private final Setting<Boolean> pops = this.sgPops.booleanSetting("Pop Counter", true, "Counts Totem Pops");
    private final Setting<Boolean> iOwn = this.sgPops.booleanSetting("Ignore Own", true, "Does not send count friends pops", this.pops::get);
    private final Setting<Boolean> iFriends = this.sgPops.booleanSetting("Ignore Friends", true, "Does not send count friends pops", this.pops::get);
    private final Setting<Boolean> weakness = this.sgWeakness.booleanSetting("Weakness", true, "Notifies about getting weakness");
    private final Setting<Boolean> single = this.sgWeakness.booleanSetting("Single", true, "Only sends it once", this.weakness::get);
    private final Setting<Double> delay = this.sgWeakness.doubleSetting("Delay", 5.0, 0.0, 100.0, 1.0, "Tick delay between alerts", this.weakness::get);
    private double timer = 0.0;
    private boolean last = false;

    public Notifier() {
        super("Notifier", "Notifies you about events", SubCategory.MISC_COMBAT, true);
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
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (this.weakness.get()) {
                if (!BlackOut.mc.player.hasStatusEffect(StatusEffects.WEAKNESS)) {
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
        }
    }

    private String getPopString(AbstractClientPlayerEntity player, int pops) {
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
