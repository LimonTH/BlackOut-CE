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
import org.spongepowered.asm.mixin.Unique;
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

    @Final
    @Shadow
    private int[] quadColors;

    @Unique
    private static final ThreadLocal<Integer> XRAY_FLUID_ALPHA = ThreadLocal.withInitial(() -> -1);

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onRenderHead(
            LevelSlice level,
            BlockState blockState,
            FluidState fluidState,
            BlockPos blockPos,
            BlockPos offset,
            TranslucentGeometryCollector collector,
            ChunkModelBuilder meshBuilder,
            Material material,
            ColorProvider<FluidState> colorProvider,
            TextureAtlasSprite[] sprites,
            CallbackInfo ci
    ) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) { XRAY_FLUID_ALPHA.set(-1); return; }
        if (xray.isTarget(blockState.getBlock())) { XRAY_FLUID_ALPHA.set(-1); return; }

        final int opacity = xray.opacity.get();
        if (opacity <= 0) {
            ci.cancel();
            return;
        }
        XRAY_FLUID_ALPHA.set(opacity < 255 ? opacity : -1);
    }

    @Inject(method = "render", at = @At("RETURN"), remap = false, require = 0)
    private void onRenderReturn(CallbackInfo ci) {
        XRAY_FLUID_ALPHA.set(-1);
    }

    @Inject(method = "updateQuad", at = @At("RETURN"), remap = false, require = 0)
    private void onUpdateQuadReturn(CallbackInfo ci) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) return;

        // Максимальная яркость для target-блоков рядом с жидкостью (оригинальная логика)
        Arrays.fill(this.brightness, 1.0f);
        for (int i = 0; i < 4; i++) {
            this.quadLightData.lm[i] = 0xF000F0;
            this.quadLightData.br[i] = 1.0f;
        }

        // Применяем alpha для non-target жидкостей
        final int alpha = XRAY_FLUID_ALPHA.get();
        if (alpha < 0) return;

        final int alphaShifted = alpha << 24;
        for (int i = 0; i < 4; i++) {
            this.quadColors[i] = (this.quadColors[i] & 0x00FFFFFF) | alphaShifted;
        }
    }
}