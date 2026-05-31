package bodevelopment.client.blackout.rendering.framebuffer;

import bodevelopment.client.blackout.annotations.Internal;

import org.lwjgl.opengl.GL14;

@Internal
public class BlendFrameBuffer extends FrameBuffer {
    public void start() {
        this.clear(0.0F, 0.0F, 0.0F, 0.0F);
        this.bind(true);
        GL14.glBlendFuncSeparate(770, 771, 1, 1);
    }
}
