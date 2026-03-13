package bodevelopment.client.blackout.util.render;

import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

public class ModelVertexConsumer implements VertexConsumer {
    public final List<List<Vec3>> parts = new ArrayList<>();
    private List<Vec3> currentPart = new ArrayList<>();
    private final List<Vec3> currentPolygon = new ArrayList<>();

    public final List<Vec3> vertices = new ArrayList<>();

    public void start() {
        this.parts.clear();
        this.vertices.clear();
        this.currentPart = new ArrayList<>();
        this.currentPolygon.clear();
    }

    public void nextPart() {
        fixRemaining();
        if (!currentPart.isEmpty()) {
            parts.add(new ArrayList<>(currentPart));
            currentPart.clear();
        }
    }

    @Override
    public @NotNull VertexConsumer addVertex(float x, float y, float z) {
        writeVertex(x, y, z);
        checkPolygon();
        return this;
    }

    @Override
    public @NotNull VertexConsumer addVertex(Matrix4f matrix, float x, float y, float z) {
        Vector4f pos = new Vector4f(x, y, z, 1.0F).mul(matrix);
        writeVertex(pos.x(), pos.y(), pos.z());
        checkPolygon();
        return this;
    }

    private void writeVertex(double x, double y, double z) {
        currentPolygon.add(new Vec3(x, y, z));
    }

    private void checkPolygon() {
        if (currentPolygon.size() >= 4) {
            Vec3 v0 = currentPolygon.get(0);
            Vec3 v1 = currentPolygon.get(1);
            Vec3 v2 = currentPolygon.get(2);
            Vec3 v3 = currentPolygon.get(3);

            addTriangle(v0, v1, v2);
            addTriangle(v0, v2, v3);

            currentPolygon.clear();
        }
    }

    private void addTriangle(Vec3 v1, Vec3 v2, Vec3 v3) {
        vertices.add(v1); vertices.add(v2); vertices.add(v3);
        currentPart.add(v1); currentPart.add(v2); currentPart.add(v3);
    }

    public void fixRemaining() {
        if (currentPolygon.size() == 3) {
            addTriangle(currentPolygon.get(0), currentPolygon.get(1), currentPolygon.get(2));
            currentPolygon.clear();
        }
    }

    @Override public @NotNull VertexConsumer setColor(int r, int g, int b, int a) { return this; }
    @Override public @NotNull VertexConsumer setUv(float u, float v) { return this; }
    @Override public @NotNull VertexConsumer setUv1(int u, int v) { return this; }
    @Override public @NotNull VertexConsumer setUv2(int u, int v) { return this; }
    @Override public @NotNull VertexConsumer setNormal(float x, float y, float z) { return this; }

    public record Wrapper(ModelVertexConsumer parent) implements VertexConsumer {
        @Override
        public @NotNull VertexConsumer addVertex(float x, float y, float z) {
            parent.addVertex(x, y, z);
            return this;
        }

        @Override
        public @NotNull VertexConsumer addVertex(Matrix4f matrix, float x, float y, float z) {
            parent.addVertex(matrix, x, y, z);
            return this;
        }

        @Override public @NotNull VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public @NotNull VertexConsumer setUv(float u, float v) { return this; }
        @Override public @NotNull VertexConsumer setUv1(int u, int v) { return this; }
        @Override public @NotNull VertexConsumer setUv2(int u, int v) { return this; }
        @Override public @NotNull VertexConsumer setNormal(float x, float y, float z) { return this; }
    }
}