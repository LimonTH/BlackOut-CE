package bodevelopment.client.blackout.util.render;

import net.minecraft.resources.ResourceLocation;

public class CapeRenderContext {
    private static final ThreadLocal<ResourceLocation> CURRENT_CAPE = new ThreadLocal<>();
    private static final ThreadLocal<float[]> CURRENT_DIMENSIONS = new ThreadLocal<>();

    public static void set(ResourceLocation cape) {
        CURRENT_CAPE.set(cape);
    }

    public static void set(ResourceLocation cape, float texWidth, float texHeight) {
        CURRENT_CAPE.set(cape);
        CURRENT_DIMENSIONS.set(new float[] { texWidth, texHeight });
    }

    public static void clear() {
        CURRENT_CAPE.remove();
        CURRENT_DIMENSIONS.remove();
    }

    public static ResourceLocation get() {
        return CURRENT_CAPE.get();
    }

    /** Returns {texWidth, texHeight} or null if not available. */
    public static float[] getDimensions() {
        return CURRENT_DIMENSIONS.get();
    }
}
