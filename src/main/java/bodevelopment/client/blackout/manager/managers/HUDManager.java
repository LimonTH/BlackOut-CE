package bodevelopment.client.blackout.manager.managers;

import bodevelopment.client.blackout.util.PlayerUtils;
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
import bodevelopment.client.blackout.util.render.Render2DUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.client.gui.screens.Screen;

public class HUDManager extends Manager {
    public final HudEditor HUD_EDITOR = new HudEditor();
    private final List<Pair<String, Class<? extends HudElement>>> elements = new ArrayList<>();
    private final Map<Integer, HudElement> loaded = new HashMap<>();
    private final PoseStack stack = new PoseStack();
    private float progress = 0.0F;

    public List<Pair<String, Class<? extends HudElement>>> getElements() {
        return this.elements;
    }

    public Map<Integer, HudElement> getLoaded() {
        return this.loaded;
    }

    public Class<? extends HudElement> getClass(String name) {
        for (Pair<String, Class<? extends HudElement>> pair : this.elements) {
            if (pair.getA().equals(name) || pair.getB().getSimpleName().equals(name)) {
                return pair.getB();
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
                    this.add(targetClazz);
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
        if (!(this.progress <= 0.0F) && !(HudEditor.isOpen())) {
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

        if (elements.stream().noneMatch(p -> p.getA().equals(name))) {
            this.elements.add(new Pair<>(name, clazz));
            added = true;
        } else {
            String fullName = clazz.getName();
            if (elements.stream().noneMatch(p -> p.getA().equals(fullName))) {
                this.elements.add(new Pair<>(fullName, clazz));
                added = true;
            }
        }

        return added;
    }

    @Event
    public void onKey(KeyEvent event) {
        if (event.key == 345 && event.pressed && PlayerUtils.isInGame()) {
            if (BlackOut.mc.screen == null || HudEditor.isOpen()) {
                this.toggle();
            }
        }
    }

    private float getProgress(float delta) {
        Screen screen = BlackOut.mc.screen;
        if (!PlayerUtils.isInGame()) {
            return 0.0F;
        } else if (screen instanceof HudEditor) {
            return 1.0F;
        } else {
            return screen != null && (!(screen instanceof ClickGui) || Managers.CLICK_GUI.CLICK_GUI.isOpen()) && !SharedFeatures.shouldSilentScreen()
                    ? Math.max(this.progress - delta, 0.0F)
                    : Math.min(this.progress + delta, 1.0F);
        }
    }

    public void start(PoseStack stack) {
        stack.pushPose();
        float s = 1000.0F / BlackOut.mc.getMainRenderTarget().viewWidth;
        Render2DUtils.unGuiScale(stack);
        s = 1.0F / s;
        stack.scale(s, s, s);
        // HUD is 2D — depth writes would pollute the depth buffer and cause other
        // HUD renders (vanilla / other mods) to fail the depth test in our areas.
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
    }

    public void render(PoseStack stack, float frameTime) {
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

    public void end(PoseStack stack) {
        stack.popPose();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    public void forEachElement(BiConsumer<? super Integer, ? super HudElement> consumer) {
        this.loaded.forEach(consumer);
    }

    public void add(HudElement element) {
        this.add(element.getClass());

        int newId = 0;
        while (this.loaded.containsKey(newId)) {
            newId++;
        }

        element.id = newId;
        this.loaded.put(newId, element);
    }

    private void toggle() {
        BlackOut.mc.setScreen(HudEditor.isOpen() ? null : this.HUD_EDITOR);
    }
}
