package bodevelopment.client.blackout.util.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jetbrains.annotations.NotNull;

public class DoubleSidedVertexConsumer implements VertexConsumer {

    private static final int QUAD_SIZE = 4;

    private final VertexConsumer delegate;

    private final float[] x      = new float[QUAD_SIZE];
    private final float[] y      = new float[QUAD_SIZE];
    private final float[] z      = new float[QUAD_SIZE];
    private final int[]   color  = new int[QUAD_SIZE * 4]; // r,g,b,a per vertex
    private final float[] u      = new float[QUAD_SIZE];
    private final float[] v      = new float[QUAD_SIZE];
    private final int[]   uv1u   = new int[QUAD_SIZE];
    private final int[]   uv1v   = new int[QUAD_SIZE];
    private final int[]   uv2u   = new int[QUAD_SIZE];
    private final int[]   uv2v   = new int[QUAD_SIZE];
    private final float[] nx     = new float[QUAD_SIZE];
    private final float[] ny     = new float[QUAD_SIZE];
    private final float[] nz     = new float[QUAD_SIZE];

    private int currentVertex = 0;
    private boolean hasColor, hasUv, hasUv1, hasUv2, hasNormal;

    public DoubleSidedVertexConsumer(VertexConsumer delegate) {
        this.delegate = delegate;
    }

    @Override
    public @NotNull VertexConsumer addVertex(float px, float py, float pz) {
        x[currentVertex] = px;
        y[currentVertex] = py;
        z[currentVertex] = pz;
        hasColor = false; hasUv = false; hasUv1 = false; hasUv2 = false; hasNormal = false;
        return this;
    }

    @Override
    public @NotNull VertexConsumer setColor(int r, int g, int b, int a) {
        int i = currentVertex * 4;
        color[i]     = r;
        color[i + 1] = g;
        color[i + 2] = b;
        color[i + 3] = a;
        hasColor = true;
        tryAdvance();
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv(float pu, float pv) {
        u[currentVertex] = pu;
        v[currentVertex] = pv;
        hasUv = true;
        tryAdvance();
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv1(int pu, int pv) {
        uv1u[currentVertex] = pu;
        uv1v[currentVertex] = pv;
        hasUv1 = true;
        tryAdvance();
        return this;
    }

    @Override
    public @NotNull VertexConsumer setUv2(int pu, int pv) {
        uv2u[currentVertex] = pu;
        uv2v[currentVertex] = pv;
        hasUv2 = true;
        tryAdvance();
        return this;
    }

    @Override
    public @NotNull VertexConsumer setNormal(float pnx, float pny, float pnz) {
        nx[currentVertex] = pnx;
        ny[currentVertex] = pny;
        nz[currentVertex] = pnz;
        hasNormal = true;
        tryAdvance();
        return this;
    }

    private void tryAdvance() {
        if (hasColor && hasUv && hasUv1 && hasUv2 && hasNormal) {
            currentVertex++;
            hasColor = false; hasUv = false; hasUv1 = false; hasUv2 = false; hasNormal = false;
            if (currentVertex == QUAD_SIZE) {
                flushQuad();
                currentVertex = 0;
            }
        }
    }

    private void flushQuad() {
        emitVertices(0, 1, 2, 3);
        emitVertices(3, 2, 1, 0);
    }

    private void emitVertices(int a, int b, int c, int d) {
        emitVertex(a);
        emitVertex(b);
        emitVertex(c);
        emitVertex(d);
    }

    private void emitVertex(int i) {
        int ci = i * 4;
        delegate.addVertex(x[i], y[i], z[i]);
        delegate.setColor(color[ci], color[ci+1], color[ci+2], color[ci+3]);
        delegate.setUv(u[i], v[i]);
        delegate.setUv1(uv1u[i], uv1v[i]);
        delegate.setUv2(uv2u[i], uv2v[i]);
        delegate.setNormal(nx[i], ny[i], nz[i]);
    }
}