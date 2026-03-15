package bodevelopment.client.blackout.mixin.mixins.sodium;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache", remap = false)
public class MixinSodiumBlockOcclusionCache {
    @Inject(
            method = "shouldDrawSide",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void onShouldDrawSide(
            BlockState selfState,
            BlockGetter view,
            BlockPos selfPos,
            Direction facing,
            CallbackInfoReturnable<Boolean> cir
    ) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;
        if (xray.isTarget(selfState.getBlock())) {
            cir.setReturnValue(true);
        }
    }
}