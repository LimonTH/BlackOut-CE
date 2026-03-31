package bodevelopment.client.blackout.util.render.misc;

import bodevelopment.client.blackout.rendering.framebuffer.FrameBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class FramebufferMultiBufferSource implements MultiBufferSource {
    private final Map<RenderType, ByteBufferBuilder> byteBuilders = new LinkedHashMap<>();
    private final Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();

    @Override
    public @NotNull VertexConsumer getBuffer(RenderType type) {
        BufferBuilder existing = builders.get(type);
        if (existing != null) return existing;

        ByteBufferBuilder bbb = new ByteBufferBuilder(8192);
        byteBuilders.put(type, bbb);
        BufferBuilder builder = new BufferBuilder(bbb, type.mode(), type.format());
        builders.put(type, builder);
        return builder;
    }

    public void drawToFramebuffer(FrameBuffer fbo) {
        for (var entry : builders.entrySet()) {
            RenderType type = entry.getKey();
            BufferBuilder builder = entry.getValue();

            MeshData mesh = builder.build();
            if (mesh != null) {
                type.setupRenderState();
                fbo.bind(false);
                BufferUploader.drawWithShader(mesh);
                type.clearRenderState();
            }
        }
        for (ByteBufferBuilder bbb : byteBuilders.values()) {
            bbb.close();
        }
        builders.clear();
        byteBuilders.clear();
    }
}
