package bodevelopment.client.blackout.rendering.framebuffer;

import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import bodevelopment.client.blackout.rendering.shader.Shaders;
import bodevelopment.client.blackout.util.render.RenderUtils;
import org.lwjgl.opengl.GL14;

public class GuiAlphaFrameBuffer extends FrameBuffer {
    public void start() {
        this.clear(0.0F, 0.0F, 0.0F, 0.0F);
        this.bind(true);
        GL14.glBlendFuncSeparate(770, 771, 1, 1);
    }

    public void end(float alpha) {
        this.unbind();
        RenderUtils.renderBufferWith(this, Shaders.screentex, new ShaderSetup(setup -> setup.set("alpha", alpha)));
    }
}
