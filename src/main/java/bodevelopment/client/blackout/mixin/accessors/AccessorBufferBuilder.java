package bodevelopment.client.blackout.mixin.accessors;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BufferBuilder.class)
public interface AccessorBufferBuilder {

    @Accessor("building")
    boolean isBuilding();

    @Accessor("vertices")
    int getVertexCount();
}