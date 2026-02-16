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

        BlackOut.FONT.text(this.stack, this.name, 2.0F, this.x + 5, this.y + 9, GuiColorUtils.getSettingText(this.y), false, true);
        RenderUtils.rounded(
                this.stack,
                this.x + this.width - 30.0F,
                this.y + 9,
                16.0F,
                0.0F,
                8.0F,
                0.0F,
                ColorUtils.lerpColor(this.progress, GuiColorUtils.getDisabledBindBG(this.y), GuiColorUtils.getEnabledBindBG(this.y)).getRGB(),
                ColorUtils.SHADOW100I
        );
        RenderUtils.rounded(
                this.stack,
                this.x + this.width - 30.0F + this.progress * 16.0F,
                this.y + 9,
                0.0F,
                0.0F,
                8.0F,
                0.0F,
                ColorUtils.lerpColor(this.progress, GuiColorUtils.getDisabledBindDot(this.y), GuiColorUtils.getEnabledBindDot(this.y)).getRGB(),
                ColorUtils.SHADOW100I
        );
        return this.getHeight();
    }

    @Override
    public boolean onMouse(int key, boolean pressed) {
        if (key == 0 && pressed && this.mx > this.x && this.mx < this.x + this.width && this.my > this.y && this.my < this.y + this.getHeight()) {
            this.setValue(!this.get());
            Managers.CONFIG.saveAll();
            return true;
        } else {
            return false;
        }
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
