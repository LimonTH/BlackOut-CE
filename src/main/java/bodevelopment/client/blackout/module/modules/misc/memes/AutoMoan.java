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

import java.util.concurrent.ThreadLocalRandom;

public class AutoMoan extends Module {
    private static final String[] submissive = new String[]{
            "fuck me harder daddy",
            "deeper! daddy deeper!",
            "Fuck yes you're so big!",
            "I love your cock %s!",
            "Do not stop fucking my ass before i cum!",
            "Oh you're so hard for me",
            "Want to widen my ass up %s?",
            "I love you daddy",
            "Make my bussy pop",
            "%s loves my bussy so much",
            "i made %s cum so hard with my tight bussy",
            "Your cock is so big and juicy daddy!",
            "Please fuck me as hard as you can",
            "im %s's personal femboy cumdumpster!",
            "Please shoot your hot load deep inside me daddy!",
            "I love how %s's dick feels inside of me!",
            "%s gets so hard when he sees my ass!",
            "%s really loves fucking my ass really hard!",
            "Fill me up with your warm milk %s~",
            "I'm just a submissive little pet for %s",
            "Please punish me %s, I've been a bad boy!",
            "Breed me like a good boy %s...",
            "I'm shaking for you %s, please don't stop~",
            "My bussy belongs to %s only!",
            "Can I be your favorite toy, %s?",
            "Look how wet I am for you %s~",
            "I'll do anything for you daddy %s!",
            "Put your leash on me and take me home %s..."
    };

    private static final String[] dominant = new String[]{
            "Be a good boy for daddy",
            "I love pounding your ass %s!",
            "Give your bussy to daddy!",
            "I love how you drip pre-cum while i fuck your ass %s",
            "Slurp up and down my cock like a good boy",
            "Come and jump on daddy's cock %s",
            "I love how you look at me while you suck me off %s",
            "%s looks so cute when i fuck him",
            "%s's bussy is so incredibly tight!",
            "%s takes dick like the good boy he is",
            "I love how you shake your ass on my dick",
            "%s moans so cutely when i fuck his ass",
            "%s is the best cumdumpster there is!",
            "%s is always horny and ready for his daddy's dick",
            "My dick gets rock hard every time i see %s",
            "Stay still %s, daddy isn't finished yet~",
            "You belong to me now, %s. Say my name!",
            "Good boy %s, take it all!",
            "Does %s want some of daddy's special milk?",
            "You're such a messy little slut, %s!",
            "Who's a good little cum-slut? That's right, it's %s!",
            "Keep stretching for me %s, you're doing so well~",
            "I'm going to ruin you, %s!",
            "Cry for me %s, I love those sounds...",
            "I own every inch of your body %s, never forget it."
    };

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<MoanMode> moanmode = this.sgGeneral.e("Message Mode", MoanMode.Submissive, "What kind of messages to send.");
    private final Setting<Boolean> ignoreFriends = this.sgGeneral.b("Ignore Friends", true, "Doesn't send messages targeted to friends.");
    private final Setting<Integer> delay = this.sgGeneral.i("Tick Delay", 100, 10, 500, 1, "Tick delay between moans.");

    private int timer = 0;

    public AutoMoan() {
        super("Auto Moan", "Moans sexual things to the closest person.", SubCategory.MEMES, true);
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
                this.moanmode.get().send(target.getName().getString());
                this.timer = 0;
            }
        }
    }

    private PlayerEntity getClosest() {
        PlayerEntity closest = null;
        double minDistance = Double.MAX_VALUE;

        for (PlayerEntity player : BlackOut.mc.world.getPlayers()) {
            if (player == BlackOut.mc.player) continue;
            if (this.ignoreFriends.get() && Managers.FRIENDS.isFriend(player)) continue;
            if (player.isSpectator()) continue;

            double dist = BlackOut.mc.player.squaredDistanceTo(player);
            if (dist < minDistance) {
                minDistance = dist;
                closest = player;
            }
        }
        return closest;
    }

    public enum MoanMode {
        Dominant(AutoMoan.dominant),
        Submissive(AutoMoan.submissive);

        private final String[] messages;
        private int lastNum = -1;

        MoanMode(String[] messages) {
            this.messages = messages;
        }

        private void send(String targetName) {
            int num;
            if (messages.length <= 1) {
                num = 0;
            } else {
                do {
                    num = ThreadLocalRandom.current().nextInt(messages.length);
                } while (num == lastNum);
            }

            this.lastNum = num;
            ChatUtils.sendMessage(this.messages[num].replace("%s", targetName));
        }
    }
}