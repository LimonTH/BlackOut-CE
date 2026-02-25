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

    public final Setting<ColorMode> colormode = this.sgGeneral.enumSetting("Color Logic", ColorMode.Custom, "The algorithmic style used to calculate the shader colors.");
    private final Setting<Double> saturation = this.sgGeneral.doubleSetting("Rainbow Intensity", 0.8, 0.0, 1.0, 0.1, "The color richness of the rainbow cycle.", () -> this.colormode.get() == ColorMode.Rainbow);
    private final Setting<Double> waveSpeed = this.sgGeneral.doubleSetting("Oscillation Speed", 2.0, 0.0, 10.0, 0.1, "How fast the colors transition in Wave mode.", () -> this.colormode.get() == ColorMode.Wave);
    private final Setting<Double> waveLength = this.sgGeneral.doubleSetting("Oscillation Scale", 2.0, 0.0, 5.0, 0.1, "The spatial frequency of the color wave.", () -> this.colormode.get() == ColorMode.Wave);
    private final Setting<BlackOutColor> waveColor = this.sgGeneral.colorSetting("Wave Secondary", new BlackOutColor(125, 125, 125, 255), "The secondary color used for wave interpolation.", () -> this.colormode.get() == ColorMode.Wave);
    private final Setting<Integer> dist = this.sgGeneral.intSetting("Bloom Radius", 5, 1, 10, 1, "The pixel width and intensity of the outer glow effect.");
    private final Setting<Boolean> texture = this.sgGeneral.booleanSetting("Overlay Texture", false, "Whether to blend the original hand texture with the interior shader color.");
    private final Setting<BlackOutColor> outsideColor = this.sgGeneral.colorSetting("Outline Palette", new BlackOutColor(255, 0, 0, 255), "The color of the glowing silhouette.");
    private final Setting<BlackOutColor> insideColor = this.sgGeneral.colorSetting("Interior Palette", new BlackOutColor(255, 0, 0, 50), "The color applied to the hand model itself.");

    public HandESP() {
        super("Hand ESP", "Applies post-processing shaders to the view-model (hands and items) to create glowing outlines or custom textures.", SubCategory.MISC_VISUAL, true);
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
