package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public class MixinOptions {
    @Inject(method = "getCloudsType", at = @At("HEAD"), cancellable = true)
    private void onGetCloudsType(CallbackInfoReturnable<CloudStatus> cir) {
        NoRender noRender = NoRender.getInstance();
        if (noRender != null && noRender.enabled && noRender.clouds.get()) {
            cir.setReturnValue(CloudStatus.OFF);
        }
    }
}
