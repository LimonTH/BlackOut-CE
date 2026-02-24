package bodevelopment.client.blackout.module.setting.multisettings;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import bodevelopment.client.blackout.rendering.framebuffer.FrameBuffer;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.rendering.shader.Shaders;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.util.math.Box;

public class BoxMultiSetting {
    public static int id = 0;
    public final Setting<BoxRenderMode> mode;
    public final Setting<RenderShape> shape;
    public final Setting<BlackOutColor> lineColor;
    public final Setting<BlackOutColor> sideColor;
    public final Setting<Integer> bloom;
    public final Setting<Boolean> blur;
    public final Setting<BlackOutColor> insideColor;
    public final Setting<BlackOutColor> bloomColor;
    public final Setting<BlackOutColor> shaderOutlineColor;
    private final String insideBufferName;
    private final String bloomBufferName;
    private float shaderAlpha;

    private BoxMultiSetting(SettingGroup sg, String name, BlackOutColor defaultColor, SingleOut<Boolean> visible) {
        if (name == null) {
            name = "";
        } else {
            name = name + " ";
        }

        this.insideBufferName = "insideBuffer-" + id;
        this.bloomBufferName = "bloomBuffer-" + id;
        id++;

        this.mode = sg.enumSetting(name + "Render Mode", BoxRenderMode.Normal,
                "The rendering method: Normal (classic lines/sides) or Shader (modern glow/blur effects).", visible);
        this.shape = sg.enumSetting(name + "Shape", RenderShape.Full,
                "Which parts of the box to render: Sides, Outlines, or Both.");
        this.lineColor = sg.colorSetting(name + "Line Color", defaultColor.withAlpha(255),
                "The color of the box edges in Normal mode.",
                () -> this.mode.get() == BoxRenderMode.Normal && visible.get());
        this.sideColor = sg.colorSetting(name + "Side Color", defaultColor.withAlpha(50),
                "The color of the box faces in Normal mode.",
                () -> this.mode.get() == BoxRenderMode.Normal && visible.get());
        this.bloom = sg.intSetting(name + "Bloom", 3, 0, 10, 1,
                "The intensity of the glow effect around the box (Shader mode only).",
                () -> this.mode.get() == BoxRenderMode.Shader && visible.get());
        this.blur = sg.booleanSetting(name + "Blur", false,
                "Whether to blur the background behind the rendered box.",
                () -> this.mode.get() == BoxRenderMode.Shader && visible.get());
        this.insideColor = sg.colorSetting(name + "Inside Color", defaultColor.withAlpha(50),
                "The fill color used for the box faces in Shader mode.",
                () -> this.mode.get() == BoxRenderMode.Shader && visible.get());
        this.shaderOutlineColor = sg.colorSetting(name + "Outline Color", defaultColor.withAlpha(255),
                "The color of the edges in Shader mode.",
                () -> this.mode.get() == BoxRenderMode.Shader && this.shape.get().outlines && visible.get());
        this.bloomColor = sg.colorSetting(name + "Bloom Color", defaultColor.withAlpha(150),
                "The color of the glow/bloom aura surrounding the box.",
                () -> this.mode.get() == BoxRenderMode.Shader && visible.get());

        BlackOut.EVENT_BUS.subscribe(this, () -> BlackOut.mc.player == null || BlackOut.mc.world == null);
    }

    public static BoxMultiSetting of(SettingGroup sg) {
        return of(sg, null);
    }

    public static BoxMultiSetting of(SettingGroup sg, String name) {
        return of(sg, name, () -> true);
    }

    public static BoxMultiSetting of(SettingGroup sg, String name, SingleOut<Boolean> visible) {
        return new BoxMultiSetting(sg, name, new BlackOutColor(255, 0, 0, 255), visible);
    }

    @Event
    public void onRenderHud(RenderEvent.Hud.Pre event) {
        if (this.mode.get() == BoxRenderMode.Shader) {
            FrameBuffer insideBuffer = Managers.FRAME_BUFFER.getBuffer(this.insideBufferName);
            FrameBuffer bloomBuffer = Managers.FRAME_BUFFER.getBuffer(this.bloomBufferName);
            if (this.blur.get()) {
                float prevAlpha = Renderer.getAlpha();
                Renderer.setAlpha(this.shaderAlpha);
                Renderer.on3DBlur();
                Renderer.setTexture(Managers.FRAME_BUFFER.getBuffer("3dblur").getTexture(), 1);
                RenderUtils.renderBufferWith(insideBuffer, Shaders.screentexoverlay, new ShaderSetup(setup -> {
                    setup.set("uTexture0", 0);
                    setup.set("uTexture1", 1);
                }));
                Renderer.setAlpha(prevAlpha);
            }

            RenderUtils.renderBufferWith(
                    insideBuffer, Shaders.shaderbloom, new ShaderSetup(setup -> setup.color("clr", this.insideColor.get().alphaMulti(this.shaderAlpha).getRGB()))
            );
            if (this.bloom.get() > 0) {
                bloomBuffer.clear(0.0F, 0.0F, 0.0F, 1.0F);
                bloomBuffer.bind(true);
                RenderUtils.renderBufferWith(insideBuffer, Shaders.screentex, new ShaderSetup(setup -> setup.set("alpha", 1.0F)));
                bloomBuffer.unbind();
                RenderUtils.blurBufferBW(this.bloomBufferName, this.bloom.get() + 1);
                bloomBuffer.bind(true);
                Renderer.setTexture(insideBuffer.getTexture(), 1);
                RenderUtils.renderBufferWith(bloomBuffer, Shaders.subtract, new ShaderSetup(setup -> {
                    setup.set("uTexture0", 0);
                    setup.set("uTexture1", 1);
                }));
                bloomBuffer.unbind();
                RenderUtils.renderBufferWith(
                        bloomBuffer, Shaders.shaderbloom, new ShaderSetup(setup -> setup.color("clr", this.bloomColor.get().alphaMulti(this.shaderAlpha).getRGB()))
                );
            }

            insideBuffer.clear(0.0F, 0.0F, 0.0F, 1.0F);
        }
    }

    public void render(Box box) {
        this.render(box, 1.0F, 1.0F);
    }

    public void render(Box box, float alpha, float alphaS) {
        switch (this.mode.get()) {
            case Normal:
                Render3DUtils.box(box, this.sideColor.get().alphaMulti(alpha), this.lineColor.get().alphaMulti(alpha), this.shape.get());
                break;
            case Shader:
                this.shaderAlpha = alphaS;
                FrameBuffer insideBuffer = Managers.FRAME_BUFFER.getBuffer(this.insideBufferName);
                insideBuffer.bind(true);
                if (this.shape.get().sides) {
                    Render3DUtils.box(box, BlackOutColor.WHITE, null, RenderShape.Sides);
                }
                insideBuffer.unbind();
                if (this.shape.get().outlines) {
                    Render3DUtils.box(box, null, this.shaderOutlineColor.get().alphaMulti(alphaS), RenderShape.Outlines);
                }
        }
    }

    public enum BoxRenderMode {
        Normal,
        Shader
    }
}
