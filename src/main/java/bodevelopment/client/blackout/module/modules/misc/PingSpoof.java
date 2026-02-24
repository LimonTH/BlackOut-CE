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
    private final Setting<SpoofMode> mode = this.sgGeneral.enumSetting("Spoof Mode", SpoofMode.Fake, "Real mode actually increases your ping.");
    private final Setting<Integer> jitterInterval = this.sgGeneral.intSetting("Jitter Interval", 5, 0, 20, 1, ".", () -> this.mode.get() == SpoofMode.Real);
    private final Setting<Integer> extra = this.sgGeneral.intSetting("Extra", 50, 0, 1000, 10, ".");
    private final Setting<Integer> jitter = this.sgGeneral.intSetting("Jitter", 5, 0, 1000, 10, ".");
    private int ji = 0;
    private long nextJ = 0L;

    public PingSpoof() {
        super("Ping Spoof", "Increases your ping.", SubCategory.MISC, true);
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
