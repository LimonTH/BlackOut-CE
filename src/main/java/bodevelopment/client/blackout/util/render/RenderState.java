package bodevelopment.client.blackout.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.CoreShaders;

/**
 * AutoCloseable GL state management. Automatically restores state on close.
 *
 * Usage:
 *   try (RenderState state = RenderState.blend2D()) {
 *       // draw calls
 *   }
 */
public class RenderState implements AutoCloseable {
    private final Runnable restore;

    private RenderState(Runnable restore) {
        this.restore = restore;
    }

    @Override
    public void close() {
        this.restore.run();
    }

    /**
     * 2D blended color rendering (HUD quads, lines, circles).
     * Enables: blend, defaultBlendFunc, POSITION_COLOR shader.
     */
    public static RenderState blend2D() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);
        return new RenderState(() -> RenderSystem.disableBlend());
    }

    /**
     * 2D blended textured rendering.
     * Enables: blend, defaultBlendFunc, POSITION_TEX shader.
     */
    public static RenderState texture2D() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(CoreShaders.POSITION_TEX);
        return new RenderState(() -> {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        });
    }

    /**
     * 3D blended rendering without depth test (ESP boxes, filled shapes).
     * Enables: blend, defaultBlendFunc, disables: cull, depthTest, depthMask.
     */
    public static RenderState blend3D() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        return new RenderState(() -> {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        });
    }

    /**
     * 3D line rendering (outlines, wireframes).
     * Same as blend3D + RENDERTYPE_LINES shader + lineWidth.
     */
    public static RenderState lines3D(float width) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(CoreShaders.RENDERTYPE_LINES);
        RenderSystem.lineWidth(width);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        return new RenderState(() -> {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        });
    }

    /**
     * 2D blended color rendering with depth and cull disabled (angled circles, overlays).
     */
    public static RenderState blend2DFull() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        return new RenderState(() -> {
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        });
    }
}
