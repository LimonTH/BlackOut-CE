package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;

import java.util.UUID;

public class ServerSpoof extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Double> delay = this.sgGeneral.doubleSetting("Packet Delay", 2.0, 0.0, 10.0, 0.1, "The interval in seconds between each sequential spoofed status packet (Accepted, Downloaded, Loaded).");

    private final ResourcePackStatusC2SPacket.Status[] statuses = new ResourcePackStatusC2SPacket.Status[]{ResourcePackStatusC2SPacket.Status.ACCEPTED, ResourcePackStatusC2SPacket.Status.DOWNLOADED, ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED};
    private UUID id;
    private long time = -1L;
    private int progress = 0;

    public ServerSpoof() {
        super("Server Spoof", "Automatically spoofs resource pack download and installation packets to bypass mandatory server pack requirements without downloading them.", SubCategory.MISC, true);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null && BlackOut.mc.player.age >= 20 && this.time >= 0L) {
            if (System.currentTimeMillis() > this.time + this.delay.get() * 1000.0) {
                this.sendPacket(new ResourcePackStatusC2SPacket(this.id, this.statuses[this.progress]));
                if (this.progress > 1) {
                    this.time = -1L;
                } else {
                    this.time = System.currentTimeMillis();
                }

                this.progress++;
            }
        }
    }

    @Event
    public void onReceive(PacketEvent.Receive.Post event) {
        if (event.packet instanceof ResourcePackSendS2CPacket packet) {
            event.setCancelled(true);
            this.id = packet.id();
            this.time = System.currentTimeMillis();
            this.progress = 0;
        }
    }
}
