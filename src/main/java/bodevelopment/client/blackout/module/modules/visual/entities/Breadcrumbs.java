package bodevelopment.client.blackout.module.modules.visual.entities;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.GameJoinEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.timers.RenderList;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

public class Breadcrumbs extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgColor = this.addGroup("Color");

    private final Setting<Boolean> onlyMoving = this.sgGeneral.booleanSetting("Motion Filtering", true, "Prevents the accumulation of particles while the player is stationary.");
    private final Setting<Double> size = this.sgGeneral.doubleSetting("Particle Scale", 3.0, 1.0, 10.0, 0.1, "The diameter of the rendered trail dots.");
    private final Setting<Double> delay = this.sgGeneral.doubleSetting("Sampling Interval", 0.1, 0.0, 3.0, 0.01, "The time frequency (in seconds) for dropping a new position marker.");
    private final Setting<Double> renderTime = this.sgGeneral.doubleSetting("Persistence Duration", 3.0, 0.001, 20.0, 0.05, "The total lifespan of each particle before it is culled from the buffer.");

    private final Setting<BlackOutColor> clr = this.sgColor.colorSetting("Core Color", new BlackOutColor(255, 255, 255, 100), "The primary color applied to the particle center.");
    private final Setting<BlackOutColor> clr1 = this.sgColor.colorSetting("Aura Color", new BlackOutColor(175, 175, 175, 100), "The secondary color applied to the particle boundary.");
    public final Setting<ColorMode> colorMode = this.sgColor.enumSetting("Chroma Mode", ColorMode.Custom, "The color distribution algorithm used for the trail particles.");
    private final Setting<Double> saturation = this.sgColor.doubleSetting("Rainbow Saturation", 0.8, 0.0, 1.0, 0.1, "The intensity of the color palette when using Rainbow mode.", () -> this.colorMode.get() == ColorMode.Rainbow);
    private final Setting<Integer> iAlpha = this.sgColor.intSetting("Core Opacity", 150, 0, 255, 1, "The transparency level of the inner particle dot.", () -> this.colorMode.get() == ColorMode.Rainbow);
    private final Setting<Integer> oAlpha = this.sgColor.intSetting("Aura Opacity", 50, 0, 255, 1, "The transparency level of the outer particle glow.", () -> this.colorMode.get() == ColorMode.Rainbow);

    private final MatrixStack stack = new MatrixStack();
    private final RenderList<Vec3d> list = RenderList.getList(true);
    private long lastAddition = System.currentTimeMillis();

    public Breadcrumbs() {
        super("Breadcrumbs", "Visualizes the player's historical trajectory by rendering a persistent trail of color-coded particles.", SubCategory.ENTITIES, true);
    }

    @Event
    public void onGameJoin(GameJoinEvent event) {
        this.list.clear();
    }

    @Event
    public void onRender(RenderEvent.Hud.Post event) {
        if (BlackOut.mc.world != null && BlackOut.mc.player != null) {
            if (this.lastAddition + this.delay.get() * 1000.0 < System.currentTimeMillis()) {
                this.addDot(event.tickDelta, BlackOut.mc.player);
                this.lastAddition = System.currentTimeMillis();
            }

            this.stack.push();
            RenderUtils.unGuiScale(this.stack);
            this.list.update((pos, time, d) -> this.drawDot(this.stack, pos, d));
            this.stack.pop();
        }
    }

    private void addDot(double tickDelta, Entity entity) {
        if (!this.onlyMoving.get()
                || entity.prevX != entity.getX()
                || entity.prevY != entity.getY()
                || entity.prevZ != entity.getZ()) {
            double x = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
            double y = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
            double z = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());
            this.list.add(new Vec3d(x, y, z), this.renderTime.get());
        }
    }

    private void drawDot(MatrixStack stack, Vec3d vec, double delta) {
        Vec2f f = RenderUtils.getCoords(vec.x, vec.y, vec.z, true);
        if (f != null) {
            Color[] colors = this.getColors();
            Color color1 = colors[0];
            Color color2 = colors[1];
            float alpha = (float) (1.0 - delta);
            float prevAlpha = Renderer.getAlpha();
            Renderer.setAlpha(alpha);
            float s = this.size.get().floatValue();
            RenderUtils.rounded(stack, f.x, f.y, 0.0F, 0.0F, s * 2.0F, s * 2.0F, color2.getRGB(), color2.getRGB());
            RenderUtils.rounded(stack, f.x, f.y, 0.0F, 0.0F, s, s, color1.getRGB(), color1.getRGB());
            Renderer.setAlpha(prevAlpha);
        }
    }

    private Color[] getColors() {
        Color[] colors = new Color[2];
        switch (this.colorMode.get()) {
            case Custom:
                colors[0] = this.clr.get().getColor();
                colors[1] = this.clr1.get().getColor();
                break;
            case Rainbow:
                int rainbowColor = ColorUtils.getRainbow(4.0F, this.saturation.get().floatValue(), 1.0F, 150L);
                colors[0] = new Color(rainbowColor >> 16 & 0xFF, rainbowColor >> 8 & 0xFF, rainbowColor & 0xFF, this.iAlpha.get());
                rainbowColor = ColorUtils.getRainbow(4.0F, this.saturation.get().floatValue(), 1.0F, 300L);
                colors[1] = new Color(rainbowColor >> 16 & 0xFF, rainbowColor >> 8 & 0xFF, rainbowColor & 0xFF, this.oAlpha.get());
                break;
            case Wave:
                colors[0] = ColorUtils.getWave(this.clr.get().getColor(), this.clr1.get().getColor(), 1.0, 1.0, 1);
                colors[1] = ColorUtils.getWave(this.clr.get().getColor(), this.clr1.get().getColor(), 1.0, 1.0, 1);
        }

        return colors;
    }

    public enum ColorMode {
        Rainbow,
        Custom,
        Wave
    }
}