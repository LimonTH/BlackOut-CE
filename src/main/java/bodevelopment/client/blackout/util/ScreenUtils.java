package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;

/**
 * Single source of truth for screen dimensions, mouse coordinates, and GUI scale.
 * All units are raw physical pixels (1 = 1px). Use {@link #beginPixelSpace} /
 * {@link #endPixelSpace} to enter/exit pixel-space rendering.
 */
public final class ScreenUtils {

    private ScreenUtils() {
    }

    // Dimensions (raw pixels)

    public static int screenWidth() {
        return BlackOut.mc.getWindow().getScreenWidth();
    }

    public static int screenHeight() {
        return BlackOut.mc.getWindow().getScreenHeight();
    }

    /**
     * GUI-scaled (≈ screen / guiScale).
     */
    public static int guiWidth() {
        return BlackOut.mc.getWindow().getGuiScaledWidth();
    }

    public static int guiHeight() {
        return BlackOut.mc.getWindow().getGuiScaledHeight();
    }

    /**
     * Framebuffer (screen × guiScale).
     */
    public static int framebufferWidth() {
        return BlackOut.mc.getMainRenderTarget().viewWidth;
    }

    public static int framebufferHeight() {
        return BlackOut.mc.getMainRenderTarget().viewHeight;
    }

    public static double guiScale() {
        return BlackOut.mc.getWindow().getGuiScale();
    }

    // Mouse (raw pixel space)

    /**
     * Raw-pixel mouse X (for rendering after {@link #beginPixelSpace}).
     */
    public static double mouseX() {
        Minecraft mc = BlackOut.mc;
        return mc.mouseHandler.xpos()
                * (double) mc.getWindow().getGuiScaledWidth()
                / (double) mc.getWindow().getScreenWidth()
                * guiScale();
    }

    /**
     * Raw-pixel mouse Y.
     */
    public static double mouseY() {
        Minecraft mc = BlackOut.mc;
        return mc.mouseHandler.ypos()
                * (double) mc.getWindow().getGuiScaledHeight()
                / (double) mc.getWindow().getScreenHeight()
                * guiScale();
    }

    // Mouse (GUI-scaled, matches Screen.render parameters)

    /**
     * GUI-scaled mouse X (int-truncated, matches what Screen.render receives).
     */
    public static int mouseGuiX() {
        Minecraft mc = BlackOut.mc;
        return (int) (mc.mouseHandler.xpos()
                * (double) mc.getWindow().getGuiScaledWidth()
                / (double) mc.getWindow().getScreenWidth());
    }

    /**
     * GUI-scaled mouse Y.
     */
    public static int mouseGuiY() {
        Minecraft mc = BlackOut.mc;
        return (int) (mc.mouseHandler.ypos()
                * (double) mc.getWindow().getGuiScaledHeight()
                / (double) mc.getWindow().getScreenHeight());
    }

    // Rendering

    /**
     * Enter pixel-space: push + unGuiScale + disableDepth + depthMask(false).
     */
    public static void beginPixelSpace(PoseStack stack) {
        stack.pushPose();
        float s = (float) guiScale();
        stack.scale(1.0F / s, 1.0F / s, 1.0F);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
    }

    /**
     * Exit pixel-space: depthMask(true) + enableDepth + pop.
     */
    public static void endPixelSpace(PoseStack stack) {
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        stack.popPose();
    }

    // Helpers

    public static double aspectRatio() {
        return (double) screenWidth() / screenHeight();
    }

    public static int minDim() {
        return Math.min(screenWidth(), screenHeight());
    }

    public static int maxDim() {
        return Math.max(screenWidth(), screenHeight());
    }
}
