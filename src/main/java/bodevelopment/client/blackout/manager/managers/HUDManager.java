package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.KeyEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.gui.clickgui.ClickGui;
import bodevelopment.client.blackout.hud.HudEditor;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.hud.elements.Arraylist;
import bodevelopment.client.blackout.manager.Manager;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.randomstuff.Pair;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.BOLogger;
import bodevelopment.client.blackout.util.ClassUtils;
import bodevelopment.client.blackout.util.SharedFeatures;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

import java.util.*;
import java.util.function.BiConsumer;

public class HUDManager extends Manager {
    public final HudEditor HUD_EDITOR = new HudEditor();
    private final List<Pair<String, Class<? extends HudElement>>> elements = new ArrayList<>();
    private final Map<Integer, HudElement> loaded = new HashMap<>();
    private final MatrixStack stack = new MatrixStack();
    private float progress = 0.0F;

    public List<Pair<String, Class<? extends HudElement>>> getElements() {
        return this.elements;
    }

    public Map<Integer, HudElement> getLoaded() {
        return this.loaded;
    }

    public Class<? extends HudElement> getClass(String name) {
        for (Pair<String, Class<? extends HudElement>> pair : this.elements) {
            if (pair.getLeft().equals(name)) {
                return pair.getRight();
            }
        }

        return null;
    }

    @Override
    public void init() {
        BlackOut.EVENT_BUS.subscribe(this, () -> false);

        String internalPath = HudElement.class.getCanonicalName().replace(HudElement.class.getSimpleName(), "elements");
        this.addHudObjects(internalPath, HudElement.class.getClassLoader());

        this.HUD_EDITOR.initElements();
    }

    private void addHudObjects(String path, ClassLoader loader) {
        if (path == null) return;
        ClassUtils.forEachClass(clazz -> {
            if (HudElement.class.isAssignableFrom(clazz)
                    && !clazz.isInterface()
                    && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {

                try {
                    Class<? extends HudElement> targetClazz = clazz.asSubclass(HudElement.class);

                    HudElement instance = ClassUtils.instance(targetClazz);
                    this.add(instance);
                } catch (Exception e) {
                    BOLogger.error("Error while adding HUD element: " + clazz.getName(), e);
                }
            }
        }, path, loader);
    }

    @Event
    public void onRender(RenderEvent.Hud.Pre event) {
        this.progress = this.getProgress((float) event.frameTime * 5.0F);
        Arraylist.updateDeltas();
        if (!(this.progress <= 0.0F) && !(BlackOut.mc.currentScreen instanceof HudEditor)) {
            this.start(this.stack);
            Renderer.setAlpha(this.progress);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.progress);
            this.render(this.stack, (float) event.frameTime);
            Renderer.setAlpha(1.0F);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            this.end(this.stack);
        }
    }

    public boolean add(Class<? extends HudElement> clazz) {
        if (clazz == null) return false;

        HudElement tempInstance = ClassUtils.instance(clazz);
        String name = tempInstance.name;
        boolean added = false;

        if (elements.stream().noneMatch(p -> p.getLeft().equals(name))) {
            this.elements.add(new Pair<>(name, clazz));
            added = true;
        } else {
            String fullName = clazz.getName();
            if (elements.stream().noneMatch(p -> p.getLeft().equals(fullName))) {
                this.elements.add(new Pair<>(fullName, clazz));
                added = true;
            }
        }

        return added;
    }

    @Event
    public void onKey(KeyEvent event) {
        if (event.key == 345 && event.pressed && BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (BlackOut.mc.currentScreen == null || BlackOut.mc.currentScreen instanceof HudEditor) {
                this.toggle();
            }
        }
    }

    private float getProgress(float delta) {
        Screen screen = BlackOut.mc.currentScreen;
        if (BlackOut.mc.player == null || BlackOut.mc.world == null) {
            return 0.0F;
        } else if (screen instanceof HudEditor) {
            return 1.0F;
        } else {
            return screen != null && (!(screen instanceof ClickGui) || Managers.CLICK_GUI.CLICK_GUI.isOpen()) && !SharedFeatures.shouldSilentScreen()
                    ? Math.max(this.progress - delta, 0.0F)
                    : Math.min(this.progress + delta, 1.0F);
        }
    }

    public void start(MatrixStack stack) {
        stack.push();
        float s = 1000.0F / BlackOut.mc.getFramebuffer().viewportWidth;
        RenderUtils.unGuiScale(stack);
        s = 1.0F / s;
        stack.scale(s, s, s);
    }

    public void render(MatrixStack stack, float frameTime) {
        Managers.HUD.forEachElement((id, element) -> element.renderElement(stack, frameTime));
    }

    public void clear() {
        this.loaded.values().forEach(HudElement::onRemove);
        this.loaded.clear();
    }

    public void remove(int id) {
        if (this.loaded.containsKey(id)) {
            this.loaded.remove(id).onRemove();
        }
    }

    public void end(MatrixStack stack) {
        stack.pop();
    }

    public void forEachElement(BiConsumer<? super Integer, ? super HudElement> consumer) {
        this.loaded.forEach(consumer);
    }

    public void add(HudElement element) {
        this.add(element.getClass());

        for (int i = 1; i < 1000; i++) {
            if (!this.loaded.containsKey(i)) {
                this.loaded.put(i, element);
                element.id = i;
                return;
            }
        }
    }

    private void toggle() {
        BlackOut.mc.setScreen(BlackOut.mc.currentScreen instanceof HudEditor ? null : this.HUD_EDITOR);
    }
}
