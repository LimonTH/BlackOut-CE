package bodevelopment.client.blackout.util;


import net.minecraft.util.math.MathHelper;

import java.awt.*;

public class ColorUtils {
    public static Color SHADOW100 = new Color(0, 0, 0, 100);
    public static int SHADOW100I = SHADOW100.getRGB();
    public static Color SHADOW80 = new Color(0, 0, 0, 80);
    public static int SHADOW80I = SHADOW80.getRGB();

    public static int intColor(int red, int green, int blue, int alpha) {
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    public static Color dark(Color color, double multiplier) {
        return new Color(
                (int) Math.floor(color.getRed() / multiplier),
                (int) Math.floor(color.getGreen() / multiplier),
                (int) Math.floor(color.getBlue() / multiplier),
                color.getAlpha()
        );
    }

    public static Color lerpColor(double delta, Color min, Color max) {
        return new Color(
                lerp(delta, min.getRed(), max.getRed()),
                lerp(delta, min.getGreen(), max.getGreen()),
                lerp(delta, min.getBlue(), max.getBlue()),
                lerp(delta, min.getAlpha(), max.getAlpha())
        );
    }

    public static Color getWave(Color color, Color color2, double speed, double length, int i) {
        double f = Math.sin(System.currentTimeMillis() / 1000.0 * speed - i / length) + 1.0;
        return new Color(
                colorVal(color.getRed(), color2.getRed(), f),
                colorVal(color.getGreen(), color2.getGreen(), f),
                colorVal(color.getBlue(), color2.getBlue(), f),
                color.getAlpha()
        );
    }

    public static int colorVal(int original, int wave, double f) {
        return MathHelper.clamp((int) Math.floor(wave + (original - wave) * f), 0, 255);
    }

    public static int getRainbow(float seconds, float saturation, float brightness) {
        float hue = (float) (System.currentTimeMillis() % (int) (seconds * 1000.0F)) / (seconds * 1000.0F);
        return Color.HSBtoRGB(hue, saturation, brightness);
    }

    public static int getRainbow(float seconds, float saturation, float brightness, long index) {
        float hue = (float) ((System.currentTimeMillis() + index) % (int) (seconds * 1000.0F)) / (seconds * 1000.0F);
        return Color.HSBtoRGB(hue, saturation, brightness);
    }

    public static float sinWave(float seconds, double offset) {
        return ((float) Math.sin((System.currentTimeMillis() + offset * 1000.0) % (int) (seconds * 1000.0F) / (seconds * 1000.0F) * 2.0 * Math.PI) + 1.0F) / 2.0F;
    }

    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static int withAlpha(int color, int alpha) {
        return alpha << 24 | color & 16777215;
    }

    public static int alphaMulti(int color, double alpha) {
        return withAlpha(color, (int) ((color >>> 24 & 0xFF) * alpha));
    }

    private static int lerp(double delta, int min, int max) {
        return (int) (min + (max - min) * delta);
    }
}
