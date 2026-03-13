package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.ChatUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

public class AuthMe extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgProfiles = this.addGroup("Profiles");

    private final Setting<Integer> profileCount = this.sgGeneral.intSetting("Profile Count", 1, 0, 10, 1, "The number of distinct account profiles to manage.");
    private final Setting<String> defaultPassword = this.sgGeneral.stringSetting("Fallback Password", "topShotta", "The password used if the current nickname does not match any configured profile.");
    private final Setting<Double> delay = this.sgGeneral.doubleSetting("Authentication Delay", 2.5, 0.0, 5.0, 0.1, "The delay in seconds between detecting an authentication prompt and sending the response.");
    private final Setting<Boolean> passwordConfirm = this.sgGeneral.booleanSetting("Double Entry", true, "Repeats the password during registration to satisfy confirmation requirements.");

    private final List<Setting<String>> nicks = new ArrayList<>();
    private final List<Setting<String>> passes = new ArrayList<>();

    private static final String[] REGISTER_KEYWORDS = {
            "/register", "/reg", "register", "зарегистрируйтесь", "/рег", "создайте пароль"
    };

    private static final String[] LOGIN_KEYWORDS = {
            "/login", "/l ", "login", "авторизуйтесь", "войдите", "/логин", "пароль"
    };

    private static final String[] AUTH_PROMPT_INDICATORS = {
            "please", "type", "use", "welcome", "введите", "используйте"
    };

    private long time = -1L;
    private boolean register = false;

    public AuthMe() {
        super("Auth Me", "Automatically executes login and registration commands upon detecting server authentication prompts.", SubCategory.MISC, true);

        for (int i = 1; i <= 10; i++) {
            final int index = i;
            SingleOut<Boolean> visibility = () -> profileCount.get() >= index;

            nicks.add(this.sgProfiles.stringSetting("Nick " + i, "Player" + i, "The username associated with profile " + i + ".", visibility));
            passes.add(this.sgProfiles.stringSetting("Password " + i, "pass" + i, "The password associated with profilee " + i + ".", visibility));
        }
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.level != null) {
            if (this.time >= 0L && (System.currentTimeMillis() - this.time >= this.delay.get() * 1000.0)) {
                String message = this.getAuthMessage();

                if (message != null) {
                    ChatUtils.sendMessage(message);

                    Managers.NOTIFICATIONS.addNotification(
                            (this.register ? "Registering" : "Logging in") + " as " + BlackOut.mc.player.getGameProfile().getName(),
                            this.getDisplayName(),
                            2.0, Notifications.Type.Info
                    );
                }
                this.time = -1L;
            }
        }
    }

    @Event
    public void onPacketReceive(PacketEvent.Receive.Pre event) {
        if (event.packet instanceof ClientboundSystemChatPacket packet) {
            String msg = packet.content().getString().replaceAll("§[0-9a-fk-or]", "").toLowerCase().trim();

            if (System.currentTimeMillis() - this.time < (this.delay.get() * 1000.0) + 1000.0) return;

            boolean foundLogin = false;
            boolean foundRegister = false;

            for (String key : REGISTER_KEYWORDS) {
                if (msg.contains(key)) {
                    foundRegister = true;
                    break;
                }
            }

            if (!foundRegister) {
                for (String key : LOGIN_KEYWORDS) {
                    if (msg.contains(key)) {
                        foundLogin = true;
                        break;
                    }
                }
            }

            boolean hasContext = false;
            for (String context : AUTH_PROMPT_INDICATORS) {
                if (msg.contains(context)) {
                    hasContext = true;
                    break;
                }
            }

            boolean isCommand = msg.contains("/") || msg.contains("!");

            if (foundRegister && (hasContext || isCommand)) {
                this.time = System.currentTimeMillis();
                this.register = true;
            } else if (foundLogin && (hasContext || isCommand)) {
                this.time = System.currentTimeMillis();
                this.register = false;
            }
        }
    }

    private String getPasswordForCurrentAccount() {
        if (BlackOut.mc.player == null) return this.defaultPassword.get();
        String currentNick = BlackOut.mc.player.getGameProfile().getName();

        for (int i = 0; i < profileCount.get(); i++) {
            String nick = nicks.get(i).get();
            if (nick.equalsIgnoreCase(currentNick)) {
                return passes.get(i).get();
            }
        }
        return this.defaultPassword.get();
    }

    private String getAuthMessage() {
        String pass = getPasswordForCurrentAccount();
        if (pass == null || pass.isEmpty()) return null;

        return this.register
                ? "/register " + pass + (this.passwordConfirm.get() ? " " + pass : "")
                : "/login " + pass;
    }
}