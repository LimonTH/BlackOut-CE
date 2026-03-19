package bodevelopment.client.blackout.mixin.mixins.sodium;

import bodevelopment.client.blackout.module.modules.visual.misc.XRay;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
public class MixinSodiumBlockRenderer {
    @Final
    @Shadow
    private ChunkVertexEncoder.Vertex[] vertices;

    @Unique
    private static final ThreadLocal<Integer> XRAY_ALPHA = ThreadLocal.withInitial(() -> -1);

    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onRenderModelHead(
            BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci
    ) {
        XRay xray = XRay.getInstance();
        if (xray == null || !xray.enabled) { XRAY_ALPHA.set(-1); return; }
        if (xray.isTarget(state.getBlock())) { XRAY_ALPHA.set(-1); return; }

        final int opacity = xray.opacity.get();
        if (opacity <= 0) { ci.cancel(); return; }
        XRAY_ALPHA.set(opacity < 255 ? opacity : -1);
    }

    @Inject(method = "renderModel", at = @At("RETURN"), remap = false, require = 0)
    private void onRenderModelReturn(CallbackInfo ci) {
        XRAY_ALPHA.set(-1);
    }

    @Inject(
            method = "bufferQuad",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/texture/SpriteFinderCache;forBlockAtlas()Lnet/fabricmc/fabric/api/renderer/v1/model/SpriteFinder;"
            ),
            remap = false,
            require = 0
    )
    private void onBufferQuadModifyAlpha(CallbackInfo ci) {
        final int alpha = XRAY_ALPHA.get();
        if (alpha < 0) return;

        final int alphaShifted = alpha << 24;
        final ChunkVertexEncoder.Vertex[] verts = this.vertices;
        for (int i = 0; i < 4; i++) {
            verts[i].color = (verts[i].color & 0x00FFFFFF) | alphaShifted;
        }
    }

    @ModifyVariable(
            method = "bufferQuad",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildBuffers;get(Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;"
            ),
            remap = false,
            require = 0,
            name = "pass")
    private TerrainRenderPass modifyRenderPass(TerrainRenderPass pass) {
        final int alpha = XRAY_ALPHA.get();
        if (alpha >= 0 && alpha < 255) {
            return DefaultTerrainRenderPasses.TRANSLUCENT;
        }
        return pass;
    }
}