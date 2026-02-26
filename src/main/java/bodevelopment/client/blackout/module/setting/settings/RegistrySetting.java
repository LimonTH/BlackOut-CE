package bodevelopment.client.blackout.module.setting.settings;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.interfaces.functional.EpicInterface;
import bodevelopment.client.blackout.interfaces.functional.SingleOut;
import bodevelopment.client.blackout.util.GuiColorUtils;
import com.google.gson.JsonElement;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

public class RegistrySetting<T> extends ListSetting<T> {
    private static final Map<Registry<?>, List<?>> registries = new HashMap<>();
    private final Registry<T> registry;

    @SafeVarargs
    public RegistrySetting(String name, Registry<T> registry, EpicInterface<T, String> getName, String description, SingleOut<Boolean> visible, Predicate<T> filter, T... val) {
        super(name, getFilteredList(registry, filter), getName, description, visible, val);
        this.registry = registry;
    }

    @SafeVarargs
    public RegistrySetting(String name, Registry<T> registry, EpicInterface<T, String> getName, String description, SingleOut<Boolean> visible, T... val) {
        super(name, getList(registry), getName, description, visible, val);
        this.registry = registry;
    }

    private static <T> List<T> getFilteredList(Registry<T> registry, Predicate<T> filter) {
        return registry.getEntrySet().stream()
                .map(Entry::getValue)
                .filter(filter)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> getList(Registry<T> registry) {
        if (registries.containsKey(registry)) {
            return (List<T>) registries.get(registry);
        } else {
            List<T> list = new ArrayList<>(registry.getEntrySet().stream().map(Entry::getValue).toList());
            registries.put(registry, list);
            return list;
        }
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
        float textWidth = BlackOut.FONT.getWidth(text) * textScale;

        BlackOut.FONT.text(
                this.stack,
                text,
                textScale,
                valueX - textWidth,
                nameY,
                GuiColorUtils.getSettingText(this.y),
                false,
                true
        );

        return this.getHeight();
    }

    @Override
    protected String join() {
        StringBuilder builder = new StringBuilder();
        boolean b = false;

        for (T item : this.get()) {
            if (!b) {
                b = true;
            } else {
                builder.append(",");
            }

            builder.append(this.registry.getId(item));
        }

        return builder.toString();
    }

    @Override
    public void set(JsonElement element) {
        this.get().clear();

        for (String string : element.getAsString().split(",")) {
            Identifier id = Identifier.of(string);
            if (this.registry.containsId(id)) {
                this.get().add(this.registry.get(id));
            }
        }
        this.checkChange();
    }

    public Registry<T> getRegistry() {
        return this.registry;
    }
}
