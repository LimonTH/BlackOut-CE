package bodevelopment.client.blackout.rendering.framebuffer;

import bodevelopment.client.blackout.BlackOut;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL30C;

/**
 * Framebuffer with a packed GL_DEPTH24_STENCIL8 renderbuffer.
 * Used for HUD background merging via stencil-based deduplication.
 */
public class StencilFrameBuffer {
    private int fboId;
    private int textureId;
    private int rboId;
    private int width;
    private int height;

    public StencilFrameBuffer() {
        load();
    }

    private void load() {
        width = BlackOut.mc.getWindow().getWidth();
        height = BlackOut.mc.getWindow().getHeight();

        fboId = GL30C.glGenFramebuffers();
        textureId = GL30C.glGenTextures();
        rboId = GL30C.glGenRenderbuffers();

        GlStateManager._bindTexture(textureId);
        GlStateManager._texParameter(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_MIN_FILTER, GL30C.GL_NEAREST);
        GlStateManager._texParameter(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_MAG_FILTER, GL30C.GL_NEAREST);
        GlStateManager._texParameter(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_WRAP_S, GL30C.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_WRAP_T, GL30C.GL_CLAMP_TO_EDGE);
        GlStateManager._texImage2D(GL30C.GL_TEXTURE_2D, 0, GL30C.GL_RGBA8, width, height, 0, GL30C.GL_RGBA, GL30C.GL_UNSIGNED_BYTE, null);
        GlStateManager._bindTexture(0);

        GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, rboId);
        GL30C.glRenderbufferStorage(GL30C.GL_RENDERBUFFER, GL30C.GL_DEPTH24_STENCIL8, width, height);
        GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, 0);

        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, fboId);
        GlStateManager._glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, GL30C.GL_TEXTURE_2D, textureId, 0);
        GL30C.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_STENCIL_ATTACHMENT, GL30C.GL_RENDERBUFFER, rboId);
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, 0);
    }

    /** Bind, clear color to transparent, depth to 1.0, and stencil to 0. Leaves FBO bound. */
    public void clearAndBind() {
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, fboId);
        GL30C.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        GL30C.glClearDepth(1.0);
        GL30C.glClearStencil(0);
        GL30C.glClear(GL30C.GL_COLOR_BUFFER_BIT | GL30C.GL_DEPTH_BUFFER_BIT | GL30C.GL_STENCIL_BUFFER_BIT);
    }

    /** Bind this FBO without clearing. */
    public void bind() {
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, fboId);
    }

    /** Restore the main Minecraft render target. */
    public void unbind() {
        BlackOut.mc.getMainRenderTarget().bindWrite(false);
    }

    public int getTexture() { return textureId; }

    public void delete() {
        GL30C.glDeleteFramebuffers(fboId);
        GL30C.glDeleteTextures(textureId);
        GL30C.glDeleteRenderbuffers(rboId);
    }

    public void resize() {
        delete();
        load();
    }

    public boolean needsResize() {
        return width != BlackOut.mc.getWindow().getWidth()
            || height != BlackOut.mc.getWindow().getHeight();
    }
}
