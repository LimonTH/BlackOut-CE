package bodevelopment.client.blackout.hud;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.PublicAPI;
import bodevelopment.client.blackout.enums.ConfigType;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.rendering.renderer.ColorRenderer;
import bodevelopment.client.blackout.util.PlayerUtils;
import bodevelopment.client.blackout.util.render.RenderLayer;
import bodevelopment.client.blackout.util.render.RenderState;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@PublicAPI
public class HudElement {
    public final List<SettingGroup> settingGroups = new ArrayList<>();

    protected final SettingGroup sgScale = this.addGroup("Sizing");

    private final Setting<Double> scale = this.sgScale.doubleSetting("Master Scale", 1.0, 0.1, 10.0, 0.1, "The global scale multiplier applied to this interface element.");

    public final String name;
    public final String description;
    public float x = 0.0F;
    public float y = 0.0F;
    public int id;
    public boolean enabled = true;
    public long toggleTime = 0L;
    protected float frameTime;
    protected PoseStack stack;
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

    private boolean sizeChangedInRender = false;

    protected void setSize(float width, float height) {
        this.width = width;
        this.height = height;
        this.sizeChangedInRender = true;
    }

    public void renderElement(PoseStack stack, float frameTime) {
        if (this.enabled || HudEditor.isEditing()) {
            this.frameTime = frameTime;
            this.stack = stack;
            this.pushStack(stack);
            this.sizeChangedInRender = false;
            this.render();
            boolean sizeWasNotSet = !this.sizeChangedInRender;
            if (sizeWasNotSet) {
                float placeholderWidth = BlackOut.FONT.getWidth(this.name) * this.getScale() + 8.0F;
                float placeholderHeight = BlackOut.FONT.getHeight() * this.getScale() + 4.0F;
                this.setSize(placeholderWidth, placeholderHeight);
            }
            this.popStack(stack);

            if (sizeWasNotSet && HudEditor.isEditing() && !PlayerUtils.isInGame()) {
                this.pushStack(stack);
                this.renderPlaceholder();
                this.popStack(stack);
            }
        }
    }

    private void renderPlaceholder() {
        float s = this.getScale();
        BlackOut.FONT.text(
                this.stack,
                this.name,
                s * 0.7F,
                4.0F,
                BlackOut.FONT.getHeight() * s * 0.5F + 2.0F,
                new Color(160, 160, 160),
                false,
                true
        );
    }

    public void renderQuad(PoseStack stack, boolean selected) {
        this.pushStack(stack);
        ColorRenderer renderer = ColorRenderer.getInstance();
        float r = 1.0F;
        float g = this.enabled ? 1.0F : 0.0F;
        float b = this.enabled ? 1.0F : 0.0F;
        renderer.quad(stack, 0.0F, 0.0F, 0.0F, this.width, this.height, r, g, b, selected ? 0.1F : 0.05F);
        try (RenderState state = renderer.begin(stack, VertexFormat.Mode.DEBUG_LINE_STRIP)) {
            renderer.quadOutlineShape(0.0F, 0.0F, 0.0F, this.width, this.height, r, g, b, selected ? 1.0F : 0.5F);
        }
        this.popStack(stack);
    }

    private void pushStack(PoseStack stack) {
        stack.pushPose();
        stack.translate(this.x, this.y, RenderLayer.HUD);
        stack.scale(this.getScale(), this.getScale(), 1.0F);
    }

    private void popStack(PoseStack stack) {
        stack.popPose();
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
        return this.width;
    }

    public float getHeight() {
        return this.height;
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
