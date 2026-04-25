package bodevelopment.client.blackout.mixin.mixins;

import com.mojang.patchy.MojangBlockListSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import java.util.function.Predicate;

@Mixin(value = MojangBlockListSupplier.class, remap = false)
public class MixinMcBlockedServerBypass {
    @Overwrite
    public Predicate<String> createBlockList() {
        return (address) -> false;
    }
}