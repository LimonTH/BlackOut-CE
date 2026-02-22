package bodevelopment.client.blackout.util.render;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import java.util.ArrayList;
import java.util.List;

public class ModelVertexConsumer implements VertexConsumer {
    public final List<Vec3d[]> positions = new ArrayList<>();
    private Vec3d[] currentArray = new Vec3d[4];
    private int i = 0;

    public void start() {
        this.positions.clear();
        this.i = 0;
        this.currentArray = new Vec3d[4];
    }

    @Override
    public VertexConsumer vertex(Matrix4f matrix, float x, float y, float z) {
        Vector4f pos = new Vector4f(x, y, z, 1.0F).mul(matrix);
        addVertex(pos.x(), pos.y(), pos.z());
        return this;
    }

    @Override
    public VertexConsumer vertex(float x, float y, float z) {
        addVertex(x, y, z);
        return this;
    }

    private void addVertex(double x, double y, double z) {
        if (this.currentArray == null) {
            this.currentArray = new Vec3d[4];
        }

        this.currentArray[this.i++] = new Vec3d(x, y, z);

        if (this.i >= 4) {
            this.positions.add(this.currentArray);
            this.currentArray = new Vec3d[4];
            this.i = 0;
        }
    }

    @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
    @Override public VertexConsumer texture(float u, float v) { return this; }
    @Override public VertexConsumer overlay(int u, int v) { return this; }
    @Override public VertexConsumer light(int u, int v) { return this; }
    @Override public VertexConsumer normal(float x, float y, float z) { return this; }
}