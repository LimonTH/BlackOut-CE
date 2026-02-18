package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.module.modules.client.GuiSettings;
import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import bodevelopment.client.blackout.rendering.shader.Shaders;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;

public class GuiRenderUtils {
    private static final long initTime = System.currentTimeMillis();

    public static void renderWaveText(MatrixStack stack, String text, float textScale, float x, float y, boolean xCenter, boolean yCenter, boolean bold) {
        GuiSettings guiSettings = GuiSettings.getInstance();
        guiSettings.textColor.render(stack, text, textScale, x, y, xCenter, yCenter, bold);
    }

    public static void renderWaveText(MatrixStack stack, String text, float textScale, float x, float y, boolean xCenter, boolean yCenter, int clr1, int clr2) {
        float shaderTime = (float) (System.currentTimeMillis() - initTime) / 1000.0f;

        BlackOut.FONT.text(stack, text, textScale, x, y, Color.WHITE.getRGB(), xCenter, yCenter, Shaders.fontwave, new ShaderSetup(setup -> {
            setup.set("frequency", 10.0F);
            setup.set("speed", 2.0F);
            setup.color("clr1", clr1);
            setup.color("clr2", clr2);
            setup.set("time", shaderTime);
        }));
    }

    public static Color getGuiColors(double darkness) {
        return getGuiColors((float) darkness);
    }

    public static Color getGuiColors(float darkness) {
        GuiSettings guiSettings = GuiSettings.getInstance();

        if (guiSettings.textColor.isWave()) {
            Color c1 = guiSettings.textColor.getTextColor().getColor();
            Color c2 = guiSettings.textColor.getWaveColor().getColor();

            Color dc1 = ColorUtils.dark(c1, 1.0 / darkness);
            Color dc2 = ColorUtils.dark(c2, 1.0 / darkness);

            return ColorUtils.getWave(dc1, dc2, 2.0, 1.0, 1);
        }

        if (guiSettings.textColor.isRainbow()) {
            return new Color(ColorUtils.getRainbow(10.0F, guiSettings.textColor.saturation(), darkness));
        }

        return guiSettings.textColor.getTextColor().getColor();
    }

    public static int withBrightness(int color, double brightness) {
        int a = color >> 24 & 0xFF;
        int r = (int) ((color >> 16 & 0xFF) * brightness);
        int g = (int) ((color >> 8 & 0xFF) * brightness);
        int b = (int) ((color & 0xFF) * brightness);

        return (a << 24) | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
    }
}
