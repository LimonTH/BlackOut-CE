package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.option.KeyBinding;

public class Keystrokes extends HudElement {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<Boolean> useBlur = this.sgGeneral.booleanSetting("Blur", true, "Uses a blur effect", () -> true);
    private final Setting<Boolean> shadow = this.sgGeneral.booleanSetting("Shadow", true, ".", () -> true);
    private final Setting<BlackOutColor> shadowColor = this.sgGeneral.colorSetting("Shadow Color", new BlackOutColor(0, 0, 0, 100), "Shadow Color", this.shadow::get);
    private final Setting<BlackOutColor> pressedShadow = this.sgGeneral
            .colorSetting("Pressed Shadow", new BlackOutColor(255, 255, 255, 100), "Pressed Shadow Color", this.shadow::get);
    private final Setting<BlackOutColor> txtdColor = this.sgGeneral.colorSetting("Text Color", new BlackOutColor(255, 255, 255, 255), ".");
    private final Setting<BlackOutColor> pressedtxtColor = this.sgGeneral.colorSetting("Pressed Text Color", new BlackOutColor(175, 175, 175, 255), ".");
    private final Setting<BlackOutColor> backgroundColor = this.sgGeneral.colorSetting("Background Color", new BlackOutColor(0, 0, 0, 50), "Background Color");
    private final Setting<BlackOutColor> pressedColor = this.sgGeneral.colorSetting("Pressed Color", new BlackOutColor(255, 255, 255, 50), "Pressed Color");

    public Keystrokes() {
        super("Keystrokes", "Shows your keystrokes");
        this.setSize(10.0F, 10.0F);
    }

    @Override
    public void render() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.stack.push();
            this.setSize(44.0F, 48.0F);
            this.renderKey(18, 0, "W", BlackOut.mc.options.forwardKey);
            this.renderKey(0, 18, "A", BlackOut.mc.options.leftKey);
            this.renderKey(18, 18, "S", BlackOut.mc.options.backKey);
            this.renderKey(36, 18, "D", BlackOut.mc.options.rightKey);
            if (this.useBlur.get()) {
                RenderUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(0.0F, 36.0F, 44.0F, 8.0F, 3.0F, 10));
                Renderer.onHUDBlur();
            }

            boolean pressed = BlackOut.mc.options.jumpKey.isPressed();
            BlackOutColor color = pressed ? this.pressedColor.get() : this.backgroundColor.get();
            RenderUtils.rounded(
                    this.stack, 0.0F, 36.0F, 44.0F, 8.0F, 3.0F, this.shadow.get() ? 3.0F : 0.0F, color.getRGB(), color.withAlpha((int) (color.alpha * 0.5)).getRGB()
            );
            RenderUtils.rounded(
                    this.stack,
                    17.0F,
                    38.0F,
                    10.0F,
                    1.0F,
                    1.0F,
                    0.0F,
                    pressed ? this.pressedtxtColor.get().getRGB() : this.txtdColor.get().getRGB(),
                    ColorUtils.SHADOW100I
            );
            this.stack.pop();
        }
    }

    public void renderKey(int x, int y, String key, KeyBinding bind) {
        boolean pressed = bind.isPressed();
        if (this.useBlur.get()) {
            RenderUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(x, y, 8.0F, 8.0F, 3.0F, 10));
            Renderer.onHUDBlur();
        }

        RenderUtils.rounded(
                this.stack,
                x,
                y,
                8.0F,
                8.0F,
                3.0F,
                this.shadow.get() ? 3.0F : 0.0F,
                pressed ? this.pressedColor.get().getRGB() : this.backgroundColor.get().getRGB(),
                pressed ? this.pressedShadow.get().getRGB() : this.shadowColor.get().getRGB()
        );
        BlackOut.FONT.text(this.stack, key, 1.0F, x + 4, y + 4, pressed ? this.pressedtxtColor.get().getColor() : this.txtdColor.get().getColor(), true, true);
    }
}
