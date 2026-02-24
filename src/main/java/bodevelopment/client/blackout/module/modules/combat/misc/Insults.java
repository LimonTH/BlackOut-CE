package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.ChatUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Insults extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Integer> range = this.sgGeneral.intSetting("Range", 25, 0, 50, 1, ".");
    public final Setting<Integer> tickDelay = this.sgGeneral.intSetting("Tick Delay", 50, 0, 100, 1, ".");
    private final SettingGroup sgKill = this.addGroup("Kill");
    public final Setting<Boolean> kill = this.sgKill.booleanSetting("Kill", true, "Should we send a message when enemy dies");
    public final Setting<MessageMode> killMsgMode = this.sgKill
            .enumSetting("Kill Message Mode", MessageMode.Exhibition, "What kind of messages to send.", () -> true);
    private final SettingGroup sgPop = this.addGroup("Pop");
    public final Setting<Boolean> pop = this.sgPop.booleanSetting("Pop", true, "Should we send a message when enemy pops a totem");
    private final Random r = new Random();
    private final List<Message> messageQueue = new LinkedList<>();
    private final String[] insults = new String[]{
            "<NAME> couldn't even beat 4 block",
            "<NAME> maybe you should be searching for your dad instead of minecraft pvp tutorials",
            "That was the easiest fight ever",
            "Remember to report me to staff!",
            "<NAME> you're almost as salty as the semen dripping from your moms mouth",
            "It's always great seeing disabled people like <NAME> play games",
            "<NAME> is a Novoline user lol",
            "<NAME> just got 10 hearted by Nostalgia",
            "I can't hack, the anticheat would ban me!",
            "<NAME> got an F on the IQ test",
            "<NAME> tried to hit disconnect but missed that too",
            "You really can't even hit a block man?",
            "<NAME> probably went to cry to his mom",
            "Im speechless from how bad you are <NAME>",
            "<NAME> should probably stop looking at catboys and start looking for jobs",
            "<NAME>'s parents would have rather been in a car accident than with their child",
            "You probably should stop sniffing glue <NAME>",
            "<NAME>'s iq is lower than the average room temperature in Antarctica",
            "The only thing <NAME> ever succeeded in was having an extra chromosome",
            "Even a blind man would throw up by looking at a picture of <NAME>",
            "You did not even manage to hit me.",
            "I would say that your aim is cancer but it at least kills people",
            "<NAME> couldn't handle the amazing combat features of Nostalgia!",
            "The last time <NAME> saw the sun was when he had to go the hospital for drinking too much milk",
            "FBI OPEN UP! We saw you searching for free minecraft cheats <NAME>!",
            "<NAME> has a horrible anime addiction",
            "<NAME> probably asks for free Rise keys",
            "<NAME> probably fucks pencil sharpeners"
    };
    private final String[] exhibobo = new String[]{
            "Wow, you just died in a block game %s",
            "%s died in a block game lmfao.",
            "%s died for using an android device. LOL",
            "%s, your mother is of the homophobic type",
            "That's a #VictoryRoyale!, better luck next time, %s!",
            "%s, used Flux then got backhanded by the face of hypixel",
            "even loolitsalex has more wins then you %s",
            "my grandma plays minecraft better than you %s",
            "%s, you should look into purchasing vape",
            "Omg %s I'm so sorry",
            "%s, What's worse your skin or the fact your a casual f3ckin normie",
            "you know what %s, blind gamers deserve a chance too. I support you.",
            "that was a pretty bad move %s",
            "how does it feel to get stomped on %s",
            "%s, do you really like dying this much?",
            "if i had a choice between %s and jake paul, id choose jake paul",
            "hey %s, what does your IQ and kills have in common? They are both low af",
            "Hey %s, want some PvP advice?",
            "wow, you just died in a game about legos",
            "i'm surprised that you were able hit the 'Install' button %s",
            "%s I speak English not your gibberish.",
            "%s Take the L, kid",
            "%s got memed",
            "%s is a default skin!!!1!1!1!1!!1!1",
            "%s You died in a fucking block game",
            "%s likes anime",
            "%s Trash dawg, you barely even hit me.",
            "%s I just fucked him so hard he left the game",
            "%s get bent over and fucked kid",
            "%s couldn't even beat 4 block",
            "Someone get this kid a tissue, %s is about to cry!",
            "%s's dad is bald",
            "%s Your family tree must be a cactus because everybody on it is a prick.",
            "%s You're so fucking trash that the binman mistook you for garbage and collected you in the morning",
            "%s some kids were dropped at birth but you were clearly thrown at a wall",
            "%s go back to your mother's womb you retarded piece of shit",
            "Thanks for the free kill %s !",
            "Benjamin's forehead is bigger than your future Minecraft PvP career %s",
            "%s are you even trying?",
            "%s You. Are. Terrible.",
            "%s my mom is better at this game then you",
            "%s lololololol mad? lololololol",
            "%s /friend me so we can talk about how useless you are",
            "%s: \"Staff! Staff! Help me! I am dogcrap at this game and i am getting rekt!\"",
            "%s Is it really that hard to trace me while i'm hopping around you?",
            "%s, Vape is a cool thing you should look into!",
            "%s I'm not using reach, you just need to click faster.",
            "%s I hope you recorded that, so that you can watch how trash you really are.",
            "%s You have to use the left and right mouse button in this game, in case you forgot.",
            "%s I think that the amount of ping you have equates to your braincells dumbfuck asshat",
            "%s ALT+F4 to remove the problem",
            "%s alt+f4 for hidden perk window",
            "%s You'll eventually switch back to Fortnite again, so why not do it now?",
            "%s go back to fortnite where you belong, you degenerate 5 year old",
            "%s I'll be sure to Orange Justice the fucck out of your corpse",
            "%s Exhibob better than you!1",
            "%s I'm a real gamer, and you just got owned!!",
            "%s Take a taste of your own medicine you clapped closet cheater",
            "%s go drown in your own salt",
            "%s go and suck off prestonplayz, you 7 yr old fanboy",
            "%s how are you so bad. I'm losing brain cells just watching you play",
            "%s Jump down from your school building with a rope around your neck.",
            "%s dominated, monkey :dab:",
            "%s Please add me as a friend so that you can shout at me. I live for it.",
            "%s i fvcked your dad",
            "%s Yeah, I dare you, rage quit. Come on, make us both happy.",
            "%s No, you are not blind! I DID own you!",
            "%s easy 10 hearted L",
            "%s It's almost as if i can hear you squeal from the other side!",
            "%s If you read this, you are confirmed homosexual",
            "%s have you taken a dump lately? Because I just beat the shit of out you.",
            "%s 6 block woman beater",
            "%s feminist demolisher",
            "%s chromosome count doubles the size of this game",
            "a million years of evolution and we get %s",
            "if the body is 70 percent water how is %s 100 percent salt???",
            "%s L",
            "%s got rekt",
            "%s you're so fat that when you had a fire in your house you dialled 999 on the microwave",
            "LMAO %s is a Fluxuser",
            "LMAO %s is a Sigmauser",
            "%s I suffer from these fukking kicks, grow brain lol",
            "LMAO %s a crack user",
            "%s Hypixel thought could stop us from cheating, huh, you are just as delusional as him",
            "%s GET FUCKED IM ON BADLION CLIENT WHORE",
            "%s should ask tene if i was hacking or not",
            "%s check out ARITHMOS CHANNEL",
            "%s gay",
            "%s, please stop",
            "%s, I play fortnite duos with your mom",
            "%s acts hard but %s's dad beats him harder",
            "Lol commit not alive %s",
            "How'd you hit the DOWNLOAD button with that aim? %s",
            "I'd say your aim is cancer, but at least cancer kills people. %s",
            "%s is about as useful as pedals on a wheelchair",
            "%s's aim is now sponsored by Parkinson's!",
            "%s, I'd say uninstall but you'd probably miss that too.",
            "%s, I bet you edate.",
            "%s, you probably watch tenebrous videos and are intruiged",
            "%s Please could you not commit not die kind sir thanks",
            "%s gay",
            "%s you probably suck on door knobs",
            "%s go commit stop breathing u dumb idot",
            "%s go commit to sucking on door knobs",
            "the only way you can improve at pvp %s is by taking a long walk off a short pier",
            "L %s",
            "%s Does not have a good client",
            "%s's client refused to work",
            "%s Stop hacking idiot",
            "%s :potato:",
            "%s go hunt kangaroos fucking aussie ping",
            "%s Super Mario Bros. deathsound",
            "Hey everyone, do /friend add %s , and tell them how trash they are",
            "%s Just do a France 1940, thank you",
            "Hey %s , would you like to hear a joke? Yeah, you ain't getting any",
            "%s got OOFed",
            "You mum your dad the ones you never had %s",
            "%s please be toxic to me, I enjoy it",
            "oof %s",
            "%s knock knock, FBI open up, we saw you searched for cracked vape.",
            "%s plez commit jump out of window for free rank",
            "%s you didn't even stand a chance!",
            "%s keep trying!",
            "%s, you're the type of player to get 3rd place in a 1v1",
            "%s, I'm not saying you're worthless, but I would unplug your life support to charge my phone",
            "I didn't know dying was a special ability %s",
            "%s, Stephen Hawking had better hand-eye coordination than you",
            "%s, kids like you were the inspiration for birth control",
            "%s you're the definition of bane",
            "%s lol GG!!!",
            "%s lol bad client what is it exhibition?",
            "%s L what are you lolitsalex?",
            "%s gg e z kid",
            "%s tene is my favorite youtuber and i bought his badlion client clock so i'm legit",
            "Don't forget to report me %s",
            "Your IQ is that of a Steve %s",
            "%s have you taken a dump lately? Because I just beat the shit of out you.",
            "%s dont ever put bean in my donut again.",
            "%s 2 plus 2 is 4, minus 1 that's your IQ",
            "I think you need vape %s !",
            "%s You just got oneTapped LUL",
            "%s You're the inspiration for birth control",
            "%s I don't understand why condoms weren't named by you.",
            "%s, My blind grandpa has better aim than you.",
            "%s, Exhibob better then you!",
            "%s, u r So E.Z",
            "Exhibition > %s",
            "%s, NMSL",
            "%s, your parents abondoned you, then the orphanage did the same",
            "%s,stop using trash client like sigma.",
            "%s, your client is worse than sigma, and that's an achievement",
            "%s, ur fatter than Napoleon",
            "%s please consider not alive",
            "%s, probably bought sigma premium",
            "%s, probably asks for sigma premium keys",
            "%s the type of person to murder someone and apologize saying it was a accident",
            "%s you're the type of person who would quickdrop irl",
            "%s, got an F on the iq test.",
            "Don't forget to report me %s",
            "%s even viv is better than you LMAO",
            "%s your mom gaye",
            "%s I Just Sneezed On Your Forehead",
            "%s your teeth are like stars - golden, and apart.",
            "%s Rose are blue, stars are red, you just got hacked on and now you're dead",
            "%s i don't hack because watchdog is watching so it would ban me anyway.",
            "%s, chill out on the paint bro",
            "%s You got died from the best client in the game, now with Infinite Sprint bypass",
            "%s you're so fat, that your bellybutton reaches your house 20 minutes before you do",
            "%s your dick is so small, that you bang cheerios"
    };
    private final String[] noclue = new String[]{
            "Yeah, these niggas say, I’ll catch a foul, what do they know?",
            "Just tryna score a point in the end zone",
            "Didn’t ask your opinion, nigga who the fuck are you?",
            "Hand on my choppa, I'll turn you to pasta linguini, Yeah okay",
            "Niggas be hostile, I know that they just wanna be me, in my lane",
            "Vibin' with the gang, smoking gas in the coupe",
            "Had to drop her and she had no clue",
            "Run up, you done up, your ass gon' get toast",
            "All you niggas on lame shit",
            "Your bitch come back to my place, I do the most",
            "Break their net, with the rolex, that two-tone",
            "Mobbin' with a thick bitch, might be a redbone"
    };
    private final String[] blackout = new String[]{
            "% blacked out",
            "% got smoked by Blackout Client",
            "% could not handle the Blackout Client AutoCrystal",
            "% maybe its time for you to get Blackout Client so you don't have to constantly die to everyone",
            "% i don't even know what to say tbh that was just a horrible attempt at CPVP",
            "% i think you should just uninstall",
            "% if you would be on Blackout Client you would still be alive!",
            "% a rat can play better than you",
            "% theres nothing wrong with disabled people playing games but at least don't embarrass yourself",
            "% could not handle the Blackout Client infinite sprint bypass",
            "% did you forget your CA bind or is it just that bad?",
            "% you should get Blackout Client the real opp stoppa",
            "% put the burger down and focus on the game to not constantly die, you also might stop gaining weight",
            "% never knew anyone could play that bad",
            "% the amount of hoes and the kills you have is the exact same a nice round 0",
            "% is so fat i can feel the gravitational pull from another country",
            "% the sad thing is the only running you do is in game",
            "% if you can't handle my Blackout then just press esc and then hit the disconnect button!",
            "% remember to take a break i know your blood pressure is rising",
            "% with the amount of salt you have your going to turn in to a salt shaker",
            "% being that mad can not be good for you remember you can quit at any time!",
            "good bye % back to the lobby!",
            "% i would have insulted your girlfriend but you don't even have one",
            "% stop watching Vtubers and focus on the game",
            "% with the amount of muscle you have it's surprising you can even move the mouse",
            "I put that boy % in a box"
    };
    private int lastNum;
    private int lastPop;
    private boolean lastState;
    private String name = null;
    private int timer = 0;

    public Insults() {
        super("Insults", "Insults people after killing them", SubCategory.MISC_COMBAT, true);
    }

    @Override
    public void onEnable() {
        this.lastState = false;
        this.lastNum = -1;
    }

    @Override
    public String getInfo() {
        return this.killMsgMode.get().name();
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        this.timer++;
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (!this.anyDead(this.range.get()) || !this.kill.get()) {
                this.lastState = false;
            } else if (!this.lastState) {
                this.lastState = true;
                this.sendKillMessage();
            }

            if (this.timer >= this.tickDelay.get() && !this.messageQueue.isEmpty() && BlackOut.mc.getNetworkHandler() != null) {
                Message msg = this.messageQueue.getFirst();
                ChatUtils.sendMessage(String.valueOf(msg.message));
                this.timer = 0;
                if (msg.kill) {
                    this.messageQueue.clear();
                } else {
                    this.messageQueue.removeFirst();
                }
            }
        }
    }

    @Event
    public void onReceive(PacketEvent.Receive.Pre event) {
        if (event.packet instanceof EntityStatusS2CPacket packet && packet.getStatus() == 35) {
            Entity entity = packet.getEntity(BlackOut.mc.world);
            if (this.pop.get()
                    && BlackOut.mc.player != null
                    && BlackOut.mc.world != null
                    && entity instanceof PlayerEntity
                    && entity != BlackOut.mc.player
                    && !Managers.FRIENDS.isFriend((PlayerEntity) entity)
                    && BlackOut.mc.player.getPos().distanceTo(entity.getPos()) <= this.range.get()) {
            }
        }
    }

    private boolean anyDead(double range) {
        for (PlayerEntity pl : BlackOut.mc.world.getPlayers()) {
            if (pl != BlackOut.mc.player
                    && !Managers.FRIENDS.isFriend(pl)
                    && pl.getPos().distanceTo(BlackOut.mc.player.getPos()) <= range
                    && pl.getHealth() <= 0.0F) {
                this.name = pl.getName().getString();
                return true;
            }
        }

        return false;
    }

    private void sendKillMessage() {
        switch (this.killMsgMode.get()) {
            case Exhibition:
                this.messageQueue.add(this.getMessage(this.exhibobo.length, "%", this.exhibobo));
                break;
            case Blackout:
                this.messageQueue.add(this.getMessage(this.blackout.length, "%", this.blackout));
                break;
            case NoClue:
                this.messageQueue.add(this.getMessage(this.noclue.length, "%", this.noclue));
                break;
            case Nostalgia:
                this.messageQueue.add(this.getMessage(this.insults.length, "<NAME>", this.insults));
        }
    }

    private Message getMessage(int length, String replace, String[] getFrom) {
        int num = this.r.nextInt(0, length - 1);
        if (num == this.lastNum) {
            num = num < length - 1 ? num + 1 : 0;
        }

        this.lastNum = num;
        return new Message(getFrom[num].replace(replace, this.name == null ? "You" : this.name), true);
    }

    public enum MessageMode {
        Blackout,
        Exhibition,
        NoClue,
        Nostalgia
    }

    private record Message(String message, boolean kill) {
    }
}
