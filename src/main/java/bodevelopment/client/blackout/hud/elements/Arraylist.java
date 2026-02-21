package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.FilterMode;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.ParentCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import bodevelopment.client.blackout.rendering.framebuffer.FrameBuffer;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.rendering.shader.Shaders;
import bodevelopment.client.blackout.util.render.AnimUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import org.apache.commons.lang3.mutable.MutableFloat;

import java.awt.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class Arraylist extends HudElement {
    public static Map<Module, MutableFloat> deltaMap = new HashMap<>();
    public final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<BracketMode> brackets = this.sgGeneral.e("Bracket Style", BracketMode.None, ".");
    private final Setting<FilterMode> filterMode = this.sgGeneral.e("Filter Mode", FilterMode.Blacklist, ".");
    private final Setting<List<Module>> moduleList = this.sgGeneral
            .l("Modules", "Only renders these modules.", Managers.MODULES.getModules(), module -> module.name);
    private final Setting<Boolean> drawInfo = this.sgGeneral.b("Show Info", true, ".");
    private final Setting<Boolean> rounded = this.sgGeneral.b("Rounded", true, "Renders a rounded background (cool af)");
    private final Setting<Boolean> sideBar = this.sgGeneral.b("Side bar", true, "Renders a sidebar", () -> !this.rounded.get());
    private final Setting<Boolean> bg = this.sgGeneral.b("Background", true, "Renders a background");
    private final Setting<BlackOutColor> bgColor = this.sgGeneral.c("Background Color", new BlackOutColor(0, 0, 0, 50), ".", this.bg::get);
    private final Setting<Boolean> useBlur = this.sgGeneral.b("Blur", true, "Uses a blur effect", () -> true);
    private final Setting<Integer> bloomIntensity = this.sgGeneral.i("Bloom Intensity", 3, 0, 10, 1, ".");
    private final Setting<BlackOutColor> bloomColor = this.sgGeneral.c("Bloom Color", new BlackOutColor(0, 0, 0, 100), "", () -> this.bloomIntensity.get() > 0);
    private final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgGeneral, "Text");
    private final Setting<BlackOutColor> customInfoColor = this.sgGeneral.c("Info Color", new BlackOutColor(150, 150, 150, 255), "Info text color.", () -> true);
    private int i = 0;
    private String info = "";

    public Arraylist() {
        super("Arraylist", "Shows currently enabled modules.");
        this.setSize(75.0F, 125.0F);
    }

    public static void updateDeltas() {
        deltaMap.forEach((module, mutableFloat) -> {
            float delta = BlackOut.mc.getRenderTickCounter().getLastFrameDuration() / 20.0F * 4.0F;
            mutableFloat.setValue(module.enabled ? Math.min(mutableFloat.getValue() + delta, 1.0F) : Math.max(mutableFloat.getValue() - delta, 0.0F));
        });
    }

    @Override
    public void render() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            Comparator<Module> comparator = Comparator.comparingDouble(
                    m -> BlackOut.FONT.getWidth(m.getDisplayName() + (m.getInfo() == null ? "" : this.getInfo(m.getInfo())))
            );
            List<Module> modules = Managers.MODULES
                    .getModules()
                    .stream()
                    .filter(module -> this.filterMode.get().shouldAccept(module, this.moduleList.get()) && module.category.parent() != ParentCategory.CLIENT)
                    .sorted(comparator.reversed())
                    .toList();
            if (this.bloomIntensity.get() > 0) {
                FrameBuffer buffer = Managers.FRAME_BUFFER.getBuffer("arraylist");
                FrameBuffer bloomBuffer = Managers.FRAME_BUFFER.getBuffer("arraylist-bloom");
                buffer.clear(0.0F, 0.0F, 0.0F, 1.0F);
                buffer.bind(true);
                this.render(
                        modules,
                        module -> {
                            String text = module.getDisplayName();
                            this.info = this.getInfo(module.getInfo());
                            float width = BlackOut.FONT.getWidth(text + this.info);
                            if (this.rounded.get()) {
                                RenderUtils.rounded(
                                        this.stack,
                                        -BlackOut.FONT.getWidth(text) - 2.0F - BlackOut.FONT.getWidth(this.info),
                                        1.5F,
                                        width - 2.0F,
                                        BlackOut.FONT.getHeight() - 2.0F,
                                        2.8F,
                                        0.0F,
                                        Color.WHITE.getRGB(),
                                        Color.WHITE.getRGB()
                                );
                            } else {
                                RenderUtils.quad(
                                        this.stack,
                                        -BlackOut.FONT.getWidth(text) - 4.0F - BlackOut.FONT.getWidth(this.info),
                                        0.0F,
                                        width + 2.0F,
                                        BlackOut.FONT.getHeight() + 2.0F,
                                        Color.WHITE.getRGB()
                                );
                            }
                        }
                );
                buffer.unbind();
                bloomBuffer.clear(0.0F, 0.0F, 0.0F, 1.0F);
                bloomBuffer.bind(true);
                RenderUtils.renderBufferWith(buffer, Shaders.screentex, new ShaderSetup(setup -> setup.set("alpha", 1.0F)));
                bloomBuffer.unbind();
                RenderUtils.blurBufferBW("arraylist-bloom", this.bloomIntensity.get() + 1);
                bloomBuffer.bind(true);
                Renderer.setTexture(buffer.getTexture(), 1);
                RenderUtils.renderBufferWith(bloomBuffer, Shaders.subtract, new ShaderSetup(setup -> setup.set("uTexture1", 1)));
                bloomBuffer.unbind();
                RenderUtils.renderBufferWith(bloomBuffer, Shaders.shaderbloom, new ShaderSetup(setup -> setup.color("clr", this.bloomColor.get().getRGB())));
            }

            if (this.rounded.get()) {
                FrameBuffer buffer = Managers.FRAME_BUFFER.getBuffer("arraylist");
                buffer.clear(0.0F, 0.0F, 0.0F, 1.0F);
                buffer.bind(true);
                this.render(
                        modules,
                        module -> {
                            String text = module.getDisplayName();
                            this.info = this.getInfo(module.getInfo());
                            float width = BlackOut.FONT.getWidth(text + this.info) - 2.0F;
                            RenderUtils.rounded(
                                    this.stack,
                                    -BlackOut.FONT.getWidth(text) - 2.0F - BlackOut.FONT.getWidth(this.info),
                                    1.5F,
                                    width,
                                    BlackOut.FONT.getHeight() - 2.0F,
                                    3.0F,
                                    0.0F,
                                    Color.WHITE.getRGB(),
                                    Color.WHITE.getRGB()
                            );
                        }
                );
                buffer.unbind();
                if (this.useBlur.get()) {
                    RenderUtils.renderBufferOverlay(buffer, Managers.FRAME_BUFFER.getBuffer("hudblur").getTexture());
                    Renderer.onHUDBlur();
                }

                if (this.bg.get()) {
                    RenderUtils.renderBufferWith(buffer, Shaders.shaderbloom, new ShaderSetup(setup -> setup.color("clr", this.bgColor.get().getRGB())));
                }
            }

            this.renderTexts(modules);
        }
    }

    private void renderTexts(List<Module> modules) {
        this.render(
                modules,
                module -> {
                    Color infoColor = this.customInfoColor.get().getColor();
                    String text = module.getDisplayName();
                    this.info = this.getInfo(module.getInfo());
                    float width = BlackOut.FONT.getWidth(text + this.info) + 2.0F;
                    if (this.sideBar.get() && !this.rounded.get()) {
                        RenderUtils.quad(this.stack, -2.0F, 0.0F, 1.0F, BlackOut.FONT.getHeight() + 2.0F, this.textColor.getTextColor().getRGB());
                    }

                    if (this.useBlur.get() && !this.rounded.get()) {
                        RenderUtils.drawLoadedBlur(
                                "hudblur",
                                this.stack,
                                renderer -> renderer.rounded(
                                        -BlackOut.FONT.getWidth(text) - (4.0F + BlackOut.FONT.getWidth(this.info)), 0.0F, width, BlackOut.FONT.getHeight() + 2.0F, 0.0F, 10
                                )
                        );
                        Renderer.onHUDBlur();
                    }

                    if (this.bg.get() && !this.rounded.get()) {
                        RenderUtils.quad(
                                this.stack,
                                -BlackOut.FONT.getWidth(text) - (4.0F + BlackOut.FONT.getWidth(this.info)),
                                0.0F,
                                width,
                                BlackOut.FONT.getHeight() + 2.0F,
                                this.bgColor.get().getRGB()
                        );
                    }

                    float x = -BlackOut.FONT.getWidth(text) - (3.0F + BlackOut.FONT.getWidth(this.info));
                    this.textColor.render(this.stack, text, 1.0F, x, 1.0F, false, false);
                    BlackOut.FONT.text(this.stack, this.info, 1.0F, -BlackOut.FONT.getWidth(this.info) - 3.0F, 1.0F, infoColor, false, false);
                }
        );
    }

    private String getBracket() {
        return switch (this.brackets.get()) {
            case None -> "";
            case Round -> "()";
            case Square -> "[]";
            case Wiggly -> "{}";
            case Triangle -> "<>";
        };
    }

    private String getInfo(String infoString) {
        if (this.brackets.get() == BracketMode.None) {
            this.info = infoString == null ? "" : " " + infoString;
        } else {
            this.info = infoString == null ? "" : " " + this.getBracket().charAt(0) + infoString + this.getBracket().charAt(1);
        }

        return this.drawInfo.get() ? this.info : "";
    }

    private void render(List<Module> list, Consumer<Module> consumer) {
        this.stack.push();
        this.stack.translate(75.0F, 0.0F, 0.0F);
        this.setSize(75.0F, Math.max((BlackOut.FONT.getHeight() + 2.0F) * this.i, 123.0F));
        this.i = 0;
        list.forEach(
                module -> {
                    float delta = deltaMap.get(module).floatValue();
                    if (!(delta <= 0.0F)) {
                        delta = (float) AnimUtils.easeOutQuad(delta);
                        float yDelta = (float) (Math.min(delta, 0.3) / 0.3);
                        float xDelta = (float) ((Math.max(delta, 0.3) - 0.3) / 0.7);
                        this.stack.push();
                        float prevAlpha = Renderer.getAlpha();
                        Renderer.setAlpha(prevAlpha * delta);
                        this.stack
                                .translate((1.0F - xDelta) * BlackOut.FONT.getWidth(module.getDisplayName() + " " + this.getInfo(module.getInfo())) + 1.0F, 0.0F, 0.0F);
                        consumer.accept(module);
                        Renderer.setAlpha(prevAlpha);
                        this.stack.pop();
                        this.stack.translate(0.0F, yDelta * (BlackOut.FONT.getHeight() + 2.0F), 0.0F);
                        this.i++;
                    }
                }
        );
        this.stack.pop();
    }

    public enum BracketMode {
        None,
        Round,
        Square,
        Wiggly,
        Triangle
    }
}
