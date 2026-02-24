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
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(InGameHud.class)
public class MixinInGameHud {
    @Final
    @Shadow
    private static Identifier PUMPKIN_BLUR;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void preRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (Zoomify.getInstance().isCleanScreen()) {
            ci.cancel();
            return;
        }
        float tickDelta = tickCounter.getTickDelta(true);

        BlurSettings blur = BlurSettings.getInstance();
        if (Renderer.shouldLoad3DBlur()) {
            RenderUtils.loadBlur("3dblur", blur.get3DBlurStrength());
        }

        HandESP handESP = HandESP.getInstance();
        if (handESP.enabled) {
            handESP.renderHud();
        }

        ShaderESP shaderESP = ShaderESP.getInstance();
        if (shaderESP.enabled) {
            shaderESP.onRenderHud();
        }

        if (Renderer.shouldLoadHUDBlur()) {
            RenderUtils.loadBlur("hudblur", blur.getHUDBlurStrength());
        }

        BlackOut.EVENT_BUS.post(RenderEvent.Hud.Pre.get(context, tickDelta));
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void postRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        float tickDelta = tickCounter.getTickDelta(true);
        BlackOut.EVENT_BUS.post(RenderEvent.Hud.Post.get(context, tickDelta));
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void renderStatusEffectOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (NoRender.getInstance().enabled && NoRender.getInstance().effectOverlay.get()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void renderScoreboard(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        CustomScoreboard customScoreboard = CustomScoreboard.getInstance();
        if (customScoreboard.enabled) {
            ci.cancel();

            customScoreboard.objectiveName = objective.getDisplayName().getString();

            TextColor clr = objective.getDisplayName().getStyle().getColor();
            int rgbValue = (clr != null) ? clr.getRgb() : 0xFFFFFF;

            customScoreboard.objectiveColor = new Color(rgbValue);
        }
    }

    @Inject(
            method = "renderOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/util/Identifier;F)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void injectPumpkinBlur(DrawContext context, Identifier texture, float opacity, CallbackInfo callback) {
        if (NoRender.getInstance().enabled && NoRender.getInstance().pumpkin.get() && PUMPKIN_BLUR.equals(texture)) {
            callback.cancel();
        }
    }

    @Inject(
            method = "renderCrosshair",
            at = @At("HEAD"),
            cancellable = true
    )
    private void drawCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (Crosshair.getInstance().enabled) {
            ci.cancel();
        }
    }
}
