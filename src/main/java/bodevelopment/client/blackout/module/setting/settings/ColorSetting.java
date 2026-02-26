package bodevelopment.client.blackout.module.setting.settings;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.clickgui.screens.ColorScreen;
import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.client.ThemeSettings;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

public class ColorSetting extends Setting<BlackOutColor> {
    public int theme = 0;
    public float saturation = 0.0F;
    public float brightness = 0.0F;
    public int alpha = 255;
    public BlackOutColor actual = this.value.copy();

    public ColorSetting(String name, BlackOutColor val, String description, SingleOut<Boolean> visible) {
        super(name, val, description, visible);
    }

    @Override
    public float render() {
        float textScale = 2.0F;
        float h = this.getHeight();
        float middleY = this.y + (h / 2.0F);

        float fontHeight = BlackOut.FONT.getHeight() * textScale;

        float textY = middleY - (fontHeight / 2.0F);

        BlackOut.FONT.text(this.stack, this.name, textScale, this.x + 5.0F, textY, GuiColorUtils.getSettingText(this.y), false, true);

        float rectWidth = 25.0F;
        float rectHeight = 8.0F;
        float rectX = this.x + this.width - rectWidth - 5.0F;

        float rectRenderY = middleY - (rectHeight / 2.0F) - 5.5F;

        int color = this.get().withAlpha(255).getRGB();

        RenderUtils.rounded(
                this.stack,
                rectX,
                rectRenderY,
                rectWidth,
                rectHeight,
                3.0F,
                4.0F,
                color,
                color
        );

        return h;
    }

    @Override
    public boolean onMouse(int key, boolean pressed) {
        float clickOffset = -5.5F;

        if (key == 0 && pressed && this.mx > this.x && this.mx < this.x + this.width
                && this.my > this.y + clickOffset && this.my < this.y + this.getHeight() + clickOffset) {

            Managers.CLICK_GUI.openScreen(new ColorScreen(this, this.name));
            Managers.CONFIG.saveAll();
            return true;
        } else {
            return false;
        }
    }

    public BlackOutColor get() {
        return switch (this.theme) {
            case 1 -> this.modifyTheme(ThemeSettings.getInstance().getMain());
            case 2 -> this.modifyTheme(ThemeSettings.getInstance().getSecond());
            default -> this.actual;
        };
    }

    public BlackOutColor getUnmodified() {
        return switch (this.theme) {
            case 1 -> BlackOutColor.from(ThemeSettings.getInstance().getMain());
            case 2 -> BlackOutColor.from(ThemeSettings.getInstance().getSecond());
            default -> this.actual;
        };
    }

    private BlackOutColor modifyTheme(int theme) {
        float[] HSB = Color.RGBtoHSB(ColorHelper.Argb.getRed(theme), ColorHelper.Argb.getGreen(theme), ColorHelper.Argb.getBlue(theme), new float[3]);
        HSB[1] = MathHelper.clamp(HSB[1] + this.saturation, 0.0F, 1.0F);
        HSB[2] = MathHelper.clamp(HSB[2] + this.brightness, 0.0F, 1.0F);
        return BlackOutColor.from(ColorUtils.withAlpha(Color.HSBtoRGB(HSB[0], HSB[1], HSB[2]), this.alpha));
    }

    @Override
    public float getHeight() {
        return 26.0F;
    }

    @Override
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty(this.name, this.theme + "§" + this.alpha + "§" + this.saturation + "§" + this.brightness + "§" + this.actual.getRGB());
    }

    @Override
    public void set(JsonElement element) {
        String[] strings = element.getAsString().split("§");
        if (strings.length != 5) {
            this.theme = 0;
            this.alpha = 255;
            this.brightness = 0.0F;
            this.saturation = 0.0F;
            this.reset();
        } else {
            this.theme = Integer.parseInt(strings[0]);
            this.alpha = Integer.parseInt(strings[1]);
            this.saturation = MathHelper.clamp(Float.parseFloat(strings[2]), -1.0F, 1.0F);
            this.brightness = MathHelper.clamp(Float.parseFloat(strings[3]), -1.0F, 1.0F);
            this.actual = BlackOutColor.from(Integer.parseInt(strings[4]));
        }
    }

    @Override
    public void reset() {
        super.reset();
        this.actual = this.defaultValue.copy();
    }

    public void setValue(BlackOutColor color) {
        super.setValue(color);
    }
}
