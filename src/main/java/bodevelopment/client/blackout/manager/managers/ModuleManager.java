package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.addon.AddonLoader;
import bodevelopment.client.blackout.enums.BindMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.KeyEvent;
import bodevelopment.client.blackout.event.events.MouseButtonEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.hud.elements.Arraylist;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.OnlyDev;
import bodevelopment.client.blackout.util.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModuleManager extends Manager {
    private final List<Module> modules = new ArrayList<>();

    @Override
    public void init() {
        this.modules.clear();

        long time = OLEPOSSUtils.testTime(() -> {
            String internalPath = Module.class.getCanonicalName().replace(Module.class.getSimpleName(), "modules");

            List<Module> internalModules = new ArrayList<>();
            this.addModuleObjects(internalPath, internalModules, Module.class.getClassLoader());

            internalModules.stream()
                    .sorted(Comparator.comparing(o -> o.name))
                    .forEach(this::add);
        });

        AddonLoader.load();

        BOLogger.info(String.format("Initializing %s modules took %sms", this.modules.size(), time));

        BlackOut.EVENT_BUS.subscribe(this, () -> BlackOut.mc.currentScreen != null || SharedFeatures.shouldSilentScreen());
        SettingUtils.init();
        SharedFeatures.init();
    }

    @Event
    public void onKey(KeyEvent event) {
        this.modules.forEach(m -> {
            if (m.bindMode.get() == BindMode.Toggle && m.bind.get().isKey(event.key) && event.pressed) {
                m.toggle();
            }
        });
    }

    @Event
    public void onMouse(MouseButtonEvent event) {
        this.modules.forEach(m -> {
            if (m.bindMode.get() == BindMode.Toggle && m.bind.get().isMouse(event.button) && event.pressed) {
                m.toggle();
            }
        });
    }

    @Event
    public void onRender(RenderEvent.World.Pre event) {
        this.modules.forEach(m -> {
            if (m.bindMode.get() == BindMode.Pressed) {
                if (m.bind.get().value != null && m.bind.get().isPressed()) {
                    m.enable();
                } else {
                    m.disable();
                }
            }
        });
    }

    public void add(Module module) {
        if (!modules.contains(module)) {
            this.modules.add(module);
            Arraylist.deltaMap.put(module, new org.apache.commons.lang3.mutable.MutableFloat(0.0F));
        }
    }

    private void addModuleObjects(String path, List<Module> list, ClassLoader loader) {
        if (path == null) return;
        ClassUtils.forEachClass(clazz -> {
            if (Module.class.isAssignableFrom(clazz)
                    && !clazz.isInterface()
                    && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {

                Class<? extends Module> moduleClass = clazz.asSubclass(Module.class);

                if (BlackOut.TYPE.isDevBuild() || !moduleClass.isAnnotationPresent(OnlyDev.class)) {
                    list.add(ClassUtils.instance(moduleClass));
                }
            }
        }, path, loader);
    }

    public final List<Module> getModules() {
        return this.modules;
    }
}
