package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.util.math.MathHelper;

import java.util.concurrent.ThreadLocalRandom;

public class PingSpoof extends Module {
    private static PingSpoof INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<SpoofMode> mode = this.sgGeneral.enumSetting("Latency Mode", SpoofMode.Fake, "Fake: Only modifies the ping value displayed in the tab list. Real: Delays packets to physically increase latency.");
    private final Setting<Integer> extra = this.sgGeneral.intSetting("Additional Ping", 50, 0, 1000, 10, "The base amount of extra latency added to your connection in milliseconds.");
    private final Setting<Integer> jitter = this.sgGeneral.intSetting("Jitter Magnitude", 5, 0, 1000, 10, "The maximum random variance added to the extra ping to simulate a natural connection.");
    private final Setting<Integer> jitterInterval = this.sgGeneral.intSetting("Refresh Rate", 5, 0, 20, 1, "The interval in ticks at which the jitter value is recalculated for Real mode.", () -> this.mode.get() == SpoofMode.Real);

    private int ji = 0;
    private long nextJ = 0L;

    public PingSpoof() {
        super("Ping Spoof", "Artificially inflates your latency to the server for visual effect or specific bypasses.", SubCategory.MISC, true);
        INSTANCE = this;
    }

    public static PingSpoof getInstance() {
        return INSTANCE;
    }

    @Override
    public String getInfo() {
        return this.mode.get().name() + " " + this.extra.get() + " " + this.ji;
    }

    public void refresh() {
        if (System.currentTimeMillis() > this.nextJ) {
            this.ji = (int) Math.round(ThreadLocalRandom.current().nextDouble() * this.jitter.get());
            this.nextJ = System.currentTimeMillis()
                    + Math.round(MathHelper.lerp(ThreadLocalRandom.current().nextDouble(), this.jitterInterval.get() / 2.0F, this.jitterInterval.get() * 1.5) * 50.0);
        }
    }

    public boolean shouldDelay(Packet<?> packet) {
        return this.mode.get() == SpoofMode.Real || packet instanceof CommonPongC2SPacket || packet instanceof KeepAliveC2SPacket;
    }

    public int getPing() {
        return this.mode.get() == SpoofMode.Fake ? this.extra.get() : this.extra.get() + this.ji;
    }

    public enum SpoofMode {
        Fake,
        Real
    }
}
