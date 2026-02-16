package bodevelopment.client.blackout.mixin.accessors;

import net.minecraft.block.AbstractBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractBlock.Settings.class)
public interface AccessorBlockSettings {
    @Accessor("replaceable")
    boolean replaceable();
}
