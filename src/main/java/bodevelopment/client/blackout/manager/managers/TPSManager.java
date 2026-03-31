package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.util.CollectionUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;

public class TPSManager extends Manager {
    private static final int MAX_SAMPLES = 10;
    private static final long REGION_TICK_THRESHOLD_MS = 40;
    private static final long REGION_STALE_MS = 5000;

    private final List<Double> list = Collections.synchronizedList(new ArrayList<>());
    private final List<Double> regionList = Collections.synchronizedList(new ArrayList<>());

    private long prevWorldTime = 0L;
    private long prevTime = 0L;
    private long lastBundleTime = 0L;
    private long lastTickBoundary = 0L;

    public double regionTps = 20.0;
    public double tps = 20.0;

    @Override
    public void init() {
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
    }

    @Event
    public void onReceive(PacketEvent.Receive.Pre event) {
        if (event.packet instanceof ClientboundSetTimePacket packet) {
            long tickDelta = packet.gameTime() - this.prevWorldTime;
            double sus = tickDelta / ((System.currentTimeMillis() - this.prevTime) / 1000.0);
            synchronized (this.list) {
                this.list.addFirst(sus);
                CollectionUtils.limitSize(this.list, MAX_SAMPLES);
                this.tps = calcTrimmedMean(this.list);
            }

            this.prevWorldTime = packet.gameTime();
            this.prevTime = System.currentTimeMillis();
        }

        if (event.packet instanceof ClientboundBundlePacket) {
            long now = System.currentTimeMillis();

            if (now - this.lastBundleTime > REGION_STALE_MS) {
                this.lastTickBoundary = now;
                this.lastBundleTime = now;
                synchronized (this.regionList) {
                    this.regionList.clear();
                }
                return;
            }

            if (now - this.lastBundleTime > REGION_TICK_THRESHOLD_MS) {
                if (this.lastTickBoundary != 0) {
                    long tickInterval = now - this.lastTickBoundary;
                    double tickTps = 1000.0 / tickInterval;

                    synchronized (this.regionList) {
                        this.regionList.addFirst(Math.min(20.0, tickTps));
                        CollectionUtils.limitSize(this.regionList, MAX_SAMPLES);
                        this.regionTps = calcTrimmedMean(this.regionList);
                    }
                }
                this.lastTickBoundary = now;
            }

            this.lastBundleTime = now;
        }
    }

    private static double calcTrimmedMean(List<Double> samples) {
        List<Double> sorted = samples.stream().sorted().toList();
        int size = sorted.size();

        if (size <= 2) {
            return Math.min(20.0, calcAverage(sorted));
        }

        int trim = Math.max(1, size / 5);
        return Math.min(20.0, calcAverage(sorted.subList(trim, size - trim)));
    }

    private static double calcAverage(List<Double> values) {
        double total = 0.0;
        for (double d : values) {
            total += d;
        }
        return total / values.size();
    }
}
