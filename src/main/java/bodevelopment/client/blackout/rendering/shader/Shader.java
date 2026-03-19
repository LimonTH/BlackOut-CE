package bodevelopment.client.blackout.rendering.shader;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30C;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Shader {
    private final int[] currentShader = new int[1];
    private final Map<String, Uniform> uniformMap = new HashMap<>();
    private final List<String> absent = new ArrayList<>();
    private final int id;
    private final String name;
    private final long initTime = System.currentTimeMillis();
    private boolean logged = false;

    public Shader(String name) {
        this.name = name;
        this.id = ShaderReader.create(name);
    }

    public void render(BufferBuilder bufferBuilder, ShaderSetup shaderSetup) {
        MeshData meshData = bufferBuilder.buildOrThrow();
        if (meshData == null) return;
        if (shaderSetup != null) shaderSetup.setup(this);

        MeshData.DrawState drawState = meshData.drawState();
        VertexFormat format = drawState.format();

        ByteBuffer vertexData = meshData.vertexBuffer();
        ByteBuffer indexData = meshData.indexBuffer();

        if (!this.logged) {
            System.out.println("[BO-VTX] shader=" + this.name
                + " vBuf: pos=" + vertexData.position() + " lim=" + vertexData.limit() + " rem=" + vertexData.remaining()
                + " iBuf: " + (indexData != null ? "pos=" + indexData.position() + " lim=" + indexData.limit() + " rem=" + indexData.remaining() : "null")
                + " format=" + format + " stride=" + format.getVertexSize()
                + " vertCount=" + drawState.vertexCount());
            // Dump first vertex position (3 floats = 12 bytes)
            if (vertexData.remaining() >= 12) {
                float v0x = vertexData.getFloat(vertexData.position());
                float v0y = vertexData.getFloat(vertexData.position() + 4);
                float v0z = vertexData.getFloat(vertexData.position() + 8);
                System.out.println("[BO-VTX] first vertex: " + v0x + ", " + v0y + ", " + v0z);
            }
            if (vertexData.remaining() >= format.getVertexSize() * 2) {
                int stride = format.getVertexSize();
                float v1x = vertexData.getFloat(vertexData.position() + stride);
                float v1y = vertexData.getFloat(vertexData.position() + stride + 4);
                float v1z = vertexData.getFloat(vertexData.position() + stride + 8);
                System.out.println("[BO-VTX] second vertex: " + v1x + ", " + v1y + ", " + v1z);
            }
        }

        int vao = GL30C.glGenVertexArrays();
        GL30C.glBindVertexArray(vao);

        int vbo = GL30C.glGenBuffers();
        GL30C.glBindBuffer(GL30C.GL_ARRAY_BUFFER, vbo);
        GL30C.glBufferData(GL30C.GL_ARRAY_BUFFER, vertexData, GL30C.GL_DYNAMIC_DRAW);

        setupVertexAttributes(format);

        int ibo = 0;
        boolean hasIndices = indexData != null && indexData.remaining() > 0;

        if (hasIndices) {
            ibo = GL30C.glGenBuffers();
            GL30C.glBindBuffer(GL30C.GL_ELEMENT_ARRAY_BUFFER, ibo);
            GL30C.glBufferData(GL30C.GL_ELEMENT_ARRAY_BUFFER, indexData, GL30C.GL_DYNAMIC_DRAW);
        } else if (drawState.mode() == VertexFormat.Mode.QUADS) {
            // QUADS mode needs index data for triangle conversion (0,1,2, 2,3,0 per quad)
            int quadCount = drawState.vertexCount() / 4;
            int indexCount = quadCount * 6;
            java.nio.IntBuffer quadIndices = org.lwjgl.system.MemoryUtil.memAllocInt(indexCount);
            for (int q = 0; q < quadCount; q++) {
                int base = q * 4;
                quadIndices.put(base).put(base + 1).put(base + 2);
                quadIndices.put(base + 2).put(base + 3).put(base);
            }
            quadIndices.flip();
            ibo = GL30C.glGenBuffers();
            GL30C.glBindBuffer(GL30C.GL_ELEMENT_ARRAY_BUFFER, ibo);
            GL30C.glBufferData(GL30C.GL_ELEMENT_ARRAY_BUFFER, quadIndices, GL30C.GL_DYNAMIC_DRAW);
            org.lwjgl.system.MemoryUtil.memFree(quadIndices);
            hasIndices = true;
        }

        this.drawRaw(drawState, hasIndices, ibo != 0 && indexData == null);

        GL30C.glBindVertexArray(0);
        GL30C.glDeleteBuffers(vbo);
        if (ibo != 0) GL30C.glDeleteBuffers(ibo);
        GL30C.glDeleteVertexArrays(vao);

        meshData.close();
    }

    private static void setupVertexAttributes(VertexFormat format) {
        int stride = format.getVertexSize();
        int[] offsets = format.getOffsetsByElement();
        List<VertexFormatElement> elements = format.getElements();

        for (int i = 0; i < elements.size(); i++) {
            VertexFormatElement element = elements.get(i);
            int location = element.id();
            int offset = offsets[location];
            int glType = switch (element.type()) {
                case FLOAT -> GL30C.GL_FLOAT;
                case UBYTE -> GL30C.GL_UNSIGNED_BYTE;
                case BYTE -> GL30C.GL_BYTE;
                case USHORT -> GL30C.GL_UNSIGNED_SHORT;
                case SHORT -> GL30C.GL_SHORT;
                case UINT -> GL30C.GL_UNSIGNED_INT;
                case INT -> GL30C.GL_INT;
            };
            boolean normalized = element.usage() == VertexFormatElement.Usage.COLOR;

            GL30C.glEnableVertexAttribArray(location);
            GL30C.glVertexAttribPointer(location, element.count(), glType, normalized, stride, offset);
        }
    }

    private void drawRaw(MeshData.DrawState drawState, boolean hasIndices, boolean generatedIntIndices) {
        Matrix4f modelViewMat = RenderSystem.getModelViewMatrix();
        this.setIf("ModelViewMat", modelViewMat != null ? modelViewMat : new Matrix4f());
        Matrix4f projMat = Renderer.getProjectionMatrix();
        this.setIf("ProjMat", projMat);

        if (!this.logged) {
            this.logged = true;
            int[] fbo = new int[1];
            int[] viewport = new int[4];
            GL30C.glGetIntegerv(GL30C.GL_FRAMEBUFFER_BINDING, fbo);
            GL30C.glGetIntegerv(GL30C.GL_VIEWPORT, viewport);
            while (GL30C.glGetError() != 0) {}
            System.out.println("[BO] shader=" + this.name + " id=" + this.id
                + " verts=" + drawState.vertexCount() + " mode=" + drawState.mode()
                + " hasIdx=" + hasIndices + " genIdx=" + generatedIntIndices
                + " fbo=" + fbo[0] + " vp=[" + viewport[0] + "," + viewport[1] + "," + viewport[2] + "," + viewport[3] + "]"
                + " proj=[" + projMat.m00() + "," + projMat.m11() + "," + projMat.m22() + "]"
                + " mv=" + (modelViewMat != null ? "[" + modelViewMat.m00() + "," + modelViewMat.m11() + "]" : "null"));
        }

        // Save and set GL state
        boolean wasDepthTest = GL30C.glIsEnabled(GL30C.GL_DEPTH_TEST);
        boolean wasCullFace = GL30C.glIsEnabled(GL30C.GL_CULL_FACE);
        boolean wasBlend = GL30C.glIsEnabled(GL30C.GL_BLEND);
        if (wasDepthTest) GL30C.glDisable(GL30C.GL_DEPTH_TEST);
        if (wasCullFace) GL30C.glDisable(GL30C.GL_CULL_FACE);
        if (!wasBlend) GL30C.glEnable(GL30C.GL_BLEND);
        GL30C.glBlendFuncSeparate(GL30C.GL_SRC_ALPHA, GL30C.GL_ONE_MINUS_SRC_ALPHA, GL30C.GL_ONE, GL30C.GL_ZERO);

        this.setIf("uAlpha", Renderer.getAlpha());
        if (Renderer.getMatrices() != null && Renderer.getMatrices().last() != null) {
            Matrix4f positionMatrix = Renderer.getMatrices().last().pose();
            Matrix3f normalMatrix = Renderer.getMatrices().last().normal();
            if (positionMatrix != null) {
                this.setIf("uMatrices", positionMatrix);
            }
            if (normalMatrix != null) {
                this.setIf("uMatrices2", normalMatrix);
            }
        }

        this.setIf("uResolution", BlackOut.mc.getWindow().getScreenWidth(), BlackOut.mc.getWindow().getScreenHeight());
        this.timeIf(this.initTime);
        GL30C.glGetIntegerv(35725, this.currentShader);
        this.bind();

        if (!this.logged) {
            int[] boundProg = new int[1];
            GL30C.glGetIntegerv(GL30C.GL_CURRENT_PROGRAM, boundProg);
            System.out.println("[BO-GL] shader=" + this.name + " boundProgram=" + boundProg[0] + " expected=" + this.id);
            // Check if ProjMat uniform exists and what location it has
            int projLoc = GL30C.glGetUniformLocation(this.id, "ProjMat");
            int mvLoc = GL30C.glGetUniformLocation(this.id, "ModelViewMat");
            System.out.println("[BO-GL] ProjMat loc=" + projLoc + " ModelViewMat loc=" + mvLoc);
        }

        int glMode = switch (drawState.mode()) {
            case LINES, DEBUG_LINES -> GL30C.GL_LINES;
            case LINE_STRIP, DEBUG_LINE_STRIP -> GL30C.GL_LINE_STRIP;
            case TRIANGLES -> GL30C.GL_TRIANGLES;
            case TRIANGLE_STRIP -> GL30C.GL_TRIANGLE_STRIP;
            case TRIANGLE_FAN -> GL30C.GL_TRIANGLE_FAN;
            case QUADS -> GL30C.GL_TRIANGLES;
        };

        if (hasIndices) {
            int indexType;
            int indexCount;
            if (generatedIntIndices) {
                indexType = GL30C.GL_UNSIGNED_INT;
                indexCount = drawState.vertexCount() / 4 * 6;
            } else {
                indexType = drawState.indexType() == VertexFormat.IndexType.SHORT ? GL30C.GL_UNSIGNED_SHORT : GL30C.GL_UNSIGNED_INT;
                indexCount = drawState.indexCount();
            }
            GL30C.glDrawElements(glMode, indexCount, indexType, 0);
        } else {
            GL30C.glDrawArrays(glMode, 0, drawState.vertexCount());
        }

        int glErr = GL30C.glGetError();
        if (glErr != 0 || !this.logged) {
            System.out.println("[BO] GL err after draw shader=" + this.name + " err=" + glErr);
        }

        this.unbind();
        GlStateManager._glUseProgram(this.currentShader[0]);

        // Restore GL state
        if (wasDepthTest) GL30C.glEnable(GL30C.GL_DEPTH_TEST);
        if (wasCullFace) GL30C.glEnable(GL30C.GL_CULL_FACE);
        if (!wasBlend) GL30C.glDisable(GL30C.GL_BLEND);
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
        GlStateManager._glUseProgram(this.id);
        this.uniformMap.forEach((name, uniform) -> uniform.upload());
    }

    public void unbind() {
        GlStateManager._glUseProgram(0);
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
