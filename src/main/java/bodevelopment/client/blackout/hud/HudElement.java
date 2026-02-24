package bodevelopment.client.blackout.hud;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.ConfigType;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.rendering.renderer.ColorRenderer;
import com.google.gson.JsonObject;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class HudElement {
    public final List<SettingGroup> settingGroups = new ArrayList<>();
    public final String name;
    public final String description;
    protected final SettingGroup sgScale = this.addGroup("Scale");
    private final Setting<Double> scale = this.sgScale.doubleSetting("Scale", 1.0, 0.1, 10.0, 0.1, ".");
    public float x = 0.0F;
    public float y = 0.0F;
    public int id;
    public boolean enabled = true;
    public long toggleTime = 0L;
    protected float frameTime;
    protected MatrixStack stack;
    private float width = 0.0F;
    private float height = 0.0F;

    public HudElement(String name, String description) {
        this.name = name;
        this.description = description;
    }

    protected float getScale() {
        return this.scale.get().floatValue();
    }

    public void toggle() {
        Managers.CONFIG.save(ConfigType.HUD);
        this.enabled = !this.enabled;
    }

    protected void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public void renderElement(MatrixStack stack, float frameTime) {
        if (this.enabled || BlackOut.mc.currentScreen instanceof HudEditor) {
            this.frameTime = frameTime;
            this.stack = stack;
            this.pushStack(stack);
            this.render();
            this.popStack(stack);
        }
    }

    public void renderQuad(MatrixStack stack, boolean selected) {
        this.pushStack(stack);
        ColorRenderer renderer = ColorRenderer.getInstance();
        float r = 1.0F;
        float g = this.enabled ? 1.0F : 0.0F;
        float b = this.enabled ? 1.0F : 0.0F;
        renderer.quad(stack, 0.0F, 0.0F, 0.0F, this.width, this.height, r, g, b, selected ? 0.1F : 0.05F);
        renderer.startRender(stack, VertexFormat.DrawMode.DEBUG_LINE_STRIP);
        renderer.quadOutlineShape(0.0F, 0.0F, 0.0F, this.width, this.height, r, g, b, selected ? 1.0F : 0.5F);
        renderer.endRender();
        this.popStack(stack);
    }

    private void pushStack(MatrixStack stack) {
        stack.push();
        stack.translate(this.x, this.y, 0.0F);
        stack.scale(this.getScale(), this.getScale(), 0.0F);
    }

    private void popStack(MatrixStack stack) {
        stack.pop();
    }

    protected void render() {
    }

    public void onRemove() {
    }

    protected SettingGroup addGroup(String name) {
        SettingGroup group = new SettingGroup(name);
        this.settingGroups.add(group);
        return group;
    }

    public void readSettings(JsonObject jsonObject) {
        this.forEachSetting(s -> s.read(jsonObject));
    }

    public void writeSettings(JsonObject jsonObject) {
        this.forEachSetting(s -> s.write(jsonObject));
    }

    public void forEachSetting(Consumer<? super Setting<?>> consumer) {
        this.settingGroups.forEach(group -> group.settings.forEach(consumer));
    }

    public float getWidth() {
        return this.width * this.getScale();
    }

    public float getHeight() {
        return this.height * this.getScale();
    }

    protected static class Component {
        public final String text;
        public final float width;
        public final Color color;
        public final boolean bold;

        public Component(String text, Color color) {
            this.text = text;
            this.width = BlackOut.FONT.getWidth(text);
            this.color = color;
            this.bold = false;
        }

        public Component(String text) {
            this.text = text;
            this.width = BlackOut.FONT.getWidth(text);
            this.color = null;
            this.bold = false;
        }

        public Component(String text, Boolean bold) {
            this.text = text;
            this.width = bold ? BlackOut.BOLD_FONT.getWidth(text) : BlackOut.FONT.getWidth(text);
            this.color = null;
            this.bold = bold;
        }

        public Component(String text, Color color, Boolean bold) {
            this.text = text;
            this.width = bold ? BlackOut.BOLD_FONT.getWidth(text) : BlackOut.FONT.getWidth(text);
            this.color = color;
            this.bold = bold;
        }
    }
}
