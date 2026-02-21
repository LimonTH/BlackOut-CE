package bodevelopment.client.blackout.module.modules.misc.memes;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
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
    private static final String[] submissive = new String[]{ // TODO: Добавить фразочек AutoMoan
            "fuck me harder daddy",
            "deeper! daddy deeper!",
            "Fuck yes your so big!",
            "I love your cock %s!",
            "Do not stop fucking my ass before i cum!",
            "Oh your so hard for me",
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
            "why wont u say the last message"
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
            "%s is the best cumdupster there is!",
            "%s is always horny and ready for his daddy's dick",
            "My dick gets rock hard every time i see %s",
            "why wont u say the last message"
    };
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<MoanMode> moanmode = this.sgGeneral.e("Message Mode", MoanMode.Submissive, "What kind of messages to send.");
    private final Setting<Boolean> ignoreFriends = this.sgGeneral.b("Ignore Friends", true, "Doesn't send messages targeted to friends.");
    private final Setting<Integer> delay = this.sgGeneral.i("Tick Delay", 50, 0, 100, 1, "Tick delay between moans.");
    private double timer = 0.0;

    public AutoMoan() {
        super("Auto Moan", "Moans sexual things to the closest person.", SubCategory.MEMES, true);
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        this.timer = Math.min(this.delay.get(), this.timer + event.frameTime);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (!(this.timer++ < this.delay.get())) {
                this.MOAN();
                this.timer = 0.0;
            }
        }
    }

    private void MOAN() {
        PlayerEntity target = this.getClosest();
        if (target != null) {
            String name = target.getName().getString();
            this.moanmode.get().send(name);
        }
    }

    private PlayerEntity getClosest() {
        if (BlackOut.mc.world.getPlayers().isEmpty()) {
            return null;
        } else {
            PlayerEntity closest = null;
            double closestDistance = -1.0;

            for (PlayerEntity player : BlackOut.mc.world.getPlayers()) {
                if (player != BlackOut.mc.player && (!this.ignoreFriends.get() || !Managers.FRIENDS.isFriend(player))) {
                    double distance = BlackOut.mc.player.squaredDistanceTo(player);
                    if (closest == null || !(distance > closestDistance)) {
                        closest = player;
                        closestDistance = distance;
                    }
                }
            }

            return closest;
        }
    }

    public enum MoanMode {
        Dominant(AutoMoan.dominant),
        Submissive(AutoMoan.submissive);

        private final String[] messages;
        private int lastNum;

        MoanMode(String[] messages) {
            this.messages = messages;
        }

        private void send(String targetName) {
            int num = ThreadLocalRandom.current().nextInt(0, this.messages.length - 1);
            if (num == this.lastNum) {
                num = num < this.messages.length - 1 ? num + 1 : 0;
            }

            this.lastNum = num;
            ChatUtils.sendMessage(this.messages[num].replace("%s", targetName));
        }
    }
}
