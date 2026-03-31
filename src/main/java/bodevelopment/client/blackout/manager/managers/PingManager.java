package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.module.modules.misc.PingSpoof;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.network.protocol.Packet;

public class PingManager extends Manager {
    private final List<DelayedPacket> sending = Collections.synchronizedList(new ArrayList<>());

    public void update() {
        PingSpoof spoof = PingSpoof.getInstance();
        spoof.refresh();
        long ping = spoof.getPing();
        synchronized (this.sending) {
            this.sending.removeIf(d -> {
                if (System.currentTimeMillis() < d.time() + ping) {
                    return false;
                } else {
                    d.runnable().run();
                    return true;
                }
            });
        }
    }

    public boolean shouldDelay(Packet<?> packet) {
        PingSpoof spoof = PingSpoof.getInstance();
        return spoof.enabled && BlackOut.mc.player != null && spoof.shouldDelay(packet);
    }

    public void addSend(Runnable runnable) {
        this.sending.add(new DelayedPacket(runnable, System.currentTimeMillis()));
    }

    public void clear() {
        synchronized (this.sending) {
            this.sending.clear();
        }
    }

    private record DelayedPacket(Runnable runnable, long time) {
    }
}
