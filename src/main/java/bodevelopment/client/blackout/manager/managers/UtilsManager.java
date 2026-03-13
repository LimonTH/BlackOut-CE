package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.GameJoinEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.util.DamageUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

public class UtilsManager extends Manager {
    @Override
    public void init() {
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
    }

    @Event
    public void onRenderWorld(RenderEvent.World.Post event) {
        RenderUtils.onRender();
    }

    @Event
    public void onJoin(GameJoinEvent event) {
        DamageUtils.raycastContext = new ClipContext(
                new Vec3(0.0, 0.0, 0.0), new Vec3(0.0, 0.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, BlackOut.mc.player
        );
    }
}
