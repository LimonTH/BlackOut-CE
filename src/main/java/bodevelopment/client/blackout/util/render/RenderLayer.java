package bodevelopment.client.blackout.util.render;

public class RenderLayer {
    /** WORLD */
    public static final float WORLD = 0.0F;

    /** Particles */
    public static final float PARTICLES = 100.0F;

    /** ESP */
    public static final float ESP = 200.0F;

    /** Nametags */
    public static final float NAMETAGS = 300.0F;

    /** HUD */
    public static final float HUD = 500.0F;

    /** GUI */
    public static final float GUI = 700.0F;

    /** GUI Elements (buttons, sliders) */
    public static final float GUI_ELEMENT = 800.0F;

    /** GUI Popups (menus, dialogs) */
    public static final float GUI_POPUP = 900.0F;

    /** Offset for sorting  */
    public static final float OFFSET_LARGE = 10.0F;
    /** Offset for sorting */
    public static final float OFFSET_SMALL = 1.0F;
    /** Offset for sorting */
    public static final float OFFSET_MILI = 0.1F;
    /** Offset for sorting */
    public static final float OFFSET_MICRO = 0.01F;
    /** Offset for sorting */
    public static final float OFFSET_NANO = 0.001F;

    public static boolean isStandardLayer(float z) {
        return z == WORLD || z == ESP || z == PARTICLES || z == HUD || z == GUI ||
                z == GUI_ELEMENT || z == GUI_POPUP || z == OFFSET_SMALL ||
                z == OFFSET_LARGE || z == OFFSET_MICRO || z == OFFSET_NANO || z == OFFSET_MILI || z == NAMETAGS;
    }
}
