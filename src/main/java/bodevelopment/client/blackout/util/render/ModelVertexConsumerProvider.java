package bodevelopment.client.blackout.util.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public class ModelVertexConsumerProvider implements MultiBufferSource {
    public final ModelVertexConsumer consumer = new ModelVertexConsumer();

    public VertexConsumer getBuffer(RenderType layer) {
        return this.consumer;
    }
}
