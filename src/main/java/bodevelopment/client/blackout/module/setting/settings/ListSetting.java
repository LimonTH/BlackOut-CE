package bodevelopment.client.blackout.module.setting.settings;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.clickgui.screens.ListScreen;
import bodevelopment.client.blackout.interfaces.functional.EpicInterface;
import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.util.GuiColorUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;

public class ListSetting<T> extends Setting<List<T>> {
    public final List<T> list;
    private final EpicInterface<T, String> getName;

    @SafeVarargs
    public ListSetting(String name, List<T> list, EpicInterface<T, String> getName, String description, SingleOut<Boolean> visible, T... val) {
        super(name, new ArrayList<>(Arrays.asList(val)), description, visible);
        this.list = list;
        this.getName = getName;
    }

    @Override
    public float render() {
        float textScale = 2.0F;
        float baseH = 26.0F;
        float middleY = this.y + (baseH / 2.0F);

        float fontHeight = BlackOut.FONT.getHeight() * textScale;
        float nameY = middleY - (fontHeight / 2.0F);

        BlackOut.FONT.text(this.stack, this.name, textScale, this.x + 5.0F, nameY, GuiColorUtils.getSettingText(this.y), false, true);

        String text = String.valueOf(this.get().size());

        float valueX = this.x + this.width - 10.0F;

        BlackOut.FONT.text(
                this.stack,
                text,
                textScale,
                valueX - (BlackOut.FONT.getWidth(text) * textScale),
                nameY,
                GuiColorUtils.getSettingText(this.y),
                false,
                true
        );

        return this.getHeight();
    }

    @Override
    public boolean onMouse(int key, boolean pressed) {
        if (key == 0 && pressed && this.mx > this.x && this.mx < this.x + this.width && this.my > this.y && this.my < this.y + this.getHeight()) {
            Managers.CLICK_GUI.openScreen(new ListScreen<>(this, this.getName));
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
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty(this.name, this.join());
    }

    protected String join() {
        StringBuilder builder = new StringBuilder();
        boolean b = false;

        for (T item : this.get()) {
            if (!b) {
                b = true;
            } else {
                builder.append(",");
            }

            builder.append(this.getName.get(item));
        }

        return builder.toString();
    }

    @Override
    public void set(JsonElement element) {
        this.get().clear();
        Map<String, T> names = new HashMap<>();
        this.list.forEach(item -> names.put(this.getName.get(item), item));

        for (String string : element.getAsString().split(",")) {
            if (names.containsKey(string)) {
                this.get().add(names.get(string));
            }
        }
        this.checkChange();
    }
}
