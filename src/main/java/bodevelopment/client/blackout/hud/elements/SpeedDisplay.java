package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BackgroundMultiSetting;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.util.math.Vec3d;

public class SpeedDisplay extends HudElement {
    public final SettingGroup sgGeneral = this.addGroup("General");

    private final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgGeneral, "Text Color");
    private final Setting<Boolean> bg = this.sgGeneral.booleanSetting("Backdrop", true, "Renders a background panel.");
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgGeneral, this.bg::get, null);
    private final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Gaussian Diffusion", true, "Applies a blur effect.");
    private final Setting<Boolean> rounded = this.sgGeneral.booleanSetting("Bezel Rounding", true, "Smooths corners.", () -> this.bg.get() || this.blur.get());

    private final Setting<Boolean> onlyHorizontal = this.sgGeneral.booleanSetting("Only Horizontal", true, "Excludes vertical velocity from calculation.");
    private final Setting<Integer> decimals = this.sgGeneral.intSetting("Decimals", 1, 0, 3, 1, "How many decimal places to show.");

    public SpeedDisplay() {
        super("Speed", "Displays your current movement velocity in blocks per second.");
        this.setSize(40.0F, 10.0F);
    }

    @Override
    public void render() {
        if (BlackOut.mc.player == null) return;

        Vec3d vel = BlackOut.mc.player.getVelocity();
        double speed = onlyHorizontal.get() ? vel.horizontalLength() : vel.length();
        speed *= 20.0;

        String speedString = String.format("%." + decimals.get() + "f", speed);
        String text = "Speed: " + speedString + " bps";

        float width = BlackOut.FONT.getWidth(text);
        float height = BlackOut.FONT.getHeight();

        this.stack.push();

        if (this.blur.get()) {
            RenderUtils.drawLoadedBlur(
                    "hudblur",
                    this.stack,
                    renderer -> renderer.rounded(0.0F, 0.0F, width + 4.0F, height + 2.0F, this.rounded.get() ? 3.0F : 0.0F, 10)
            );
            Renderer.onHUDBlur();
        }
        if (this.bg.get()) {
            this.background.render(this.stack, 0.0F, 0.0F, width + 4.0F, height + 2.0F, this.rounded.get() ? 3.0F : 0.0F, 3.0F);
        }
        this.setSize(width + 4.0F, height + 2.0F);
        this.textColor.render(this.stack, text, 2.0F, 1.0F, 0.0F, false, false, false);

        this.stack.pop();
    }
}