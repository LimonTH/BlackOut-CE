package bodevelopment.client.blackout.hud;

import bodevelopment.client.blackout.module.setting.multisettings.BackgroundMultiSetting;
import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import bodevelopment.client.blackout.rendering.framebuffer.StencilFrameBuffer;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.rendering.shader.Shaders;
import bodevelopment.client.blackout.util.render.Render2DUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL14;

/**
 * Two-pass HUD background merging.
 *
 * <p>Pass 1 (COLLECT): All HUD elements render normally, but calls to
 * {@link BackgroundMultiSetting#render}
 * are redirected to the {@code bgFbo} framebuffer.  A global color-mask suppresses
 * all other rendering so content does not appear on screen prematurely.
 *
 * <p>Composite: the collected background FBO is blended onto the main screen.
 *
 * <p>Pass 2 (SKIP): Elements render again; background calls are silently skipped
 * so only content (text, items, blur effects) draws to the main screen.
 *
 * <p>Stencil: during COLLECT, the body pass of every rounded background uses
 * NOTEQUAL(1)/REPLACE so each screen pixel receives at most one background body,
 * eliminating double-alpha and the inner-border artefact between overlapping elements.
 */
public class HudMergePass {
    public enum Phase { IDLE, COLLECT, SKIP }

    private static Phase phase = Phase.IDLE;
    private static StencilFrameBuffer bgFbo;

    private static StencilFrameBuffer bgFbo() {
        if (bgFbo == null) bgFbo = new StencilFrameBuffer();
        else if (bgFbo.needsResize()) bgFbo.resize();
        return bgFbo;
    }

    /**
     * Start the COLLECT pass.
     * Clears and binds the background FBO, suppresses color writes to the main target.
     * Must be followed eventually by {@link #endCollectAndComposite()}.
     */
    public static void beginCollect() {
        StencilFrameBuffer fbo = bgFbo();
        fbo.clearAndBind();
        fbo.unbind();
        phase = Phase.COLLECT;
        GL11C.glColorMask(false, false, false, false);
    }

    /**
     * Render a background to the background FBO.
     * Temporarily re-enables color writes, binds bgFbo, sets up stencil and blend,
     * runs {@code renderLogic}, then restores state.
     * Must be called only while {@link Phase#COLLECT} is active.
     */
    public static void renderBackgroundToBgFbo(Runnable renderLogic) {
        GL11C.glColorMask(true, true, true, true);

        StencilFrameBuffer fbo = bgFbo();
        fbo.bind();

        GL11C.glEnable(GL11C.GL_STENCIL_TEST);
        GL11C.glStencilMask(0xFF);

        GL11C.glStencilFunc(GL11C.GL_ALWAYS, 0, 0xFF);
        GL11C.glStencilOp(GL11C.GL_KEEP, GL11C.GL_KEEP, GL11C.GL_KEEP);

        RenderSystem.enableBlend();
        GL14.glBlendFuncSeparate(
            GL14.GL_SRC_ALPHA, GL14.GL_ONE_MINUS_SRC_ALPHA,
            GL14.GL_ONE,       GL14.GL_ONE_MINUS_SRC_ALPHA
        );

        renderLogic.run();

        GL11C.glDisable(GL11C.GL_STENCIL_TEST);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();

        fbo.unbind();
        GL11C.glColorMask(false, false, false, false);
    }

    /**
     * Called from {@link Render2DUtils#prepareAndRender} immediately before the body quad.
     * Switches stencil to NOTEQUAL(1)/REPLACE so each pixel receives at most one body.
     */
    public static void beginBodyRender() {
        if (phase != Phase.COLLECT) return;
        GL11C.glStencilFunc(GL11C.GL_NOTEQUAL, 1, 0xFF);
        GL11C.glStencilOp(GL11C.GL_KEEP, GL11C.GL_KEEP, GL11C.GL_REPLACE);
    }

    /**
     * Called from {@link Render2DUtils#prepareAndRender} immediately after the body quad.
     * Resets stencil to ALWAYS/no-write for the next render call.
     */
    public static void endBodyRender() {
        if (phase != Phase.COLLECT) return;
        GL11C.glStencilFunc(GL11C.GL_ALWAYS, 0, 0xFF);
        GL11C.glStencilOp(GL11C.GL_KEEP, GL11C.GL_KEEP, GL11C.GL_KEEP);
    }

    /**
     * End the COLLECT pass: restore color writes, composite the background FBO onto
     * the main screen, then enter SKIP mode for the content pass.
     */
    public static void endCollectAndComposite() {
        GL11C.glColorMask(true, true, true, true);
        phase = Phase.IDLE;

        float savedAlpha = Renderer.getAlpha();
        Render2DUtils.renderBufferWithTexture(
            bgFbo().getTexture(),
            Shaders.screentex,
            new ShaderSetup(s -> s.set("alpha", 1.0F))
        );
        Renderer.setAlpha(savedAlpha);

        phase = Phase.SKIP;
    }

    /** End the SKIP (content) pass; return to IDLE. */
    public static void endSkip() {
        phase = Phase.IDLE;
        RenderSystem.defaultBlendFunc();
    }

    public static boolean isCollecting() { return phase == Phase.COLLECT; }
    public static boolean isSkipping()   { return phase == Phase.SKIP;    }

    /** Explicit resize hook -- called on window resize events. */
    public static void onResize() {
        if (bgFbo != null) bgFbo.resize();
    }
}
