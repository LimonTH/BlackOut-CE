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
import bodevelopment.client.blackout.util.ClassUtils;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import bodevelopment.client.blackout.util.SettingUtils;
import bodevelopment.client.blackout.util.SharedFeatures;
import org.apache.commons.lang3.mutable.MutableFloat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModuleManager extends Manager {
    private final List<Module> modules = new ArrayList<>();

    @Override
    public void init() {
        this.initModules();
        this.modules.forEach(module -> Arraylist.deltaMap.put(module, new MutableFloat(0.0F)));
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

    private void initModules() {
        this.modules.clear();
        long time = OLEPOSSUtils.testTime(() -> this.getAllModuleObjects().stream().sorted(Comparator.comparing(o -> o.name)).forEach(this.modules::add));
        System.out.printf("Initializing %s modules took %sms %n", this.modules.size(), time);
    }

    private List<Module> getAllModuleObjects() {
        List<Module> list = new ArrayList<>();
        this.addModuleObjects(Module.class.getCanonicalName().replace(Module.class.getSimpleName(), "modules"), list);
        AddonLoader.addons.forEach(addon -> this.addModuleObjects(addon.modulePath, list));
        return list;
    }

    private void addModuleObjects(String path, List<Module> list) {
        ClassUtils.forEachClass(clazz -> {
            if (Module.class.isAssignableFrom(clazz)) {
                Class<? extends Module> moduleClass = clazz.asSubclass(Module.class);

                if (BlackOut.TYPE.isDevBuild() || !moduleClass.isAnnotationPresent(OnlyDev.class)) {
                    list.add(ClassUtils.instance(moduleClass));
                }
            }
        }, path);
    }

    public final List<Module> getModules() {
        return this.modules;
    }
}
