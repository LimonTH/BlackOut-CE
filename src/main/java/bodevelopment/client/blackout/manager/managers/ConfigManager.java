package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.ConfigType;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.gui.clickgui.ClickGui;
import bodevelopment.client.blackout.hud.HudEditor;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.util.ClassUtils;
import bodevelopment.client.blackout.util.FileUtils;
import com.google.gson.JsonObject;
import org.spongepowered.include.com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class ConfigManager extends Manager {
    private final String[] configs = new String[8];
    private final boolean[] toSave = new boolean[8];
    private long previousSave = 0L;

    @Override
    public void init() {
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
        File file = FileUtils.getFile("config.txt");
        if (file.exists()) {
            List<String> strings;
            try {
                strings = Files.readLines(file, Charset.defaultCharset());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (strings.size() >= this.getConfigs().length) {
                for (int i = 0; i < this.getConfigs().length; i++) {
                    this.getConfigs()[i] = strings.get(i);
                }
            } else {
                Arrays.fill(this.getConfigs(), "default");
            }
        } else {
            Arrays.fill(this.getConfigs(), "default");
        }

        this.set();
    }

    @Event
    public void onTick(TickEvent.Post event) {
        boolean shouldSave = false;

        for (boolean b : this.toSave) {
            if (b) {
                shouldSave = true;
                break;
            }
        }

        if (shouldSave
                && System.currentTimeMillis() > this.previousSave + 10000L
                && !(BlackOut.mc.currentScreen instanceof ClickGui)
                && !(BlackOut.mc.currentScreen instanceof HudEditor)) {
            this.writeCurrent();
        }
    }

    public void set() {
        File file = FileUtils.getFile("config.txt");
        FileUtils.addFile(file);
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < this.getConfigs().length; i++) {
            builder.append(this.getConfigs()[i]);
            if (i < this.getConfigs().length - 1) {
                builder.append("\n");
            }
        }

        FileUtils.write(file, builder.toString());
    }

    public void readConfigs() {
        FileUtils.addFolder("configs");

        for (ConfigType type : ConfigType.values()) {
            String configName = this.getConfigs()[type.ordinal()];
            if (!FileUtils.exists("configs", configName + ".json")) {
                this.getConfigs()[type.ordinal()] = "default";
            }
        }

        this.set();

        String mainConfigName = this.getConfigs()[0];

        for (ConfigType type : ConfigType.values()) {
            this.readConfig(this.getConfigs()[type.ordinal()], type);
        }

        this.readExtra(mainConfigName);
    }

    private void readExtra(String config) {
        JsonObject object = FileUtils.readElement("configs", config + ".json") instanceof JsonObject jsonObject ? jsonObject : null;
        Managers.HUD.clear();

        if (object == null) {
            object = new JsonObject();
            object.add("hud", new JsonObject());
        }

        if (object.has("hud")) {
            this.readHudElements(object.getAsJsonObject("hud"));
        }

        if (object.has("binds")) {
            JsonObject bindObject = object.getAsJsonObject("binds");
            Managers.MODULES.getModules().forEach(module -> {
                if (bindObject.has(module.getFileName())) {
                    module.bind.read(bindObject.getAsJsonObject(module.getFileName()));
                }
            });
        }
    }

    public void readConfig(String config, ConfigType type) {
        FileUtils.addFile("configs", config + ".json");
        JsonObject object;
        if (FileUtils.readElement("configs", config + ".json") instanceof JsonObject jsonObject) {
            object = jsonObject;
        } else {
            object = new JsonObject();
        }

        switch (type) {
            case Combat:
            case Movement:
            case Visual:
            case Misc:
            case Legit:
            case Client:
                if (object.get(type.name()) instanceof JsonObject moduleObject) {
                    Managers.MODULES.getModules().stream().filter(type.predicate).forEach(module -> this.readModule(module, moduleObject));
                } else {
                    Managers.MODULES.getModules().stream().filter(type.predicate).forEach(module -> {
                        module.settingGroups.forEach(group -> group.settings.forEach(Setting::reset));
                        module.enabled = false;
                    });
                }
                break;
        }
    }

    private void readHudElements(JsonObject jsonObject) {
        jsonObject.asMap().forEach((property, element) -> {
            if (element instanceof JsonObject object) {
                String[] strings = property.split("-");
                this.readModule(strings[0], Integer.parseInt(strings[1]), object);
            }
        });
    }

    private void readModule(Module module, JsonObject jsonObject) {
        if (jsonObject.get(module.getFileName()) instanceof JsonObject object) {
            if (object.has("enabled")) {
                module.enabled = object.get("enabled").getAsBoolean();
            } else {
                module.enabled = false;
            }

            module.readSettings(object);
        } else {
            module.settingGroups.forEach(group -> group.settings.forEach(Setting::reset));
            module.enabled = false;
            this.writeModule(module, jsonObject);
        }
    }

    private void readModule(String name, int id, JsonObject jsonObject) {
        Class<? extends HudElement> element = Managers.HUD.getClass(name);
        if (element != null) {
            HudElement hudElement = ClassUtils.instance(element);
            Managers.HUD.getLoaded().put(id, hudElement);
            hudElement.id = id;
            if (jsonObject.has("enabled")) {
                hudElement.enabled = jsonObject.get("enabled").getAsBoolean();
            } else {
                hudElement.enabled = false;
            }

            float scale = 0.5625F;
            hudElement.x = jsonObject.has("positionX") ? jsonObject.get("positionX").getAsFloat() : 500.0F;
            hudElement.y = jsonObject.has("positionY") ? jsonObject.get("positionY").getAsFloat() : 500.0F * scale;
            if (jsonObject.get("settings") instanceof JsonObject object) {
                hudElement.readSettings(object);
            } else {
                hudElement.forEachSetting(Setting::reset);
                this.writeHudElement(hudElement, id, jsonObject);
            }
        }
    }

    public void writeCurrent() {
        for (ConfigType configType : ConfigType.values()) {
            this.writeConfig(this.getConfigs()[configType.ordinal()], configType);
        }
    }

    public void writeConfig(String name, ConfigType type) {
        JsonObject configObject = new JsonObject();
        configObject.addProperty("description", "BlackOut Config");

        LocalDateTime time = LocalDateTime.now(ZoneOffset.UTC);
        JsonObject timeObject = new JsonObject();
        timeObject.addProperty("year", time.getYear());
        timeObject.addProperty("month", time.getMonthValue());
        timeObject.addProperty("day", time.getDayOfMonth());
        timeObject.addProperty("hour", time.getHour());
        timeObject.addProperty("minute", time.getMinute());
        configObject.add("lastSave", timeObject);

        for (ConfigType configType : ConfigType.values()) {
            if (configType.predicate != null) {
                JsonObject categoryObject = new JsonObject();
                Managers.MODULES.getModules().stream()
                        .filter(configType.predicate)
                        .forEach(module -> {
                            JsonObject moduleJson = new JsonObject();
                            this.writeSettings(module, moduleJson);
                            categoryObject.add(module.getFileName(), moduleJson);
                        });
                configObject.add(configType.name(), categoryObject);
            }
        }

        JsonObject hudObject = new JsonObject();
        Managers.HUD.forEachElement((id, element) -> {
            this.writeHudElement(element, id, hudObject);
        });
        configObject.add("hud", hudObject);

        JsonObject bindsObject = new JsonObject();
        Managers.MODULES.getModules().forEach(module -> {
            JsonObject bJson = new JsonObject();
            module.bind.write(bJson);
            bindsObject.add(module.getFileName(), bJson);
        });
        configObject.add("binds", bindsObject);

        FileUtils.write(FileUtils.getFile("configs", name + ".json"), configObject);

        this.previousSave = System.currentTimeMillis();
        Arrays.fill(this.toSave, false);
    }

    private void writeHudElement(HudElement element, int id, JsonObject jsonObject) {
        String key = element.getClass().getSimpleName() + "-" + id;

        JsonObject object = new JsonObject();
        object.addProperty("enabled", element.enabled);
        object.addProperty("positionX", element.x);
        object.addProperty("positionY", element.y);

        JsonObject settingsObject = new JsonObject();
        element.writeSettings(settingsObject);
        object.add("settings", settingsObject);

        jsonObject.add(key, object);
    }

    private void writeModule(Module module, JsonObject jsonObject) {
        String fileName = module.getFileName();
        JsonObject object = new JsonObject();
        jsonObject.add(fileName, object);
        this.writeSettings(module, object);
    }

    public void writeSettings(HudElement element, JsonObject jsonObject) {
        element.writeSettings(jsonObject);
    }

    public void writeSettings(Module module, JsonObject jsonObject) {
        jsonObject.addProperty("enabled", module.enabled);
        module.writeSettings(jsonObject);
    }

    public void save(ConfigType type) {
        this.toSave[type.ordinal()] = true;
    }

    public void saveAll() {
        Arrays.fill(this.toSave, true);
    }

    public void saveModule(Module module) {
        for (ConfigType configType : ConfigType.values()) {
            Predicate<Module> predicate = configType.predicate;
            if (predicate != null && predicate.test(module)) {
                this.save(configType);
            }
        }
    }

    public String[] getConfigs() {
        return this.configs;
    }
}
