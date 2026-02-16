package bodevelopment.client.blackout.rendering.renderer;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.rendering.framebuffer.FrameBuffer;

public class FrameBufferRenderer {
    public static void renderToBuffer(String buffer, Runnable runnable) {
        renderToBuffer(Managers.FRAME_BUFFER.getBuffer(buffer), runnable);
    }

    public static void renderToBuffer(FrameBuffer buffer, Runnable runnable) {
        buffer.bind(true);
        runnable.run();
        BlackOut.mc.getFramebuffer().beginWrite(true);
    }
}
