package bodevelopment.client.blackout.module.modules.misc.memes;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.ChatUtils;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Random;

public class AnteroTaateli extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<Boolean> iFriends = this.sgGeneral.b("Ignore Friends", true, "Do we ignore friends");
    private final Setting<Double> delay = this.sgGeneral.d("Delay", 50.0, 0.0, 100.0, 1.0, "How much delay to use");
    private final Random r = new Random();
    private final String[] messages = new String[]{ // TODO: добавить фразочек AnteroTaateli
            "Hey brokies top G here.",
            "Top G drinks sparkling water and breathes air.",
            "I hate dead people all you do is fucking laying down like pussies.",
            "Get up and do some push-ups.",
            "Top G is never late time is just running ahead of schedule.",
            "<NAME>, what color is your Bugatti?",
            "Hello i am Andrew Tate and you are a brokie.",
            "Instead of playing a block game how bout you pick up some women.",
            "We are living inside of The Matrix, and I’m Morpheus.",
            "The Matrix has attacked me.",
            "Fucking vape! Vape comes out of the motherfucker. Fucking vape!",
            "You don't need vape breathe air!",
            "Are you good enough on your worst day to defeat your opponents on their best day?",
            "Being poor, weak and broke is your fault. The only person who can make you rich and strong is you. Build yourself.",
            "The biggest difference between success and failure is getting started.",
            "There was a guy who looked at me obviously trying to hurt my dignity so i pulled out my RPG and obliterated that fucker",
            "Being rich is even better than you imagine it to be.",
            "Your a fucking brokie!"
    };
    private double timer = 0.0;
    private int lastIndex = 0;

    public AnteroTaateli() {
        super("Auto Andrew Tate", "What colour is your bugatti?", SubCategory.MEMES, true);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        this.timer++;
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            PlayerEntity bugatti = this.getClosest();
            if (this.timer >= this.delay.get() && bugatti != null) {
                this.timer = 0.0;
                ChatUtils.sendMessage(this.getMessage(bugatti));
            }
        }
    }

    private String getMessage(PlayerEntity pl) {
        int index = this.r.nextInt(0, this.messages.length);
        String msg = this.messages[index];
        if (index == this.lastIndex) {
            if (index >= this.messages.length - 1) {
                index = 0;
            } else {
                index++;
            }
        }

        this.lastIndex = index;
        return msg.replace("<NAME>", pl.getName().getString());
    }

    private PlayerEntity getClosest() {
        PlayerEntity closest = null;
        float distance = -1.0F;
        if (!BlackOut.mc.world.getPlayers().isEmpty()) {
            for (PlayerEntity player : BlackOut.mc.world.getPlayers()) {
                if (player != BlackOut.mc.player
                        && (!this.iFriends.get() || !Managers.FRIENDS.isFriend(player))
                        && (closest == null || !(BlackOut.mc.player.distanceTo(player) >= distance))) {
                    closest = player;
                    distance = (float) BlackOut.mc.player.getPos().distanceTo(player.getPos());
                }
            }
        }

        return closest;
    }
}
