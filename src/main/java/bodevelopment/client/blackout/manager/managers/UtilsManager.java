package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.GameJoinEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.util.DamageUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

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
        DamageUtils.raycastContext = new RaycastContext(
                new Vec3d(0.0, 0.0, 0.0), new Vec3d(0.0, 0.0, 0.0), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, BlackOut.mc.player
        );
    }
}
