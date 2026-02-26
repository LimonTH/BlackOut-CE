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

    public boolean isChoosing() {
        return choosing;
    }

    public float getWi() {
        return wi;
    }

    public float getXOffset() {
        return xOffset;
    }

    public T[] getValues() {
        return values;
    }

    public void renderDropdown() {
        if (!this.choosing) return;

        float baseH = 26.0F;
        float entryHeight = 20.0F;
        float listWidth = this.wi + 10.0F;
        float listX = this.x + this.width - listWidth - this.xOffset - 2.5F;
        float listY = this.y + baseH;

        float listHeight = (this.values.length - 1) * entryHeight;

        this.stack.push();
        this.stack.translate(0, 0, 800);

        RenderUtils.rounded(this.stack, listX, listY, listWidth, listHeight, 4.0F, 8.0F, new Color(0, 0, 0, 200).getRGB(), ColorUtils.SHADOW100I);
        RenderUtils.rounded(this.stack, listX, listY, listWidth, listHeight, 4.0F, 0.0F, new Color(20, 20, 20, 255).getRGB(), ColorUtils.SHADOW100I);

        int index = 0;
        for (T t : this.values) {
            if (t == this.get()) continue;

            float currentItemY = listY + (index * entryHeight);

            boolean hovered = this.mx > listX && this.mx < listX + listWidth && this.my > currentItemY && this.my < currentItemY + entryHeight;

            if (hovered) {
                RenderUtils.rounded(this.stack, listX + 1, currentItemY, listWidth - 2, entryHeight, 2.0F, 0.0F, new Color(255, 255, 255, 15).getRGB(), ColorUtils.SHADOW100I);
            }

            float itemScale = 1.6F;
            BlackOut.FONT.text(
                    this.stack,
                    t.name(),
                    itemScale,
                    listX + (listWidth / 2.0F),
                    currentItemY + (entryHeight / 2.0F),
                    hovered ? Color.WHITE : new Color(160, 160, 160),
                    true,
                    true
            );

            index++;
        }

        this.stack.pop();
    }

    @Override
    public float render() {
        if (BlackOut.mc.textRenderer != null && this.maxWidth == 0.0) {
            this.maxWidth = 0.0;
            for (T v : this.values) {
                double w = BlackOut.FONT.getWidth(v.name()) * 2.0F;
                if (w > this.maxWidth) {
                    this.maxWidth = w;
                }
            }
            this.xOffset = (float) Math.max(this.maxWidth / 2.0 - 60.0, 0.0);
            this.wi = (float) Math.max(this.maxWidth, 100.0);
        }

        float textScale = 2.0F;
        float baseH = 26.0F;
        float middleY = this.y + (baseH / 2.0F);
        float fontHeight = BlackOut.FONT.getHeight() * textScale;

        float nameY = middleY - (fontHeight / 2.0F);
        BlackOut.FONT.text(this.stack, this.name, textScale, this.x + 5.0F, nameY, GuiColorUtils.getSettingText(this.y), false, true);

        float valueX = this.x + this.width - this.wi / 2.0F - 10.0F - this.xOffset;
        float valueY = middleY - (fontHeight / 2.0F);
        BlackOut.FONT.text(this.stack, this.get().name(), textScale, valueX, valueY, GuiColorUtils.getSettingText(this.y), true, true);
        return this.getHeight();
    }

    @Override
    public float getHeight() {
        return 26.0F;
    }

    @Override
    public boolean onMouse(int key, boolean pressed) {
        if (key == 0 && pressed) {
            float clickOffset = -5.5F;

            boolean mainHover = this.mx > this.x && this.mx < this.x + this.width
                    && this.my > this.y + clickOffset && this.my < this.y + 26.0F + clickOffset;

            if (mainHover) {
                this.choosing = !this.choosing;
                return true;
            }

            if (this.choosing) {
                float entryHeight = 20.0F;
                float listWidth = this.wi + 10.0F;
                float listX = this.x + this.width - listWidth - this.xOffset - 2.5F;

                float listY = this.y + 26.0F + clickOffset;
                float listHeight = (this.values.length - 1) * entryHeight;

                if (this.mx > listX && this.mx < listX + listWidth
                        && this.my > listY && this.my < listY + listHeight) {

                    int clickedIndex = (int) ((this.my - listY) / entryHeight);

                    int currentIndex = 0;
                    for (T t : this.values) {
                        if (t == this.get()) continue;
                        if (currentIndex == clickedIndex) {
                            this.setValue(t);
                            this.choosing = false;
                            Managers.CONFIG.saveAll();
                            return true;
                        }
                        currentIndex++;
                    }
                }

                this.choosing = false;
            }
        }
        return false;
    }

    public T getClosest() {
        float baseH = 26.0F;
        float offset = 0.0F;
        T closest = this.get();
        double cd = Math.abs(this.my - (this.y + baseH / 2.0F));

        for (T t : this.values) {
            if (t != this.get()) {
                offset += 20.0F;
                double d = Math.abs(this.my - (this.y + baseH + offset - 10.0F));
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
