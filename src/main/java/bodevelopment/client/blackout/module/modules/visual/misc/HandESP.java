package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import bodevelopment.client.blackout.rendering.framebuffer.BlendFrameBuffer;
import bodevelopment.client.blackout.rendering.framebuffer.FrameBuffer;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.rendering.shader.Shaders;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;

import java.awt.*;

public class HandESP extends Module {
    private static HandESP INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<ColorMode> colormode = this.sgGeneral.enumSetting("Mode", ColorMode.Custom, "What style to use");
    private final Setting<Double> saturation = this.sgGeneral
            .doubleSetting("Rainbow Saturation", 0.8, 0.0, 1.0, 0.1, ".", () -> this.colormode.get() == ColorMode.Rainbow);
    private final Setting<Double> waveSpeed = this.sgGeneral
            .doubleSetting("Wave Speed", 2.0, 0.0, 10.0, 0.1, "Slower wave effect", () -> this.colormode.get() == ColorMode.Wave);
    private final Setting<Double> waveLength = this.sgGeneral
            .doubleSetting("Wave Length", 2.0, 0.0, 5.0, 0.1, "Longer wave effect", () -> this.colormode.get() == ColorMode.Wave);
    private final Setting<BlackOutColor> waveColor = this.sgGeneral
            .colorSetting("Wave Color", new BlackOutColor(125, 125, 125, 255), "Text Color For The Wave", () -> this.colormode.get() == ColorMode.Wave);
    private final Setting<Integer> dist = this.sgGeneral.intSetting("Distance", 5, 1, 10, 1, ".");
    private final Setting<Boolean> texture = this.sgGeneral.booleanSetting("Texture", false, ".");
    private final Setting<BlackOutColor> outsideColor = this.sgGeneral.colorSetting("Outside Color", new BlackOutColor(255, 0, 0, 255), ".");
    private final Setting<BlackOutColor> insideColor = this.sgGeneral.colorSetting("Inside Color", new BlackOutColor(255, 0, 0, 50), ".");

    public HandESP() {
        super("Hand ESP", "Modifies how hands are rendered.", SubCategory.MISC_VISUAL, true);
        INSTANCE = this;
    }

    public static HandESP getInstance() {
        return INSTANCE;
    }

    public void draw(Runnable runnable) {
        if (!this.enabled) {
            runnable.run();
        } else {
            BlendFrameBuffer buffer = Managers.FRAME_BUFFER.getBlend("handESP");
            buffer.start();
            runnable.run();
            buffer.unbind();
        }
    }

    public void renderHud() {
        FrameBuffer buffer = Managers.FRAME_BUFFER.getBuffer("handESP");
        FrameBuffer bloomBuffer = Managers.FRAME_BUFFER.getBuffer("handESP-bloom");
        if (this.texture.get()) {
            RenderUtils.renderBufferWith(buffer, Shaders.screentexcolor, new ShaderSetup(setup -> setup.color("clr", this.insideColor.get().getRGB())));
        }

        buffer.bind(true);
        RenderUtils.renderBufferWith(buffer, Shaders.convert, new ShaderSetup());
        buffer.unbind();
        if (this.dist.get() > 0) {
            bloomBuffer.clear(0.0F, 0.0F, 0.0F, 1.0F);
            bloomBuffer.bind(true);
            RenderUtils.renderBufferWith(buffer, Shaders.screentex, new ShaderSetup(setup -> setup.set("alpha", 1.0F)));
            bloomBuffer.unbind();
            RenderUtils.blurBufferBW("handESP-bloom", this.dist.get() + 1);
            bloomBuffer.bind(true);
            Renderer.setTexture(buffer.getTexture(), 1);
            RenderUtils.renderBufferWith(bloomBuffer, Shaders.subtract, new ShaderSetup(setup -> {
                setup.set("uTexture0", 0);
                setup.set("uTexture1", 1);
            }));
            bloomBuffer.unbind();
            RenderUtils.renderBufferWith(bloomBuffer, Shaders.shaderbloom, new ShaderSetup(setup -> setup.color("clr", this.getColor(false))));
            buffer.clear(1.0F, 1.0F, 1.0F, 0.0F);
        }
    }

    private int getColor(boolean inside) {
        return switch (this.colormode.get()) {
            case Custom -> inside ? this.insideColor.get().getRGB() : this.outsideColor.get().getRGB();
            case Rainbow -> this.getRainbowColor(inside ? this.insideColor.get().alpha : this.outsideColor.get().alpha);
            case Wave -> ColorUtils.getWave(
                            inside ? this.insideColor.get().getColor() : this.outsideColor.get().getColor(),
                            this.waveColor.get().getColor(),
                            this.waveSpeed.get(),
                            this.waveLength.get(),
                            1
                    )
                    .getRGB();
        };
    }

    private int getRainbowColor(int alpha) {
        Color color = new Color(ColorUtils.getRainbow(4.0F, this.saturation.get().floatValue(), 1.0F, 150L));
        color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        return color.getRGB();
    }

    public enum ColorMode {
        Rainbow,
        Custom,
        Wave
    }
}
