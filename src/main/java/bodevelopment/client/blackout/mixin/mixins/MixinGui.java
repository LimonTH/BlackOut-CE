package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.module.modules.client.BlurSettings;
import bodevelopment.client.blackout.module.modules.misc.Zoomify;
import bodevelopment.client.blackout.module.modules.visual.entities.ShaderESP;
import bodevelopment.client.blackout.module.modules.visual.misc.Crosshair;
import bodevelopment.client.blackout.module.modules.visual.misc.CustomScoreboard;
import bodevelopment.client.blackout.module.modules.visual.misc.HandESP;
import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.render.Render2DUtils;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.scores.Objective;

@Mixin(Gui.class)
public class MixinGui {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void preRender(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Zoomify.getInstance().isCleanScreen()) {
            ci.cancel();
            return;
        }
        float tickDelta = tickCounter.getGameTimeDeltaPartialTick(true);

        BlurSettings blur = BlurSettings.getInstance();
        if (Renderer.shouldLoad3DBlur()) {
            Render2DUtils.loadBlur("3dblur", blur.get3DBlurStrength());
        }

        HandESP handESP = HandESP.getInstance();
        if (handESP.enabled) handESP.renderHud();

        ShaderESP shaderESP = ShaderESP.getInstance();
        if (shaderESP.enabled) shaderESP.onRenderHud();

        if (Renderer.shouldLoadHUDBlur()) {
            Render2DUtils.loadBlur("hudblur", blur.getHUDBlurStrength());
        }

        BlackOut.EVENT_BUS.post(RenderEvent.Hud.Pre.get(context, tickDelta));
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void postRender(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        BlackOut.EVENT_BUS.post(RenderEvent.Hud.Post.get(context, tickCounter.getGameTimeDeltaPartialTick(true)));
    }

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void renderStatusEffectOverlay(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (NoRender.getInstance().enabled && NoRender.getInstance().effectOverlay.get()) ci.cancel();
    }

    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    private void onRenderVignette(GuiGraphics guiGraphics, Entity entity, CallbackInfo ci) {
        if (NoRender.getInstance().enabled && NoRender.getInstance().vignette.get()) ci.cancel();
    }

    @Inject(
            method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void renderScoreboard(GuiGraphics context, Objective objective, CallbackInfo ci) {
        CustomScoreboard customScoreboard = CustomScoreboard.getInstance();
        if (customScoreboard.enabled) {
            ci.cancel();
            customScoreboard.objectiveName = objective.getDisplayName().getString();
            TextColor clr = objective.getDisplayName().getStyle().getColor();
            customScoreboard.objectiveColor = new Color(clr != null ? clr.getValue() : 0xFFFFFF);
        }
    }

    @Inject(
            method = "renderTextureOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;F)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void injectOverlayFilter(GuiGraphics context, ResourceLocation texture, float opacity, CallbackInfo ci) {
        NoRender noRender = NoRender.getInstance();
        if (!noRender.enabled) return;

        ResourceLocation pumpkinTex = ResourceLocation.withDefaultNamespace("textures/misc/pumpkinblur.png");

        if (noRender.pumpkin.get()       && pumpkinTex.equals(texture)) { ci.cancel();
        }
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderPortal(GuiGraphics guiGraphics, float f, CallbackInfo ci) {
        if (NoRender.getInstance().enabled && NoRender.getInstance().portalOverlay.get()) ci.cancel();
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void drawCrosshair(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Crosshair.getInstance().enabled) ci.cancel();
    }
}