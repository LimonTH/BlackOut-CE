package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.rendering.framebuffer.BlendFrameBuffer;
import bodevelopment.client.blackout.rendering.framebuffer.FrameBuffer;
import bodevelopment.client.blackout.rendering.framebuffer.GuiAlphaFrameBuffer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;

public class FrameBufferManager extends Manager {
    private final Map<String, FrameBuffer> buffers = new Object2ObjectOpenHashMap<>();

    public FrameBuffer getBuffer(String name) {
        return this.buffers.computeIfAbsent(name, n -> this.add(n, new FrameBuffer()));
    }

    public BlendFrameBuffer getBlend(String name) {
        return (BlendFrameBuffer) this.buffers.computeIfAbsent(name, n -> this.add(n, new BlendFrameBuffer()));
    }

    public GuiAlphaFrameBuffer getGui() {
        return (GuiAlphaFrameBuffer) this.buffers.computeIfAbsent("gui", n -> this.add(n, new GuiAlphaFrameBuffer()));
    }

    public <T extends FrameBuffer> T add(String name, T buffer) {
        this.buffers.put(name, buffer);
        return buffer;
    }

    public void onResize() {
        this.buffers.forEach((name, buffer) -> buffer.resize());
    }
}
