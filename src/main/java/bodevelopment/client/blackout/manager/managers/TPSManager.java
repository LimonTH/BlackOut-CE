package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TPSManager extends Manager {
    private final List<Double> list = Collections.synchronizedList(new ArrayList<>());
    public double tps = 20.0;
    private long prevWorldTime = 0L;
    private long prevTime = 0L;

    @Override
    public void init() {
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
    }

    @Event
    public void onReceive(PacketEvent.Receive.Pre event) {
        if (event.packet instanceof WorldTimeUpdateS2CPacket packet) {
            long tickDelta = packet.getTime() - this.prevWorldTime;
            double sus = tickDelta / ((System.currentTimeMillis() - this.prevTime) / 1000.0);
            synchronized (this.list) {
                this.list.addFirst(sus);
                OLEPOSSUtils.limitList(this.list, 10);
                this.calcTps();
            }

            this.prevWorldTime = packet.getTime();
            this.prevTime = System.currentTimeMillis();
        }
    }

    private void calcTps() {
        double average = this.calcAverage(this.list);
        this.tps = this.calcAverage(
                this.list.stream().sorted(Comparator.comparingDouble(d -> Math.abs(d - average))).toList().subList(0, Math.min(this.list.size(), 7))
        );
    }

    private double calcAverage(List<Double> values) {
        double total = 0.0;

        for (double d : values) {
            total += d;
        }

        return total / values.size();
    }
}
