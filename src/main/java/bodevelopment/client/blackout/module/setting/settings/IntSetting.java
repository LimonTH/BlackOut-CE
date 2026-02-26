package bodevelopment.client.blackout.module.setting.settings;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.TextField;
import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.SelectedComponent;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

public class IntSetting extends Setting<Integer> {
    private static final Color CLEAR = new Color(255, 255, 255, 0);
    public final int min;
    public final int max;
    public final int step;
    private final TextField textField = new TextField();
    private final int id = SelectedComponent.nextId();
    private float sliderPos;
    private float sliderAnim = 0.0F;
    private boolean moving = false;

    public IntSetting(String name, Integer val, int min, int max, int step, String description, SingleOut<Boolean> visible) {
        super(name, val, description, visible);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    @Override
    public float render() {
        float sliderPadding = 10.0F;

        if (this.moving) {
            this.sliderPos = (float) MathHelper.clamp(MathHelper.getLerpProgress(this.mx, this.x + sliderPadding, this.x + this.width - sliderPadding), 0.0, 1.0);
            float val = MathHelper.lerp(this.sliderPos, (float)this.min, (float)this.max);
            this.setValue(Math.round(val / this.step) * this.step);
        } else {
            this.sliderPos = MathHelper.clamp(MathHelper.getLerpProgress(this.get(), this.min, this.max), 0.0F, 1.0F);
        }

        if (Float.isNaN(this.sliderAnim) || this.sliderAnim == -1.0F) {
            this.sliderAnim = this.sliderPos;
        }
        this.sliderAnim = MathHelper.clamp(MathHelper.clampedLerp(this.sliderAnim, this.sliderPos, this.frameTime * 20.0F), 0.0F, 1.0F);

        float textScale = 2.0F;
        float baseH = 26.0F;
        float middleY = this.y + (baseH / 2.0F);

        float fontHeight = BlackOut.FONT.getHeight() * textScale;
        float topTextY = middleY - (fontHeight / 2.0F);

        BlackOut.FONT.text(this.stack, this.name, textScale, this.x + 5.0F, topTextY, GuiColorUtils.getSettingText(this.y), false, true);

        if (SelectedComponent.is(this.id)) {
            try {
                this.setValue(Integer.parseInt(this.textField.getContent()));
            } catch (NumberFormatException e) {
                try {
                    this.setValue((int) Math.round(Double.parseDouble(this.textField.getContent().replace(",", "."))));
                } catch (NumberFormatException ignored) {}
            }
        } else {
            this.textField.setContent(String.valueOf(this.get()));
        }

        float fieldWidth = 40.0F;
        float fieldHeight = 10.0F;
        float fieldX = this.x + this.width - fieldWidth - 7.0F;
        float fieldY = middleY - (fieldHeight / 2.0F) - 5.5F;

        this.textField.setActive(SelectedComponent.is(this.id));
        this.textField.render(
                this.stack, textScale, this.mx, this.my, fieldX, fieldY, fieldWidth, fieldHeight, 2.0F, 5.0F, GuiColorUtils.getSettingText(this.y), CLEAR
        );

        float barWidth = this.width - 20.0F;

        RenderUtils.rounded(this.stack, this.x + 10, this.y + 25, barWidth, 0.0F, 6.0F, 2.0F, new Color(0, 0, 0, 50).getRGB(), ColorUtils.SHADOW100I);
        RenderUtils.rounded(
                this.stack,
                this.x + 10,
                this.y + 25,
                this.sliderAnim * barWidth,
                0.0F,
                4.0F,
                2.0F,
                GuiColorUtils.getSettingText(this.y).getRGB(),
                ColorUtils.SHADOW100I
        );

        return this.getHeight();
    }

    @Override
    public boolean onMouse(int key, boolean pressed) {
        if (key != 0) {
            return false;
        } else if (!pressed) {
            if (this.moving) {
                Managers.CONFIG.saveAll();
            }
            this.moving = false;
            return true;
        }

        if (this.textField.click(0, true)) {
            SelectedComponent.setId(this.id);
            return true;
        }
        else if (this.mx > this.x && this.mx < this.x + this.width && this.my > this.y && this.my < this.y + this.getHeight()) {
            if (SelectedComponent.is(this.id)) {
                SelectedComponent.reset();
            }

            this.moving = true;
            Managers.CONFIG.saveAll();
            return true;
        }
        else {
            if (SelectedComponent.is(this.id)) {
                SelectedComponent.reset();
            }
            return false;
        }
    }

    @Override
    public void onKey(int key, boolean pressed) {
        if (key == 257 && SelectedComponent.is(this.id)) {
            SelectedComponent.reset();
        }

        this.textField.type(key, pressed);
    }

    @Override
    public float getHeight() {
        return 38.0F;
    }

    @Override
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty(this.name, this.get());
    }

    @Override
    public void set(JsonElement element) {
        this.setValue(element.getAsInt());
        this.sliderPos = MathHelper.clamp(MathHelper.getLerpProgress(this.get(), (float)this.min, (float)this.max), 0.0F, 1.0F);
        this.sliderAnim = this.sliderPos;
    }
}