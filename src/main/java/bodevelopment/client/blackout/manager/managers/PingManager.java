package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.module.modules.misc.PingSpoof;
import net.minecraft.network.packet.Packet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        return spoof.enabled && spoof.shouldDelay(packet);
    }

    public void addSend(Runnable runnable) {
        this.sending.add(new DelayedPacket(runnable, System.currentTimeMillis()));
    }

    private record DelayedPacket(Runnable runnable, long time) {
    }
}
