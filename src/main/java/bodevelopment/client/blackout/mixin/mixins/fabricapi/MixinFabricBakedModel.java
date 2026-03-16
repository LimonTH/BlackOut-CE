package bodevelopment.client.blackout.mixin.mixins.fabricapi;

import java.util.function.Predicate;
import java.util.function.Supplier;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FabricBakedModel.class)
public interface MixinFabricBakedModel {
    @Inject(method = "emitBlockQuads", at = @At("HEAD"), cancellable = true)
    default void onEmitBlockQuadsHead(
            QuadEmitter emitter,
            BlockAndTintGetter blockView,
            BlockState state,
            BlockPos pos,
            Supplier<RandomSource> randomSupplier,
            Predicate<@Nullable Direction> cullTest,
            CallbackInfo ci
    ) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled || state == null) return;
        if (xray.isTarget(state.getBlock())) return;

        final int opacity = xray.opacity.get();
        if (opacity <= 0) {
            ci.cancel();
            return;
        }
        if (opacity >= 255) return;

        emitter.pushTransform(quad -> {
            for (int v = 0; v < 4; v++) {
                final int color = quad.color(v);
                quad.color(v, (color & 0x00FFFFFF) | (opacity << 24));
                quad.lightmap(v, 15728880);
            }
            return true;
        });
    }

    @Inject(method = "emitBlockQuads", at = @At("RETURN"))
    default void onEmitBlockQuadsReturn(
            QuadEmitter emitter,
            BlockAndTintGetter blockView,
            BlockState state,
            BlockPos pos,
            Supplier<RandomSource> randomSupplier,
            Predicate<@Nullable Direction> cullTest,
            CallbackInfo ci
    ) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled || state == null) return;
        if (xray.isTarget(state.getBlock())) return;

        final int opacity = xray.opacity.get();
        if (opacity <= 0 || opacity >= 255) return;

        emitter.popTransform();
    }
}
