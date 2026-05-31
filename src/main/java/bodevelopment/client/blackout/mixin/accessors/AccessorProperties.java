package bodevelopment.client.blackout.mixin.accessors;

import bodevelopment.client.blackout.annotations.Internal;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockBehaviour.Properties.class)
@Internal
public interface AccessorProperties {
    @Accessor("replaceable")
    boolean replaceable();
}
