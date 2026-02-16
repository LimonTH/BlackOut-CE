package bodevelopment.client.blackout.rendering.shader;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.GlProgramManager;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30C;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Shader {
    private final int[] currentShader = new int[1];
    private final Map<String, Uniform> uniformMap = new HashMap<>();
    private final List<String> absent = new ArrayList<>();
    private final int id;
    private final long initTime = System.currentTimeMillis();

    public Shader(String name) {
        this.id = ShaderReader.create(name);
    }

    public void render(BufferBuilder bufferBuilder, ShaderSetup shaderSetup) {
        BuiltBuffer builtBuffer = bufferBuilder.end();
        if (builtBuffer == null) return;
        if (shaderSetup != null) shaderSetup.setup(this);

        VertexBuffer vertexBuffer = builtBuffer.getDrawParameters().format().getBuffer();
        vertexBuffer.bind();
        vertexBuffer.upload(builtBuffer);
        this.draw(vertexBuffer);
    }

    private void draw(VertexBuffer vertexBuffer) {
        Matrix4f modelViewMat = RenderSystem.getModelViewMatrix();
        Matrix4f projMat = RenderSystem.getProjectionMatrix();
        if (modelViewMat != null) {
            this.setIf("ModelViewMat", modelViewMat);
        }
        if (projMat != null) {
            this.setIf("ProjMat", projMat);
        }
        this.setIf("uAlpha", Renderer.getAlpha());
        if (Renderer.getMatrices() != null && Renderer.getMatrices().peek() != null) {
            Matrix4f positionMatrix = Renderer.getMatrices().peek().getPositionMatrix();
            Matrix3f normalMatrix = Renderer.getMatrices().peek().getNormalMatrix();
            if (positionMatrix != null) {
                this.setIf("uMatrices", positionMatrix);
            }
            if (normalMatrix != null) {
                this.setIf("uMatrices2", normalMatrix);
            }
        }

        this.setIf("uResolution", BlackOut.mc.getWindow().getWidth(), BlackOut.mc.getWindow().getHeight());
        this.timeIf(this.initTime);
        GL30C.glGetIntegerv(35725, this.currentShader);
        this.bind();
        vertexBuffer.draw();
        this.unbind();
        GlProgramManager.useProgram(this.currentShader[0]);
    }

    private boolean exists(String name) {
        if (this.absent.contains(name)) {
            return false;
        } else if (this.uniformMap.containsKey(name)) {
            return true;
        } else if (GL30C.glGetUniformLocation(this.id, name) == -1) {
            this.absent.add(name);
            return false;
        } else {
            return true;
        }
    }

    private Uniform getUniform(String uniform, int length, UniformType type) {
        return this.uniformMap.computeIfAbsent(uniform, name -> new Uniform(GL30C.glGetUniformLocation(this.id, name), type, length));
    }

    public void bind() {
        GlProgramManager.useProgram(this.id);
        this.uniformMap.forEach((name, uniform) -> uniform.upload());
    }

    public void unbind() {
        GlProgramManager.useProgram(0);
    }

    public void set(String uniform, float f) {
        this.getUniform(uniform, 1, UniformType.Float).set(f);
    }

    public void set(String uniform, float x, float y) {
        this.getUniform(uniform, 2, UniformType.Float).set(x, y);
    }

    public void set(String uniform, float x, float y, float z) {
        this.getUniform(uniform, 3, UniformType.Float).set(x, y, z);
    }

    public void set(String uniform, float x, float y, float z, float a) {
        this.getUniform(uniform, 4, UniformType.Float).set(x, y, z, a);
    }

    public void set(String uniform, int i) {
        this.getUniform(uniform, 1, UniformType.Integer).set(i);
    }

    public void set(String uniform, Matrix4f matrix4f) {
        this.getUniform(uniform, 16, UniformType.Matrix).set(matrix4f);
    }

    public void time(long initTime) {
        this.set("time", (float) (System.currentTimeMillis() - initTime) / 1000.0F);
    }

    public void color(String uniform, int color) {
        this.set(uniform, (color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, (color >>> 24) / 255.0F);
    }

    public void color(String uniform, BlackOutColor color) {
        this.set(uniform, color.red / 255.0F, color.green / 255.0F, color.blue / 255.0F, color.alpha / 255.0F);
    }

    public void setIf(String uniform, float f) {
        if (this.exists(uniform)) {
            this.getUniform(uniform, 1, UniformType.Float).set(f);
        }
    }

    public void setIf(String uniform, float x, float y) {
        if (this.exists(uniform)) {
            this.getUniform(uniform, 2, UniformType.Float).set(x, y);
        }
    }

    public void setIf(String uniform, float x, float y, float z) {
        if (this.exists(uniform)) {
            this.getUniform(uniform, 3, UniformType.Float).set(x, y, z);
        }
    }

    public void setIf(String uniform, float x, float y, float z, float a) {
        if (this.exists(uniform)) {
            this.getUniform(uniform, 4, UniformType.Float).set(x, y, z, a);
        }
    }

    public void setIf(String uniform, int i) {
        if (this.exists(uniform)) {
            this.getUniform(uniform, 1, UniformType.Integer).set(i);
        }
    }

    public void setIf(String uniform, Matrix3f matrix3f) {
        if (this.exists(uniform)) {
            this.getUniform(uniform, 9, UniformType.Matrix).set(matrix3f);
        }
    }

    public void setIf(String uniform, Matrix4f matrix4f) {
        if (this.exists(uniform)) {
            this.getUniform(uniform, 16, UniformType.Matrix).set(matrix4f);
        }
    }

    public void timeIf(long initTime) {
        if (this.exists("time")) {
            this.set("time", (float) (System.currentTimeMillis() - initTime) / 1000.0F);
        }
    }

    public void colorIf(String uniform, int color) {
        this.setIf(uniform, (color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, (color >>> 24) / 255.0F);
    }

    public void colorIf(String uniform, BlackOutColor color) {
        this.setIf(uniform, color.red / 255.0F, color.green / 255.0F, color.blue / 255.0F, color.alpha / 255.0F);
    }

    private enum UniformType {
        Integer,
        Float,
        Matrix
    }

    private static class Uniform {
        private final int location;
        private final int length;
        private final UniformType type;
        private float[] floatArray;
        private int[] intArray;
        private boolean boolValue;

        private Uniform(int location, UniformType type, int length) {
            this.location = location;
            this.length = length;
            this.type = type;
            switch (type) {
                case Integer:
                    this.floatArray = null;
                    this.intArray = new int[length];
                    break;
                case Float:
                case Matrix:
                    this.floatArray = new float[length];
                    this.intArray = null;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + type + "\n REPORT TO OLEPOSSU ON DISCORD");
            }
        }

        private void set(float... floats) {
            this.floatArray = floats;
        }

        private void set(int... ints) {
            this.intArray = ints;
        }

        private void set(Matrix4f matrix4f) {
            matrix4f.get(this.floatArray);
        }

        private void set(Matrix3f matrix3f) {
            matrix3f.get(this.floatArray);
        }

        private void set(boolean bool) {
            this.boolValue = bool;
        }

        private void upload() {
            switch (this.type) {
                case Integer:
                    switch (this.length) {
                        case 1:
                            GL30C.glUniform1iv(this.location, this.intArray);
                            return;
                        case 2:
                            GL30C.glUniform2iv(this.location, this.intArray);
                            return;
                        case 3:
                            GL30C.glUniform3iv(this.location, this.intArray);
                            return;
                        case 4:
                            GL30C.glUniform4iv(this.location, this.intArray);
                            return;
                        default:
                            return;
                    }
                case Float:
                    switch (this.length) {
                        case 1:
                            GL30C.glUniform1fv(this.location, this.floatArray);
                            return;
                        case 2:
                            GL30C.glUniform2fv(this.location, this.floatArray);
                            return;
                        case 3:
                            GL30C.glUniform3fv(this.location, this.floatArray);
                            return;
                        case 4:
                            GL30C.glUniform4fv(this.location, this.floatArray);
                            return;
                        default:
                            return;
                    }
                case Matrix:
                    GL30C.glUniformMatrix4fv(this.location, false, this.floatArray);
            }
        }
    }
}
