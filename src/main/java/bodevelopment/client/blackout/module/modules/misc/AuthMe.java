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
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import java.util.ArrayList;
import java.util.List;

public class AuthMe extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgProfiles = this.addGroup("Profiles");

    private final Setting<Integer> profileCount = this.sgGeneral.i("Profiles", 1, 0, 10, 1, "How many account profiles to use");
    private final Setting<String> defaultPassword = this.sgGeneral.s("Default Password", "topShotta", "Used if no profile matches your nickname");
    private final Setting<Double> delay = this.sgGeneral.d("Delay", 2.5, 0.0, 5.0, 0.1, "Delay between receiving message and sending one.");
    private final Setting<Boolean> passwordConfirm = this.sgGeneral.b("Password Confirm", true, "Type password twice for /register");

    private final List<Setting<String>> nicks = new ArrayList<>();
    private final List<Setting<String>> passes = new ArrayList<>();

    private long time = -1L;
    private boolean register = false;

    public AuthMe() {
        super("Auth Me", "Automatically logs in based on nickname", SubCategory.MISC, true);

        for (int i = 1; i <= 10; i++) {
            final int index = i;
            SingleOut<Boolean> visibility = () -> profileCount.get() >= index;

            nicks.add(this.sgProfiles.s("Nick " + i, "Player" + i, "Nickname for profile " + i, visibility));
            passes.add(this.sgProfiles.s("Password " + i, "pass" + i, "Password for profile " + i, visibility));
        }
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
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
        if (event.packet instanceof GameMessageS2CPacket packet) {
            String msg = packet.content().getString().replaceAll("§[0-9a-fk-or]", "").toLowerCase();

            if (System.currentTimeMillis() - this.time > (this.delay.get() * 1000.0) + 1000.0) {
                if (msg.contains("/register") || msg.contains("please register") || msg.contains("use /reg")) {
                    this.time = System.currentTimeMillis();
                    this.register = true;
                }
                else if (msg.contains("/login") || msg.contains("please log in") || msg.contains("authenticate") || msg.contains("type /l ")) {
                    this.time = System.currentTimeMillis();
                    this.register = false;
                }
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