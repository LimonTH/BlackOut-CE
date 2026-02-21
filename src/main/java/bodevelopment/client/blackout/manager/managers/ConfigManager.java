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
                && !Managers.MODULES.getModules().isEmpty()
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
            this.readConfig(this.getConfigs()[type.ordinal()], type);
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
            case HUD:
                Managers.HUD.clear();
                if (object.has("hud") && object.get("hud") instanceof JsonObject hudObject) {
                    this.readHudElements(hudObject);
                }
                break;
            case Binds:
                if (object.has("binds") && object.get("binds") instanceof JsonObject bindObject) {
                    Managers.MODULES.getModules().forEach(module -> {
                        if (bindObject.has(module.getFileName()) && bindObject.get(module.getFileName()) instanceof JsonObject moduleObject) {
                            module.bind.read(moduleObject);
                        } else {
                            module.bind.reset();
                        }
                    });
                } else {
                    Managers.MODULES.getModules().forEach(module -> module.bind.reset());
                }
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
                hudElement.enabled = true;
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
        FileUtils.addFile("configs", name + ".json");
        JsonObject prevObject;
        if (FileUtils.readElement("configs", name + ".json") instanceof JsonObject object) {
            prevObject = object;
        } else {
            prevObject = null;
        }

        boolean prevFound = prevObject != null;
        JsonObject configObject = new JsonObject();
        configObject.addProperty("description", prevFound && prevObject.has("description") ? prevObject.get("description").getAsString() : "");
        JsonObject timeObject = new JsonObject();
        LocalDateTime time = LocalDateTime.now(ZoneOffset.UTC);
        timeObject.addProperty("year", time.getYear());
        timeObject.addProperty("month", time.getMonthValue());
        timeObject.addProperty("day", time.getDayOfMonth());
        timeObject.addProperty("hour", time.getHour());
        timeObject.addProperty("minute", time.getMinute());
        timeObject.addProperty("second", time.getSecond());
        configObject.add("lastSave", timeObject);
        JsonObject moduleObject = new JsonObject();
        if (type.predicate != null) {
            Managers.MODULES.getModules().stream().filter(type.predicate).forEach(module -> this.writeModule(module, moduleObject));
        }

        for (ConfigType configType : ConfigType.values()) {
            if (configType.predicate != null) {
                String key = configType.name();
                if (configType == type) {
                    configObject.add(key, moduleObject);
                } else if (prevFound && prevObject.has(key)) {
                    if (prevObject.get(key) instanceof JsonObject object) {
                        configObject.add(key, object);
                    } else {
                        configObject.add(key, new JsonObject());
                    }
                } else {
                    configObject.add(key, new JsonObject());
                }
            }
        }

        if (type == ConfigType.HUD) {
            JsonObject hudObject = new JsonObject();
            Managers.HUD.forEachElement((id, element) -> this.writeHudElement(element, id, hudObject));
            configObject.add("hud", hudObject);
        } else if (prevFound && prevObject.has("hud") && prevObject.get("hud") instanceof JsonObject object) {
            configObject.add("hud", object);
        } else {
            configObject.add("hud", new JsonObject());
        }

        if (type == ConfigType.Binds) {
            JsonObject bindObject = new JsonObject();
            Managers.MODULES.getModules().forEach(module -> {
                JsonObject jsonObject = new JsonObject();
                module.bind.write(jsonObject);
                bindObject.add(module.getFileName(), jsonObject);
            });
            configObject.add("binds", bindObject);
        } else if (prevFound && prevObject.has("binds") && prevObject.get("binds") instanceof JsonObject object) {
            configObject.add("binds", object);
        } else {
            configObject.add("binds", new JsonObject());
        }

        FileUtils.write(FileUtils.getFile("configs", name + ".json"), configObject);
        this.previousSave = System.currentTimeMillis();
        this.toSave[type.ordinal()] = false;
    }

    private void writeHudElement(HudElement element, int id, JsonObject jsonObject) {
        String fileName = element.getClass().getSimpleName();
        JsonObject object = new JsonObject();
        JsonObject settingsObject = new JsonObject();
        object.addProperty("enabled", element.enabled);
        object.addProperty("positionX", element.x);
        object.addProperty("positionY", element.y);
        this.writeSettings(element, settingsObject);
        object.add("settings", settingsObject);
        jsonObject.add(fileName + "-" + id, object);
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
