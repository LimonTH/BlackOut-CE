package bodevelopment.client.blackout.module;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.util.MovementPrediction;
import bodevelopment.client.blackout.util.SettingUtils;
import net.minecraft.util.math.Vec3d;

public class MoveUpdateModule extends Module {
    private Mode type = Mode.Normal;

    public MoveUpdateModule(String name, String description, SubCategory category) {
        super(name, description, category, true);
    }

    @Event
    public void onTickPre(TickEvent.Pre event) {
        this.type = Mode.values()[(SettingUtils.grimMovement() ? 2 : 0) + (SettingUtils.grimPackets() ? 1 : 0)];
        this.preTick();
    }

    @Event
    public void onMovePost(MoveEvent.Post event) {
        this.postMove();
    }

    @Event
    public void onTickPost(TickEvent.Post event) {
        this.postTick();
    }

    protected void preTick() {
        switch (this.type) {
            case Full:
                this.update(true, false);
                this.spoofedCalc();
                break;
            case Movement:
                this.spoofedCalc();
                break;
            case Packet:
                this.update(true, false);
        }
    }

    protected void postMove() {
        switch (this.type) {
            case Packet:
            case Normal:
                this.update(false, true);
        }
    }

    protected void postTick() {
        switch (this.type) {
            case Movement:
            case Normal:
                this.update(true, false);
        }
    }

    protected void update(boolean allowAction, boolean fakePos) {
    }

    private void spoofedCalc() {
        if (BlackOut.mc.player != null) {
            Vec3d pos = BlackOut.mc.player.getPos();
            BlackOut.mc.player.setPosition(MovementPrediction.predict(BlackOut.mc.player));
            this.update(false, true);
            BlackOut.mc.player.setPosition(pos);
        }
    }

    private enum Mode {
        Normal,
        Packet,
        Movement,
        Full
    }
}
