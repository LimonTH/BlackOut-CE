package bodevelopment.client.blackout.module.setting.settings;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.keys.Key;
import bodevelopment.client.blackout.keys.KeyBind;
import bodevelopment.client.blackout.keys.MouseButton;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.SelectedComponent;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class KeyBindSetting extends Setting<KeyBind> {
    private final int id = SelectedComponent.nextId();

    public KeyBindSetting(String name, String description, SingleOut<Boolean> visible) {
        super(name, new KeyBind(null), description, visible);
    }

    @Override
    public float render() {
        float textScale = 2.0F;
        float baseH = 26.0F;
        float middleY = this.y + (baseH / 2.0F);

        float fontHeight = BlackOut.FONT.getHeight() * textScale;
        float nameY = middleY - (fontHeight / 2.0F);
        BlackOut.FONT.text(this.stack, this.name, textScale, this.x + 5.0F, nameY, GuiColorUtils.getSettingText(this.y), false, true);
        this.get().render(this.stack, this.x + this.width - 21.0F, this.y + 10, this.x + this.width, this.mx, this.my);

        return this.getHeight();
    }

    @Override
    public boolean onMouse(int key, boolean pressed) {
        float oldMy = (float) this.my;
        this.my += 5.5F;
        boolean interacted = this.get().onMouse(key, pressed);

        this.my = oldMy;

        if (interacted) {
            SelectedComponent.setId(this.id);
        }
        return interacted;
    }

    @Override
    public void onKey(int key, boolean pressed) {
        this.get().onKey(key, pressed);
        if (key == 256 || key == 344 || !this.get().isBinding()) {
            if (SelectedComponent.is(this.id)) {
                SelectedComponent.reset();
            }
        }
    }

    @Override
    public float getHeight() {
        return 28.0F;
    }

    @Override
    public void write(JsonObject object) {
        if (this.get().value == null) {
            object.addProperty(this.name, "<NULL>");
        } else {
            object.addProperty(this.name, (this.get().value instanceof Key ? "k+" : "m+") + this.get().value.key);
        }
    }

    @Override
    public void set(JsonElement element) {
        String string = element.getAsString();
        if (string.equals("<NULL>")) {
            this.setValue(new KeyBind(null));
        } else {
            String[] strings = string.split("\\+");
            String bindType = strings[0];
            switch (bindType) {
                case "k":
                    this.setValue(new KeyBind(new Key(Integer.parseInt(strings[1]))));
                    break;
                case "m":
                    this.setValue(new KeyBind(new MouseButton(Integer.parseInt(strings[1]))));
                    break;
                default:
                    this.setValue(new KeyBind(null));
            }
        }
    }

    public int getId() {
        return this.id;
    }
}