package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import net.minecraft.client.render.block.BlockModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(BlockModelRenderer.class)
public class MixinBlockModelRenderer {
    @ModifyVariable(method = "render*", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int boostLight(int light) {
        XRay xray = XRay.getInstance();
        return (xray != null && xray.enabled) ? 15728880 : light;
    }

    @ModifyArgs(
            method = "renderQuad",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/VertexConsumer;quad(Lnet/minecraft/client/util/math/MatrixStack$Entry;Lnet/minecraft/client/render/model/BakedQuad;[FFFFF[IIZ)V"
            )
    )

    private void onModifyQuadAlpha(Args args) {
        XRay xray = XRay.getInstance();
        if (xray != null && xray.enabled) {
            applyXrayOpacity(args, 6);
        }
    }

    @ModifyArgs(
            method = "renderQuads",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/VertexConsumer;quad(Lnet/minecraft/client/util/math/MatrixStack$Entry;Lnet/minecraft/client/render/model/BakedQuad;FFFFII)V"
            )
    )
    private static void onModifyQuadsStaticAlpha(Args args) {
        XRay xray = XRay.getInstance();
        if (xray != null && xray.enabled) {
            // applyXrayOpacity(args, 5);
        }
    }

    @Unique
    private static void applyXrayOpacity(Args args, int index) {
        XRay xray = XRay.getInstance();
        if (xray != null && xray.enabled) {
            // float alpha = xray.opacity.get() / 255.0f;
            // args.set(index, alpha);
        }
    }
}