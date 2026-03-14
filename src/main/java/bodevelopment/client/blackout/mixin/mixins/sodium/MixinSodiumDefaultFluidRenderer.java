package bodevelopment.client.blackout.mixin.mixins.sodium;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer", remap = false)
public class MixinSodiumDefaultFluidRenderer {
    @Final
    @Shadow
    private QuadLightData quadLightData;

    @Final
    @Shadow
    private float[] brightness;
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void onRender(LevelSlice level, BlockState blockState, FluidState fluidState, BlockPos blockPos, BlockPos offset, TranslucentGeometryCollector collector, ChunkModelBuilder meshBuilder, Material material, ColorProvider<FluidState> colorProvider, TextureAtlasSprite[] sprites, CallbackInfo ci) {
        if (XRay.getInstance().enabled && !XRay.getInstance().isTarget(blockState.getBlock())) {
            ci.cancel();
        }
    }

    @Inject(method = "updateQuad", at = @At("RETURN"))
    private void onUpdateQuadReturn(CallbackInfo ci) {
        if (XRay.getInstance().enabled) {
            Arrays.fill(this.brightness, 1.0f);

            for (int i = 0; i < 4; i++) {
                this.quadLightData.lm[i] = 0xF000F0;
                this.quadLightData.br[i] = 1.0f;
            }
        }
    }
}