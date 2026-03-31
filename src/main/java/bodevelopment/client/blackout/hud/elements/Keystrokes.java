package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.util.PlayerUtils;
import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.render.Render2DUtils;
import net.minecraft.client.KeyMapping;

public class Keystrokes extends HudElement {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Boolean> useBlur = this.sgGeneral.booleanSetting("Gaussian Diffusion", true, "Applies a real-time blur effect behind the keys for visual depth.");
    private final Setting<Boolean> shadow = this.sgGeneral.booleanSetting("Structural Shadow", true, "Adds a subtle shadow around each key to simulate elevation.");
    private final Setting<BlackOutColor> shadowColor = this.sgGeneral.colorSetting("Idle Shadow", new BlackOutColor(0, 0, 0, 100), "The shadow color when a key is not being pressed.");
    private final Setting<BlackOutColor> pressedShadow = this.sgGeneral.colorSetting("Active Shadow", new BlackOutColor(255, 255, 255, 100), "The shadow color when a key is actively held down.");
    private final Setting<BlackOutColor> txtdColor = this.sgGeneral.colorSetting("Idle Text", new BlackOutColor(255, 255, 255, 255), "The color of the key labels in their default state.");
    private final Setting<BlackOutColor> pressedtxtColor = this.sgGeneral.colorSetting("Active Text", new BlackOutColor(175, 175, 175, 255), "The color of the key labels when the key is pressed.");
    private final Setting<BlackOutColor> backgroundColor = this.sgGeneral.colorSetting("Idle Surface", new BlackOutColor(0, 0, 0, 50), "The background color of the keys in their default state.");
    private final Setting<BlackOutColor> pressedColor = this.sgGeneral.colorSetting("Active Surface", new BlackOutColor(255, 255, 255, 50), "The background color of the keys when actively pressed.");

    public Keystrokes() {
        super("Keystrokes", "Displays a real-time visual representation of movement and jump key inputs.");
        this.setSize(10.0F, 10.0F);
    }

    @Override
    public void render() {
        if (PlayerUtils.isInGame()) {
            this.stack.pushPose();
            this.setSize(44.0F, 48.0F);
            this.renderKey(18, 0, "W", BlackOut.mc.options.keyUp);
            this.renderKey(0, 18, "A", BlackOut.mc.options.keyLeft);
            this.renderKey(18, 18, "S", BlackOut.mc.options.keyDown);
            this.renderKey(36, 18, "D", BlackOut.mc.options.keyRight);
            if (this.useBlur.get()) {
                Render2DUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(0.0F, 36.0F, 44.0F, 8.0F, 3.0F, 10));
                Renderer.onHUDBlur();
            }

            boolean pressed = BlackOut.mc.options.keyJump.isDown();
            BlackOutColor color = pressed ? this.pressedColor.get() : this.backgroundColor.get();
            Render2DUtils.rounded(
                    this.stack, 0.0F, 36.0F, 44.0F, 8.0F, 3.0F, this.shadow.get() ? 3.0F : 0.0F, color.getRGB(), color.withAlpha((int) (color.alpha * 0.5)).getRGB()
            );
            Render2DUtils.rounded(
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
            this.stack.popPose();
        }
    }

    public void renderKey(int x, int y, String key, KeyMapping bind) {
        boolean pressed = bind.isDown();
        if (this.useBlur.get()) {
            Render2DUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(x, y, 8.0F, 8.0F, 3.0F, 10));
            Renderer.onHUDBlur();
        }

        Render2DUtils.rounded(
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
