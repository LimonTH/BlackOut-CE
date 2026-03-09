package bodevelopment.client.blackout.util.render;

import net.minecraft.entity.Entity;

public class RenderEntityCapture {
    public static final ThreadLocal<Entity> CAPTURED_ENTITY = new ThreadLocal<>();
}