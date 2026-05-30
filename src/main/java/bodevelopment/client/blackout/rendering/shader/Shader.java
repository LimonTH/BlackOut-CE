package bodevelopment.client.blackout.rendering.shader;

import bodevelopment.client.blackout.annotations.NoAlloc;
import bodevelopment.client.blackout.annotations.ThreadSafe;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.util.ARGB;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30C;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps an OpenGL shader program with automatic uniform caching and
 * batched uniform upload.
 *
 * <p>Uniforms set via {@code set()} / {@code setIf()} are cached and only
 * uploaded to the GPU when the program is bound via {@link #bind()}.
 * {@code setIf()} silently ignores uniforms that don't exist in the shader,
 * making it safe to set "optional" uniforms without GL errors.
 */
@ThreadSafe
public class Shader {

    private static int boundProgram;

    private final Map<String, Uniform> uniformCache = new HashMap<>();
    private final List<String> absentUniforms = new ArrayList<>();
    private final int programId;
    private final long initTime = System.currentTimeMillis();

    public Shader(String name) {
        this.programId = ShaderReader.create(name);
    }

    public void render(BufferBuilder bufferBuilder, ShaderSetup shaderSetup) {
        MeshData builtBuffer = bufferBuilder.buildOrThrow();
        if (builtBuffer == null) return;
        if (shaderSetup != null) shaderSetup.setup(this);

        VertexBuffer vertexBuffer = builtBuffer.drawState().format().getImmediateDrawVertexBuffer();
        vertexBuffer.bind();
        vertexBuffer.upload(builtBuffer);
        draw(vertexBuffer);
    }

    @NoAlloc
    private void draw(VertexBuffer vertexBuffer) {
        Matrix4f modelView = RenderSystem.getModelViewMatrix();
        if (modelView != null) setIf("ModelViewMat", modelView);
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        if (projection != null) setIf("ProjMat", projection);

        setIf("uAlpha", Renderer.getAlpha());

        var poseStack = Renderer.getMatrices();
        if (poseStack != null && poseStack.last() != null) {
            Matrix4f pose = poseStack.last().pose();
            Matrix3f normal = poseStack.last().normal();
            if (pose != null) setIf("uMatrices", pose);
            if (normal != null) setIf("uMatrices2", normal);
        }

        setIf("uResolution",
                bodevelopment.client.blackout.util.ScreenUtils.screenWidth(),
                bodevelopment.client.blackout.util.ScreenUtils.screenHeight());

        timeIf(this.initTime);

        int previousProgram = boundProgram;
        bind();
        vertexBuffer.draw();
        unbind();
        if (previousProgram != 0) {
            GlStateManager._glUseProgram(previousProgram);
            boundProgram = previousProgram;
        }
    }

    private boolean hasUniform(String name) {
        if (absentUniforms.contains(name)) return false;
        if (uniformCache.containsKey(name)) return true;
        if (GL30C.glGetUniformLocation(programId, name) == -1) {
            absentUniforms.add(name);
            return false;
        }
        return true;
    }

    private Uniform getUniform(String name, int length, UniformType type) {
        return uniformCache.computeIfAbsent(name,
                k -> new Uniform(GL30C.glGetUniformLocation(programId, k), type, length));
    }

    public void bind() {
        GlStateManager._glUseProgram(programId);
        boundProgram = programId;
        uniformCache.forEach((name, uniform) -> uniform.upload());
    }

    public void unbind() {
        GlStateManager._glUseProgram(0);
        boundProgram = 0;
    }

    public void set(String uniform, float f)                { getUniform(uniform, 1, UniformType.Float).set(f); }
    public void set(String uniform, float x, float y)        { getUniform(uniform, 2, UniformType.Float).set(x, y); }
    public void set(String uniform, float x, float y, float z) { getUniform(uniform, 3, UniformType.Float).set(x, y, z); }
    public void set(String uniform, float x, float y, float z, float a) { getUniform(uniform, 4, UniformType.Float).set(x, y, z, a); }
    public void set(String uniform, int i)                   { getUniform(uniform, 1, UniformType.Integer).set(i); }
    public void set(String uniform, Matrix4f mat)            { getUniform(uniform, 16, UniformType.Matrix).set(mat); }

    public void time(long initTime)                          { set("time", (float)(System.currentTimeMillis() - initTime) / 1000f); }
    public void color(String uniform, int color)             { set(uniform, ARGB.red(color)/255f, ARGB.green(color)/255f, ARGB.blue(color)/255f, ARGB.alpha(color)/255f); }
    public void color(String uniform, BlackOutColor color)   { set(uniform, color.red/255f, color.green/255f, color.blue/255f, color.alpha/255f); }

    public void setIf(String uniform, float f)                { if (hasUniform(uniform)) getUniform(uniform, 1, UniformType.Float).set(f); }
    public void setIf(String uniform, float x, float y)        { if (hasUniform(uniform)) getUniform(uniform, 2, UniformType.Float).set(x, y); }
    public void setIf(String uniform, float x, float y, float z) { if (hasUniform(uniform)) getUniform(uniform, 3, UniformType.Float).set(x, y, z); }
    public void setIf(String uniform, float x, float y, float z, float a) { if (hasUniform(uniform)) getUniform(uniform, 4, UniformType.Float).set(x, y, z, a); }
    public void setIf(String uniform, int i)                   { if (hasUniform(uniform)) getUniform(uniform, 1, UniformType.Integer).set(i); }
    public void setIf(String uniform, Matrix3f mat)            { if (hasUniform(uniform)) getUniform(uniform, 9, UniformType.Matrix).set(mat); }
    public void setIf(String uniform, Matrix4f mat)            { if (hasUniform(uniform)) getUniform(uniform, 16, UniformType.Matrix).set(mat); }

    public void timeIf(long initTime)                          { if (hasUniform("time")) time(initTime); }
    public void colorIf(String uniform, int color)             { setIf(uniform, ARGB.red(color)/255f, ARGB.green(color)/255f, ARGB.blue(color)/255f, ARGB.alpha(color)/255f); }
    public void colorIf(String uniform, BlackOutColor color)   { setIf(uniform, color.red/255f, color.green/255f, color.blue/255f, color.alpha/255f); }

    private enum UniformType { Integer, Float, Matrix }

    private static class Uniform {
        private final int location;
        private final int length;
        private final UniformType type;
        private float[] floatData;
        private int[] intData;

        Uniform(int location, UniformType type, int length) {
            this.location = location;
            this.length = length;
            this.type = type;
            if (type == UniformType.Integer) {
                intData = new int[length];
            } else {
                floatData = new float[length];
            }
        }

        void set(float... values)   { floatData = values; }
        void set(int... values)     { intData = values; }
        void set(Matrix4f mat)      { mat.get(floatData); }
        void set(Matrix3f mat)      { mat.get(floatData); }

        void upload() {
            switch (type) {
                case Integer -> {
                    switch (length) {
                        case 1 -> GL30C.glUniform1iv(location, intData);
                        case 2 -> GL30C.glUniform2iv(location, intData);
                        case 3 -> GL30C.glUniform3iv(location, intData);
                        case 4 -> GL30C.glUniform4iv(location, intData);
                    }
                }
                case Float -> {
                    switch (length) {
                        case 1 -> GL30C.glUniform1fv(location, floatData);
                        case 2 -> GL30C.glUniform2fv(location, floatData);
                        case 3 -> GL30C.glUniform3fv(location, floatData);
                        case 4 -> GL30C.glUniform4fv(location, floatData);
                    }
                }
                case Matrix -> GL30C.glUniformMatrix4fv(location, false, floatData);
            }
        }
    }
}
