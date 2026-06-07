package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.util.render.CapeRenderContext;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerSkin.class)
public class MixinPlayerSkin {
    @Inject(method = "capeTexture", at = @At("HEAD"), cancellable = true)
    public void overrideCapeTexture(CallbackInfoReturnable<ResourceLocation> cir) {
        ResourceLocation custom = CapeRenderContext.get();
        if (custom != null) {
            cir.setReturnValue(custom);
        }
    }
}
