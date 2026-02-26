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

public class BoolSetting extends Setting<Boolean> {
    private float progress = -1.0F;

    public BoolSetting(String name, Boolean val, String description, SingleOut<Boolean> visible) {
        super(name, val, description, visible);
    }

    @Override
    public float render() {
        float target = this.get() ? 1.0F : 0.0F;
        if (this.progress < 0.0F) {
            this.progress = target;
        } else {
            this.progress = MathHelper.lerp(Math.min(this.frameTime * 20.0F, 1.0F), this.progress, target);
        }

        float textScale = 2.0F;
        float h = this.getHeight();
        float middleY = this.y + (h / 2.0F);

        float fontHeight = BlackOut.FONT.getHeight() * textScale;
        float textY = middleY - (fontHeight / 2.0F);

        BlackOut.FONT.text(this.stack, this.name, textScale, this.x + 5.0F, textY, GuiColorUtils.getSettingText(this.y), false, true);

        float toggleWidth = 16.0F;
        float toggleX = this.x + this.width - toggleWidth - 10.0F;

        float toggleRenderY = middleY - 5.5F;

        RenderUtils.rounded(
                this.stack,
                toggleX,
                toggleRenderY,
                toggleWidth,
                0.0F,
                8.0F,
                0.0F,
                ColorUtils.lerpColor(this.progress, GuiColorUtils.getDisabledBindBG(this.y), GuiColorUtils.getEnabledBindBG(this.y)).getRGB(),
                ColorUtils.SHADOW100I
        );

        RenderUtils.rounded(
                this.stack,
                toggleX + (this.progress * toggleWidth),
                toggleRenderY,
                0.0F,
                0.0F,
                8.0F,
                0.0F,
                ColorUtils.lerpColor(this.progress, GuiColorUtils.getDisabledBindDot(this.y), GuiColorUtils.getEnabledBindDot(this.y)).getRGB(),
                ColorUtils.SHADOW100I
        );

        return h;
    }

    @Override
    public boolean onMouse(int key, boolean pressed) {
        float clickOffset = -5.5F;
        if (key == 0 && pressed && this.mx > this.x && this.mx < this.x + this.width
                && this.my > this.y + clickOffset && this.my < this.y + this.getHeight() + clickOffset) {

            this.setValue(!this.get());
            Managers.CONFIG.saveAll();
            return true;
        }
        return false;
    }

    @Override
    public float getHeight() {
        return 26.0F;
    }

    @Override
    public void write(JsonObject object) {
        object.addProperty(this.name, this.get());
    }

    @Override
    public void set(JsonElement element) {
        this.setValue(element.getAsBoolean());
    }
}