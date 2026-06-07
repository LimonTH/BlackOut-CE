package bodevelopment.client.blackout.util.render.consumers;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class CapeVertexConsumer implements VertexConsumer {
    private static final float EXPECTED_W = 64f;
    private static final float EXPECTED_H = 64f;

    private final VertexConsumer delegate;
    private final float scaleU;
    private final float scaleV;

    public CapeVertexConsumer(VertexConsumer delegate, float texWidth, float texHeight) {
        this.delegate = delegate;
        this.scaleU = EXPECTED_W / texWidth;
        this.scaleV = EXPECTED_H / texHeight;
    }

    @Override
    public @NotNull VertexConsumer addVertex(float x, float y, float z) {
        return delegate.addVertex(x, y, z);
    }

    @Override
    public @NotNull VertexConsumer addVertex(Matrix4f matrix, float x, float y, float z) {
        return delegate.addVertex(matrix, x, y, z);
    }

    @Override
    public @NotNull VertexConsumer setColor(int r, int g, int b, int a) {
        return delegate.setColor(r, g, b, a);
    }

    @Override
    public @NotNull VertexConsumer setUv(float u, float v) {
        return delegate.setUv(u * scaleU, v * scaleV);
    }

    @Override
    public @NotNull VertexConsumer setUv1(int u, int v) {
        return delegate.setUv1(u, v);
    }

    @Override
    public @NotNull VertexConsumer setUv2(int u, int v) {
        return delegate.setUv2(u, v);
    }

    @Override
    public @NotNull VertexConsumer setNormal(float x, float y, float z) {
        return delegate.setNormal(x, y, z);
    }

    @Override
    public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        delegate.addVertex(x, y, z, color, u * scaleU, v * scaleV, overlay, light, normalX, normalY, normalZ);
    }
}
