package bodevelopment.client.blackout.mixin.mixins;

import net.minecraft.client.multiplayer.resolver.AddressCheck;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(AddressCheck.class)
public interface MixinAddressCheck {

    @Overwrite
    default boolean isAllowed(ResolvedServerAddress resolvedServerAddress) {
        return true;
    }

    @Overwrite
    default boolean isAllowed(ServerAddress serverAddress) {
        return true;
    }
}