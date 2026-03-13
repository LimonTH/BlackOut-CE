package bodevelopment.client.blackout.util.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;

public class ModelVertexConsumerProvider implements MultiBufferSource {
    public final ModelVertexConsumer consumer = new ModelVertexConsumer();

    @Override
    public @NotNull VertexConsumer getBuffer(RenderType layer) {
        return new ModelVertexConsumer.Wrapper(this.consumer);
    }
}
