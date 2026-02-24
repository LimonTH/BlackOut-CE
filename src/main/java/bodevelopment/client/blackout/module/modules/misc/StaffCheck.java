package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;

import java.util.ArrayList;
import java.util.List;

public class StaffCheck extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Boolean> kb = this.sgGeneral.booleanSetting("Knockback Alert", true, "Notifies you if you receive purely vertical velocity, which is a common method used by staff to check for anti-knockback cheats.");
    private final Setting<Boolean> nameCheck = this.sgGeneral.booleanSetting("Staff Name Alert", false, "Cross-references joining players with a known list of staff members and administrators.");

    List<String> staff = new ArrayList<>();
    private long prevTime = System.currentTimeMillis();
    private boolean added = false;

    public StaffCheck() {
        super("Staff Check", "Monitors server activity for signs of administrative presence or manual 'vibe checks' like knockback testing.", SubCategory.MISC, true);
    }

    @Override
    public void onEnable() {
        if (!this.added) {
            this.added = true;
            this.staff.add("Limon_TH"); // LimonTH
            this.staff.add("Raksamies"); // KassuK
            this.staff.add("OLEPOSSU"); // luhpossu
        }
    }

    @Event
    public void onVelocity(PacketEvent.Receive.Pre event) {
        if (event.packet instanceof EntityVelocityUpdateS2CPacket packet) {
            if (BlackOut.mc.player != null && BlackOut.mc.player.getId() != packet.getEntityId()) {
                return;
            }

            this.checkVelocity((float) packet.getVelocityX(), (float) packet.getVelocityZ(), (float) packet.getVelocityY());
        }

        if (event.packet instanceof ExplosionS2CPacket packet) {
            this.checkVelocity(packet.getPlayerVelocityX(), packet.getPlayerVelocityZ(), packet.getPlayerVelocityY());
        }
    }

    private void checkVelocity(float x, float z, float y) {
        if (BlackOut.mc.player != null) {
            if (x == 0.0F && z == 0.0F && y > 0.0F && this.kb.get()) {
                Managers.NOTIFICATIONS
                        .addNotification("Suspicious Knockback taken at tick " + BlackOut.mc.player.age, this.getDisplayName(), 2.0, Notifications.Type.Alert);
            }
        }
    }

    @Event
    public void onSend(PacketEvent.Receive.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (this.nameCheck.get()) {
                if (event.packet instanceof PlayerListS2CPacket packet) {
                    List<PlayerListS2CPacket.Entry> entries = packet.getPlayerAdditionEntries();
                    entries.forEach(
                            entry -> {
                                if (entry.displayName() != null
                                        && this.staff.contains(entry.displayName().withoutStyle().toString())
                                        && System.currentTimeMillis() - this.prevTime > 5000L) {
                                    Managers.NOTIFICATIONS.addNotification("Detected Staff", this.getDisplayName(), 2.0, Notifications.Type.Alert);
                                    this.prevTime = System.currentTimeMillis();
                                }
                            }
                    );
                }
            }
        }
    }
}
