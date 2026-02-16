package bodevelopment.client.blackout.util.render;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;

public class ModelVertexConsumer implements VertexConsumer {
    public final List<Vec3d[]> positions = new ArrayList<>();
    private float prevX, prevY, prevZ;
    private Vec3d[] currentArray;
    private int i = 0;

    public void start() {
        this.positions.clear();
        this.startArray();
    }

    private void startArray() {
        this.currentArray = new Vec3d[4];
        this.i = 0;
    }

    @Override
    public VertexConsumer vertex(float x, float y, float z) {
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        return this;
    }

    @Override
    public VertexConsumer color(int argb) {
        return this;
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        return this;
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer light(int u, int v) {
        return this;
    }

    @Override
    public VertexConsumer light(int uv) {
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        return this;
    }

    public void step() {
        if (this.currentArray == null) this.startArray();

        this.currentArray[this.i++] = new Vec3d(this.prevX, this.prevY, this.prevZ);
        if (this.i >= 4) {
            this.positions.add(this.currentArray);
            this.startArray();
        }
    }
}