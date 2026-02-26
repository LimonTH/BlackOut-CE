package bodevelopment.client.blackout.module.setting.settings;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.TextField;
import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.SelectedComponent;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.awt.*;

public class StringSetting extends Setting<String> {
    private final TextField textField = new TextField();
    private final int id = SelectedComponent.nextId();

    public StringSetting(String name, String val, String description, SingleOut<Boolean> visible) {
        super(name, val, description, visible);
    }

    @Override
    public float render() {
        float textScale = 2.0F;
        float baseH = 26.0F;
        float middleY = this.y + (baseH / 2.0F);

        float fontHeight = BlackOut.FONT.getHeight() * textScale;
        float nameY = middleY - (fontHeight / 2.0F);

        BlackOut.FONT.text(this.stack, this.name, textScale, this.x + 5.0F, nameY, GuiColorUtils.getSettingText(this.y), false, true);

        float fieldWidth = this.width - 20.0F;
        float fieldX = this.x + 10.0F;
        float fieldY = this.y + 28.0F;

        this.textField.setActive(SelectedComponent.is(this.id));
        this.textField.render(
                this.stack,
                1.8F,
                this.mx,
                this.my,
                fieldX,
                fieldY,
                fieldWidth,
                15.0F,
                4.0F,
                6.0F,
                Color.WHITE,
                GuiColorUtils.bg2
        );

        return this.getHeight();
    }

    @Override
    public boolean onMouse(int button, boolean pressed) {
        if (button == 0 && pressed) {
            boolean overSetting = this.mx > this.x && this.mx < this.x + this.width &&
                    this.my > this.y && this.my < this.y + this.getHeight();

            if (overSetting) {
                if (this.textField.click(button, true) || this.my > this.y + 25) {
                    SelectedComponent.setId(this.id);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onKey(int key, boolean pressed) {
        if (SelectedComponent.is(this.id)) {
                this.textField.type(key, pressed);
                this.setValue(this.textField.getContent());
                Managers.CONFIG.saveAll();
        }
    }

    @Override
    public float getHeight() {
        return 55.0F;
    }

    @Override
    public void write(JsonObject object) {
        object.addProperty(this.name, this.get());
    }

    @Override
    public void set(JsonElement element) {
        this.setValue(element.getAsString());
        this.textField.setContent(this.get());
    }

    public int getId() {
        return this.id;
    }
}
