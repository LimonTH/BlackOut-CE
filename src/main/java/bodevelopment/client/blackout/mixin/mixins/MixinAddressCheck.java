package bodevelopment.client.blackout.mixin.mixins;

import net.minecraft.client.multiplayer.resolver.AddressCheck;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerNameResolver.class)
public class MixinAddressCheck {

    @Redirect(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/resolver/AddressCheck;createFromService()Lnet/minecraft/client/multiplayer/resolver/AddressCheck;"
            )
    )
    private static AddressCheck overrideAddressCheck() {
        return new AddressCheck() {
            @Override
            public boolean isAllowed(ResolvedServerAddress resolvedServerAddress) {
                return true;
            }

            @Override
            public boolean isAllowed(ServerAddress serverAddress) {
                return true;
            }
        };
    }
}