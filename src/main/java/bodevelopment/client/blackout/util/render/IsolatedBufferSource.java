package bodevelopment.client.blackout.util.render;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class IsolatedBufferSource extends MultiBufferSource.BufferSource {
    private final Map<RenderType, MultiBufferSource.BufferSource> perTypeSources = new HashMap<>();

    public IsolatedBufferSource() {
        super(new ByteBufferBuilder(256), new LinkedHashMap<>());
    }

    @Override
    public @NotNull VertexConsumer getBuffer(RenderType renderType) {
        return perTypeSources.computeIfAbsent(renderType,
                t -> MultiBufferSource.immediate(new ByteBufferBuilder(4096)))
                .getBuffer(renderType);
    }

    @Override
    public void endBatch() {
        perTypeSources.values().forEach(MultiBufferSource.BufferSource::endBatch);
        perTypeSources.clear();
    }

    @Override
    public void endBatch(RenderType renderType) {
        MultiBufferSource.BufferSource source = perTypeSources.get(renderType);
        if (source != null) {
            source.endBatch(renderType);
        }
    }
}
