package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.misc.Streamer;
import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextVisitFactory.class)
public class MixinTextVisitFactory {
    @ModifyVariable(
            method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
            at = @At("HEAD"),
            argsOnly = true
    )
    private static String sus(String value) {
        Streamer streamer = Streamer.getInstance();
        return streamer.enabled ? streamer.replace(value) : value;
    }
}
