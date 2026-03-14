package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.misc.Streamer;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(StringDecomposer.class)
public class MixinStringDecomposer {
    @ModifyVariable(
            method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
            at = @At("HEAD"),
            argsOnly = true
    )
    private static String sus(String value) {
        Streamer streamer = Streamer.getInstance();
        return streamer.enabled ? streamer.replace(value) : value;
    }
}
