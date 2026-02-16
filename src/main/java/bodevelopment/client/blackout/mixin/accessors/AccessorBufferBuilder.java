package bodevelopment.client.blackout.mixin.accessors;

import net.minecraft.client.render.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BufferBuilder.class)
public interface AccessorBufferBuilder {

    // Проверка, идет ли процесс записи прямо сейчас
    @Accessor("building")
    boolean isBuilding();

    // Проверка количества вершин (в 1.21.1 может называться чуть иначе в зависимости от маппингов)
    // Если это Yarn маппинги:
    @Accessor("vertexCount")
    int getVertexCount();
}