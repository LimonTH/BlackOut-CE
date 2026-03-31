package bodevelopment.client.blackout.util.render.consumers;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

public class XRayVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final int alpha;

    public XRayVertexConsumer(VertexConsumer delegate, int alpha) {
        this.delegate = delegate;
        this.alpha = alpha;
    }

    @Override
    public @NotNull VertexConsumer addVertex(float x, float y, float z) {
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setColor(int r, int g, int b, int a) {
        delegate.setColor(r, g, b, this.alpha);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv(float u, float v) {
        delegate.setUv(u, v);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv1(int u, int v) {
        delegate.setUv1(u, v);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv2(int u, int v) {
        delegate.setUv2(u, v);
        return this;
    }

    @Override
    public @NotNull VertexConsumer setNormal(float x, float y, float z) {
        delegate.setNormal(x, y, z);
        return this;
    }
}