package bodevelopment.client.blackout.mixin.accessors;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockBehaviour.Properties.class)
public interface AccessorBlockSettings {
    @Accessor("replaceable")
    boolean replaceable();
}
