package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.ChatUtils;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

public class AuthMe extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<String> password = this.sgGeneral.s("Password", "topShotta", "The password used");
    private final Setting<Double> delay = this.sgGeneral.d("Delay", 2.5, 0.0, 5.0, 0.1, "Delay between receiving message and sending one.");
    private final Setting<Boolean> passwordConfirm = this.sgGeneral.b("Password Confirm", true, ".");
    private long time = -1L;
    private boolean register = false;

    public AuthMe() {
        super("Auth Me", "Automatically logs in", SubCategory.MISC, true);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (this.time >= 0L && !(System.currentTimeMillis() - this.time < this.delay.get() * 1000.0)) {
                ChatUtils.sendMessage(this.getMessage());
                Managers.NOTIFICATIONS
                        .addNotification("Attempted to " + (this.register ? "register" : "login"), this.getDisplayName(), 2.0, Notifications.Type.Info);
                this.time = -1L;
            }
        }
    }

    @Event
    public void onSend(PacketEvent.Receive.Pre event) {
        if (event.packet instanceof GameMessageS2CPacket packet) {
            String msg = packet.content().getString();
            if (System.currentTimeMillis() - this.time > this.delay.get() * 1000.0 + 500.0) {
                if (msg.contains("/register")) {
                    this.time = System.currentTimeMillis();
                    this.register = true;
                } else if (msg.contains("/login")) {
                    this.time = System.currentTimeMillis();
                    this.register = false;
                }
            }
        }
    }

    private String getMessage() {
        return this.register
                ? "/register " + this.password.get() + (this.passwordConfirm.get() ? " " + this.password.get() : "")
                : "/login " + this.password.get();
    }
}
