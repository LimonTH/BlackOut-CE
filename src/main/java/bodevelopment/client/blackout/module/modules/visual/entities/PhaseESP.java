package bodevelopment.client.blackout.module.modules.visual.entities;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.misc.AntiBot;
import bodevelopment.client.blackout.module.modules.visual.misc.FreeCam;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.BlockUtils;
import bodevelopment.client.blackout.util.render.AnimUtils;
import bodevelopment.client.blackout.util.render.Render2DUtils;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;

public class PhaseESP extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgColor = this.addGroup("Color");

    public final Setting<Boolean> bg = this.sgGeneral.booleanSetting("Background Plate", true, "Renders a background panel behind the phase indicator text.");
    public final Setting<Boolean> rounded = this.sgGeneral.booleanSetting("Curved Geometry", true, "Applies rounding to the corners of the background plate.", this.bg::get);
    public final Setting<Boolean> shadow = this.sgGeneral.booleanSetting("Drop Shadow", true, "Renders a soft shadow behind the plate to improve visual depth.", this.bg::get);
    private final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Gaussian Blur", true, "Applies a blur effect behind the tag to enhance legibility against complex terrain.", this.bg::get);
    private final Setting<String> infoText = this.sgGeneral.stringSetting("Indicator Label", "Phased", "The custom text to display when a player is detected inside blocks.");
    private final Setting<Double> scale = this.sgGeneral.doubleSetting("Base Scale", 1.0, 0.0, 10.0, 0.1, "The primary size of the indicator overlay.");
    private final Setting<Double> scaleInc = this.sgGeneral.doubleSetting("Distance Compensation", 1.0, 0.0, 5.0, 0.05, "Dynamically increases the label scale as the distance to the target increases.");
    private final Setting<Double> yOffset = this.sgGeneral.doubleSetting("Vertical Translation", 0.0, 0.0, 1.0, 0.01, "Adjusts the vertical placement of the indicator relative to the entity's position.");

    private final Setting<BlackOutColor> bgClose = this.sgColor.colorSetting("Proximal Background", new BlackOutColor(8, 8, 8, 120), "The background color applied when the target is at close range.", this.bg::get);
    private final Setting<BlackOutColor> bgFar = this.sgColor.colorSetting("Distal Background", new BlackOutColor(0, 0, 0, 120), "The background color applied when the target is at maximum render distance.", this.bg::get);
    private final Setting<BlackOutColor> shdwClose = this.sgColor.colorSetting("Proximal Shadow", new BlackOutColor(8, 8, 8, 100), "The shadow color applied when the target is at close range.", this.bg::get);
    private final Setting<BlackOutColor> shdwFar = this.sgColor.colorSetting("Distal Shadow", new BlackOutColor(0, 0, 0, 100), "The shadow color applied when the target is at maximum render distance.", this.bg::get);
    private final Setting<BlackOutColor> txt = this.sgColor.colorSetting("Label Color", new BlackOutColor(255, 255, 255, 255), "The color of the indicator text.");

    private final List<Entity> players = new ArrayList<>();
    private final PoseStack stack = new PoseStack();

    public PhaseESP() {
        super("Phase ESP", "Displays specialized indicators over players who are currently intersecting with solid blocks or 'phasing'.", SubCategory.ENTITIES, true);
    }

    private String getText() {
        String dn = this.infoText.get();
        return dn.isEmpty() ? "Phased" : dn;
    }

    @Event
    public void onTick(TickEvent.Post event) {
        if (BlackOut.mc.level != null && BlackOut.mc.player != null) {
            this.players.clear();
            BlackOut.mc.level.tickingEntities.forEach(entity -> {
                if (this.shouldRender(entity)) {
                    this.players.add(entity);
                }
            });
            this.players.sort(Comparator.comparingDouble(entity -> -BlackOut.mc.gameRenderer.getMainCamera().getPosition().distanceTo(entity.position())));
        }
    }

    public void renderNameTag(double tickDelta, Entity entity) {
        double x = Mth.lerp(tickDelta, entity.xo, entity.getX());
        double y = Mth.lerp(tickDelta, entity.yo, entity.getY());
        double z = Mth.lerp(tickDelta, entity.zo, entity.getZ());
        float d = (float) BlackOut.mc.gameRenderer.getMainCamera().getPosition().subtract(x, y, z).length();
        float s = this.getScale(d);
        this.stack.pushPose();
        Vec2 f = Render2DUtils.getCoords(x, y - this.yOffset.get(), z, true);
        if (f == null) {
            this.stack.popPose();
        } else {
            this.stack.translate(f.x, f.y, 0.0F);
            this.stack.scale(s, s, s);
            String text = this.getText();
            float length = BlackOut.FONT.getWidth(text);
            this.stack.pushPose();
            this.stack.translate(-length / 2.0F, -9.0F, 0.0F);
            double easedValue = AnimUtils.easeOutQuint(Mth.clamp(d / 100.0, 0.0, 1.0));
            Color color = ColorUtils.lerpColor(easedValue, this.bgClose.get().getColor(), this.bgFar.get().getColor());
            Color shadowColor = ColorUtils.lerpColor(easedValue, this.shdwClose.get().getColor(), this.shdwFar.get().getColor());
            if (this.bg.get()) {
                if (this.blur.get()) {
                    Render2DUtils.drawLoadedBlur(
                            "hudblur", this.stack, renderer -> renderer.rounded(-2.0F, -5.0F, length + 4.0F, 10.0F, this.rounded.get() ? 3.0F : 0.0F, 10)
                    );
                    Renderer.onHUDBlur();
                }

                Render2DUtils.rounded(
                        this.stack,
                        -2.0F,
                        -5.0F,
                        length + 4.0F,
                        10.0F,
                        this.rounded.get() ? 3.0F : 0.0F,
                        this.shadow.get() ? 3.0F : 0.0F,
                        color.getRGB(),
                        shadowColor.getRGB()
                );
            }

            BlackOut.FONT.text(this.stack, text, 1.0F, 0.0F, 0.0F, this.txt.get().getColor(), false, true);
            this.stack.popPose();
            this.stack.popPose();
        }
    }

    public boolean shouldRender(Entity entity) {
        if (!(entity instanceof Player)) {
            return false;
        } else {
            AntiBot antiBot = AntiBot.getInstance();
            if (antiBot.enabled && antiBot.mode.get() == AntiBot.HandlingMode.Ignore && entity instanceof AbstractClientPlayer && antiBot.getBots().contains(entity)) {
                return false;
            } else if (!BlockUtils.hasEntityCollision(entity, entity.getBoundingBox().deflate(0.04, 0.06, 0.04))) {
                return false;
            } else {
                return entity != BlackOut.mc.player || FreeCam.getInstance().enabled;
            }
        }
    }

    @Event
    public void onRender(RenderEvent.Hud.Post event) {
        if (BlackOut.mc.level != null && BlackOut.mc.player != null) {
            GlStateManager._disableDepthTest();
            GlStateManager._enableBlend();
            GlStateManager._disableCull();
            this.stack.pushPose();
            Render2DUtils.unGuiScale(this.stack);
            this.players.forEach(entity -> this.renderNameTag(event.tickDelta, entity));
            this.stack.popPose();
        }
    }

    private float getScale(float d) {
        float distSqrt = (float) Math.sqrt(d);
        return this.scale.get().floatValue() * 8.0F / distSqrt + this.scaleInc.get().floatValue() / 20.0F * distSqrt;
    }
}
