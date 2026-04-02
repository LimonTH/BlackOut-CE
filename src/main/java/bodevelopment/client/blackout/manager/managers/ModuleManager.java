package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.BindMode;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.KeyEvent;
import bodevelopment.client.blackout.event.events.MouseButtonEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.hud.elements.Arraylist;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.module.AbstractModule;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.annotations.OnlyDev;
import bodevelopment.client.blackout.util.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModuleManager extends Manager {
    private final List<AbstractModule> modules = new ArrayList<>();

    @Override
    public void init() {
        long time = TimeUtils.measure(() -> {
            String internalPath = AbstractModule.class.getCanonicalName().replace(AbstractModule.class.getSimpleName(), "modules");

            List<AbstractModule> internalModules = new ArrayList<>();
            this.addModuleObjects(internalPath, internalModules, AbstractModule.class.getClassLoader());

            internalModules.stream()
                    .sorted(Comparator.comparing(o -> o.name))
                    .forEach(this::add);
        });
        BOLogger.info(String.format("Initializing %s modules took %sms", this.modules.size(), time));

        BlackOut.EVENT_BUS.subscribe(this, () -> BlackOut.mc.screen != null || SharedFeatures.shouldSilentScreen());
        SettingUtils.init();
        SharedFeatures.init();
    }

    @Event
    public void onKey(KeyEvent event) {
        this.modules.forEach(m -> {
            if (m instanceof Module module
                    && module.bindMode.get() == BindMode.Toggle
                    && module.bind.get().isKey(event.key)
                    && event.pressed) {
                module.toggle();
            }
        });
    }

    @Event
    public void onMouse(MouseButtonEvent event) {
        this.modules.forEach(m -> {
            if (m instanceof Module module
                    && module.bindMode.get() == BindMode.Toggle
                    && module.bind.get().isMouse(event.button)
                    && event.pressed) {
                module.toggle();
            }
        });
    }

    @Event
    public void onRender(RenderEvent.World.Pre event) {
        this.modules.forEach(m -> {
            if (m instanceof Module module && module.bindMode.get() == BindMode.Pressed) {
                if (module.bind.get().value != null && module.bind.get().isPressed()) {
                    module.enable();
                } else {
                    module.disable();
                }
            }
        });
    }

    public void add(AbstractModule module) {
        if (module == null) return;

        boolean alreadyExists = this.modules.stream()
                .anyMatch(m -> m.getClass().equals(module.getClass()));

        if (!alreadyExists) {
            this.modules.add(module);
            if (module instanceof Module toggleable) {
                Arraylist.deltaMap.put(toggleable, new org.apache.commons.lang3.mutable.MutableFloat(0.0F));
            }
        }
    }

    private void addModuleObjects(String path, List<AbstractModule> list, ClassLoader loader) {
        if (path == null) return;
        ClassUtils.forEachClass(clazz -> {
            if (AbstractModule.class.isAssignableFrom(clazz)
                    && !clazz.isInterface()
                    && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {

                Class<? extends AbstractModule> moduleClass = clazz.asSubclass(AbstractModule.class);

                if (BlackOut.TYPE.isDevBuild() || !moduleClass.isAnnotationPresent(OnlyDev.class)) {
                    list.add(ClassUtils.instance(moduleClass));
                }
            }
        }, path, loader);
    }

    public final List<AbstractModule> getModules() {
        return this.modules;
    }

    public <T extends AbstractModule> T getModule(Class<T> clazz) {
        for (AbstractModule module : this.modules) {
            if (module.getClass() == clazz) {
                return clazz.cast(module);
            }
        }
        return null;
    }

    /**
     * Returns only the toggleable modules (excludes SettingsModules).
     */
    public final List<Module> getToggleableModules() {
        return this.modules.stream()
                .filter(Module.class::isInstance)
                .map(Module.class::cast)
                .toList();
    }
}
