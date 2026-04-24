package bodevelopment.client.blackout.mixin.mixins;

import com.mojang.patchy.MojangBlockListSupplier;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(MojangBlockListSupplier.class)
abstract class MixinMcBlockedServerBypass {

	@Overwrite(remap = false)
	public Predicate<String> createBlockList() {
    return (address) -> address.equalsIgnoreCase("7b7t.net");
    // :trol:
	}
}
