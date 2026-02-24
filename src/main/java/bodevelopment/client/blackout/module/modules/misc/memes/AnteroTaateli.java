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
    private final Setting<Boolean> iFriends = this.sgGeneral.booleanSetting("Ignore Friends", true, "Do we ignore friends");
    private final Setting<Double> delay = this.sgGeneral.doubleSetting("Delay (Ticks)", 100.0, 10.0, 500.0, 1.0, "Ticks between messages.");

    private final Random r = new Random();
    private final String[] messages = new String[]{
            "Hey brokies, Top G here.",
            "Top G drinks sparkling water and breathes air.",
            "I hate dead people, all you do is fucking laying down like pussies.",
            "Get up and do some push-ups.",
            "Top G is never late, time is just running ahead of schedule.",
            "<NAME>, what color is your Bugatti?",
            "Hello, I am Andrew Tate and you are a brokie.",
            "Instead of playing a block game, how bout you pick up some women.",
            "We are living inside of The Matrix, and I’m Morpheus.",
            "The Matrix has attacked me!",
            "Fucking vape! Vape comes out of the motherfucker. Fucking vape!",
            "You don't need vape, breathe air!",
            "Are you good enough on your worst day to defeat your opponents on their best day?",
            "Being poor, weak and broke is your fault. Build yourself.",
            "The biggest difference between success and failure is getting started.",
            "Being rich is even better than you imagine it to be.",
            "You're a fucking brokie!",
            "<NAME>, you think the Matrix is a joke? Work harder!",
            "My unmatched perspicacity coupled with my sheer indefatigability makes me a feared adversary in any realm of human endeavor.",
            "Coffee is for closers. Why are you drinking bean water if you haven't closed a deal today?",
            "I don't sleep, I wait. Because the world is full of opportunities and I’m taking all of them.",
            "Emotional control is not a lack of emotion; it’s a necessary function of a High Value Man.",
            "Every time you feel like quitting, remember who’s watching. The Matrix wants you to fail.",
            "<NAME>, you are a victim of your own mind. Free yourself and get rich.",
            "If you’re not making money, you’re not a man. You’re a liability.",
            "The world is a competitive place. You are either the hammer or the nail.",
            "I have everything every man has ever wanted. I am the Top G.",
            "I don’t care if you don’t like me. I like me. And my Bugatti likes me.",
            "I reached the top of the mountain and realized I am the mountain.",
            "Most people are not actually lazy. They just have goals that suck. Get better goals.",
            "Stress is not real. It’s just your body telling you to work harder.",
            "You think you’re 'depressed'? No, you’re just bored and broke. Go to the gym.",
            "Imagine being a brokie in 2026. Absolutely disgusting.",
            "Focus on what you want to become, not what you are. Unless you are me, then stay exactly the same."
    };

    private int timer = 0;
    private int lastIndex = -1;

    public AnteroTaateli() {
        super("Auto Andrew Tate", "What colour is your bugatti?", SubCategory.MEMES, true);
    }

    @Override
    public void onEnable() {
        this.timer = 0;
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player == null || BlackOut.mc.world == null) return;

        this.timer++;

        if (this.timer >= this.delay.get()) {
            PlayerEntity target = this.getClosest();

            if (target != null) {
                ChatUtils.sendMessage(this.generateMessage(target));
                this.timer = 0;
            }
        }
    }

    private String generateMessage(PlayerEntity pl) {
        int index;
        do {
            index = r.nextInt(messages.length);
        } while (index == lastIndex && messages.length > 1);

        this.lastIndex = index;
        return messages[index].replace("<NAME>", pl.getName().getString());
    }

    private PlayerEntity getClosest() {
        PlayerEntity closest = null;
        double minDistance = Double.MAX_VALUE;

        for (PlayerEntity player : BlackOut.mc.world.getPlayers()) {
            if (player == BlackOut.mc.player) continue;
            if (this.iFriends.get() && Managers.FRIENDS.isFriend(player)) continue;

            if (player.isSpectator()) continue;

            double dist = BlackOut.mc.player.distanceTo(player);
            if (dist < minDistance) {
                minDistance = dist;
                closest = player;
            }
        }
        return closest;
    }
}