package bodevelopment.client.blackout.hud;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BackgroundMultiSetting;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.util.math.MatrixStack;

public class TextElement extends HudElement {
    public final SettingGroup sgGeneral = this.addGroup("General");
    public final SettingGroup sgColor = this.addGroup("Color");

    private final Setting<Boolean> bg = this.sgGeneral.booleanSetting("Backdrop", true, "Enables the rendering of a solid or gradient background layer.");
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgGeneral, this.bg::get, null);
    private final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Gaussian Blur", true, "Applies a real-time blur effect behind the element for improved legibility.");
    private final Setting<Boolean> rounded = this.sgGeneral.booleanSetting("Rounded Corners", true, "Smooths the edges of the background and blur layers using a rounding radius.", () -> this.bg.get() || this.blur.get());

    private final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgColor, "Text");
    private final TextColorMultiSetting infoColor = TextColorMultiSetting.of(this.sgColor, "Info");

    public TextElement(String name, String description) {
        super(name, description);
    }

    protected void drawElement(MatrixStack stack, String text, String info) {
        stack.push();
        float width = BlackOut.FONT.getWidth(text + " " + info);
        this.setSize(width, BlackOut.FONT.getHeight());
        if (this.blur.get()) {
            RenderUtils.drawLoadedBlur(
                    "hudblur", stack, renderer -> renderer.rounded(0.0F, 0.0F, width, BlackOut.FONT.getHeight(), this.rounded.get() ? 3.0F : 0.0F, 10)
            );
            Renderer.onHUDBlur();
        }

        if (this.bg.get()) {
            this.background.render(stack, 0.0F, 0.0F, width, BlackOut.FONT.getHeight(), this.rounded.get() ? 3.0F : 0.0F, 3.0F);
        }

        this.textColor.render(stack, text, 1.0F, 0.0F, 0.0F, false, false);
        this.infoColor.render(stack, info, 1.0F, BlackOut.FONT.getWidth(text + " "), 0.0F, false, false);
        stack.pop();
    }
}
