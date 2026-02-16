package bodevelopment.client.blackout.rendering.framebuffer;

import bodevelopment.client.blackout.BlackOut;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL30C;

public class FrameBuffer {
    private static final int[] prevViewport = new int[4];
    private static int prevBuffer = BlackOut.mc.getFramebuffer().fbo;
    private int id;
    private int textureId;
    private int depthId;
    private int width;
    private int height;
    private boolean viewportChanged = false;

    public FrameBuffer() {
        this.load();
    }

    public static int getCurrent() {
        int[] ir = new int[1];
        GL30C.glGetIntegerv(36006, ir);
        return ir[0];
    }

    public static void bind(int id) {
        GL30C.glBindFramebuffer(36160, id);
    }

    public int getTexture() {
        return this.textureId;
    }

    public void resize() {
        this.delete();
        this.load();
    }

    private void load() {
        this.width = BlackOut.mc.getWindow().getFramebufferWidth();
        this.height = BlackOut.mc.getWindow().getFramebufferHeight();
        this.id = GL30C.glGenFramebuffers();
        this.textureId = GL30C.glGenTextures();
        this.depthId = GL30C.glGenTextures();
        GlStateManager._bindTexture(this.depthId);
        GlStateManager._texParameter(3553, 10241, 9728);
        GlStateManager._texParameter(3553, 10240, 9728);
        GlStateManager._texParameter(3553, 34892, 0);
        GlStateManager._texParameter(3553, 10242, 33071);
        GlStateManager._texParameter(3553, 10243, 33071);
        GlStateManager._texImage2D(3553, 0, 6402, this.width, this.height, 0, 6402, 5126, null);
        GlStateManager._bindTexture(this.textureId);
        GlStateManager._texParameter(3553, 10241, 9728);
        GlStateManager._texParameter(3553, 10240, 9728);
        GlStateManager._texParameter(3553, 10242, 33071);
        GlStateManager._texParameter(3553, 10243, 33071);
        GlStateManager._texImage2D(3553, 0, 32856, this.width, this.height, 0, 6408, 5121, null);
        this.bind(false);
        GlStateManager._glFramebufferTexture2D(36160, 36064, 3553, this.textureId, 0);
        GlStateManager._glFramebufferTexture2D(36160, 36096, 3553, this.depthId, 0);
        this.unbind();
    }

    public void delete() {
        GL30C.glDeleteFramebuffers(this.id);
        GL30C.glDeleteTextures(this.textureId);
        GL30C.glDeleteTextures(this.depthId);
    }

    public void safeBind(boolean viewPort) {
        this.safe(() -> this.bind(viewPort));
    }

    public void bind(boolean viewPort) {
        prevBuffer = getCurrent();
        bind(this.id);
        if (viewPort) {
            GL30C.glGetIntegerv(GL30C.GL_VIEWPORT, prevViewport);
            GL30C.glViewport(0, 0, this.width, this.height);
            this.viewportChanged = true;
        }
    }

    public void safeUnbind() {
        this.safe(this::unbind);
    }

    public void unbind() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(this::bindPrev);
        } else {
            this.bindPrev();
        }
    }

    private void bindPrev() {
        RenderSystem.assertOnRenderThreadOrInit();
        GlStateManager._glBindFramebuffer(36160, prevBuffer);
        if (this.viewportChanged) {
            GL30C.glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
            this.viewportChanged = false;
        }
    }

    private void safe(Runnable runnable) {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(runnable::run);
        } else {
            runnable.run();
        }
    }

    public void clear() {
        this.clear(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void clear(float r, float g, float b, float a) {
        this.bind(true);
        GL30C.glClearColor(r, g, b, a);
        GL30C.glClearDepth(1.0);
        GL30C.glClear(GL30C.GL_COLOR_BUFFER_BIT | GL30C.GL_DEPTH_BUFFER_BIT);
        this.unbind();
    }
}
