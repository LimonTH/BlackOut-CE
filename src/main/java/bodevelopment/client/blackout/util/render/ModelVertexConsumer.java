package bodevelopment.client.blackout.util.render;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

public class ModelVertexConsumer implements VertexConsumer {
    public final List<Vec3[]> positions = new ArrayList<>();
    private Vec3[] currentArray = new Vec3[4];
    private int i = 0;

    public void start() {
        this.positions.clear();
        this.i = 0;
        this.currentArray = new Vec3[4];
    }

    @Override
    public VertexConsumer addVertex(Matrix4f matrix, float x, float y, float z) {
        Vector4f pos = new Vector4f(x, y, z, 1.0F).mul(matrix);
        addVertex(pos.x(), pos.y(), pos.z());
        return this;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        addVertex(x, y, z);
        return this;
    }

    private void addVertex(double x, double y, double z) {
        if (this.currentArray == null) {
            this.currentArray = new Vec3[4];
        }

        this.currentArray[this.i++] = new Vec3(x, y, z);

        if (this.i >= 4) {
            this.positions.add(this.currentArray);
            this.currentArray = new Vec3[4];
            this.i = 0;
        }
    }

    @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
    @Override public VertexConsumer setUv(float u, float v) { return this; }
    @Override public VertexConsumer setUv1(int u, int v) { return this; }
    @Override public VertexConsumer setUv2(int u, int v) { return this; }
    @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
}