package bodevelopment.client.blackout.util.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;

public class ModelVertexConsumerProvider implements VertexConsumerProvider {
    public final ModelVertexConsumer consumer = new ModelVertexConsumer();

    public VertexConsumer getBuffer(RenderLayer layer) {
        return this.consumer;
    }
}
