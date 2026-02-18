package bodevelopment.client.blackout.module.setting.settings;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

public class EnumSetting<T extends Enum<?>> extends Setting<T> {
    public T[] values;
    private boolean choosing = false;
    private double maxWidth = 0.0;
    private float xOffset = 0.0F;
    private float wi = 0.0F;

    @SuppressWarnings("unchecked")
    public EnumSetting(String name, T val, String description, SingleOut<Boolean> visible) {
        super(name, val, description, visible);

        this.values = (T[]) val.getDeclaringClass().getEnumConstants();
    }

    @Override
    public float render() {
        if (BlackOut.mc.textRenderer != null && this.maxWidth == 0.0) {
            this.maxWidth = 0.0;

            for (T v : this.values) {
                double w = BlackOut.FONT.getWidth(v.name());
                if (w > this.maxWidth) {
                    this.maxWidth = w;
                }
            }

            this.xOffset = (float) Math.max(this.maxWidth / 2.0 - 60.0, 0.0);
            this.wi = (float) Math.max(this.maxWidth, 100.0);
        }

        float offset = 0.0F;
        if (this.choosing) {
            RenderUtils.rounded(
                    this.stack,
                    this.x + this.width - this.wi - this.xOffset - 10.0F,
                    this.y,
                    this.wi,
                    this.values.length * 20,
                    4.0F,
                    2.0F,
                    new Color(25, 25, 25, 85).getRGB(),
                    ColorUtils.SHADOW100I
            );

            for (T t : this.values) {
                if (t != this.get()) {
                    offset += 20.0F;
                    double xm = this.mx - (this.x + this.width - this.wi / 2.0F - 10.0F - this.xOffset);
                    double ym = this.my - (this.y + 10 + offset);
                    double d = 3.0 - MathHelper.clamp(Math.sqrt(xm * xm + ym * ym) / 10.0, 1.0, 2.0) - 1.0;
                    BlackOut.FONT
                            .text(
                                    this.stack,
                                    t.name(),
                                    1.8F,
                                    this.x + this.width - this.wi / 2.0F - 10.0F - this.xOffset,
                                    this.y + 10 + offset,
                                    ColorUtils.lerpColor(d, new Color(150, 150, 150, 255), new Color(200, 200, 200, 255)),
                                    true,
                                    true
                            );
                }
            }
        }

        BlackOut.FONT
                .text(
                        this.stack,
                        this.get().name(),
                        2.0F,
                        this.x + this.width - this.wi / 2.0F - 10.0F - this.xOffset,
                        this.y + 9,
                        GuiColorUtils.getSettingText(this.y),
                        true,
                        true
                );
        BlackOut.FONT.text(this.stack, this.name, 2.0F, this.x + 5, this.y + 9, GuiColorUtils.getSettingText(this.y), false, true);
        return this.getHeight();
    }

    @Override
    public float getHeight() {
        return 25 + (this.choosing ? (this.values.length - 1) * 20 + 4 : 0);
    }

    @Override
    public boolean onMouse(int key, boolean pressed) {
        if (key == 0
                && pressed
                && this.mx > this.x + this.width - this.wi - this.xOffset - 14.0F
                && this.mx < this.x + this.width - this.xOffset + 4.0F
                && this.my > this.y
                && this.my < this.y + this.getHeight() + (this.choosing ? 20 * this.values.length + 9 : 0)) {
            if (this.choosing) {
                this.setValue(this.getClosest());
                Managers.CONFIG.saveAll();
            }

            this.choosing = !this.choosing;
            return true;
        } else {
            return false;
        }
    }

    public T getClosest() {
        float offset = 0.0F;
        T closest = this.get();
        double cd = this.my - (this.y + 9);

        for (T t : this.values) {
            if (t != this.get()) {
                offset += 20.0F;
                double d = Math.abs(this.my - (this.y + 10 + offset));
                if (d < cd) {
                    closest = t;
                    cd = d;
                }
            }
        }

        return closest;
    }

    @Override
    public void write(JsonObject object) {
        object.addProperty(this.name, this.get().name());
    }

    @Override
    public void set(JsonElement element) {
        T newVal = null;

        for (T val : this.values) {
            if (element.getAsString().equals(val.name())) {
                newVal = val;
                break;
            }
        }

        if (newVal != null) {
            this.setValue(newVal);
        }
    }
}
