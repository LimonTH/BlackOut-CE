package bodevelopment.client.blackout.gui.clickgui.components;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.clickgui.ClickGui;
import bodevelopment.client.blackout.gui.clickgui.Component;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.modules.client.GuiSettings;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.settings.KeyBindSetting;
import bodevelopment.client.blackout.module.setting.settings.StringSetting;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.SelectedComponent;
import bodevelopment.client.blackout.util.render.AnimUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.List;

public class ModuleComponent extends Component {
    private static final Color disabledColor = new Color(150, 150, 150, 255);
    public final Module module;
    public float length;
    public float l;
    public float maxLength = -1.0F;
    public boolean opened = false;
    private float openProgress = 0.0F;
    private double toggleProgress = 0.0;
    private long prevTime = 0L;

    public ModuleComponent(MatrixStack stack, Module module) {
        super(stack);
        this.module = module;
    }

    public static float getLength(List<SettingGroup> settingGroups) {
        float fs = GuiSettings.getInstance().fontScale.get().floatValue();
        float length = switch (GuiSettings.getInstance().settingGroup.get()) {
            case Line, Shadow, None -> 0.0F;
            case Quad -> 7.0F * fs;
        };

        for (SettingGroup group : settingGroups) {
            length += switch (GuiSettings.getInstance().settingGroup.get()) {
                case Line, None -> 40.0F * fs;
                case Shadow -> 45.0F * fs;
                case Quad -> 50.0F * fs;
            };

            for (Setting<?> setting : group.settings) {
                if (setting.isVisible()) {
                    length += setting.getHeight();
                }
            }
        }

        return length;
    }

    @Override
    public float render() {
        boolean screenOpen = Managers.CLICK_GUI.CLICK_GUI.openedScreen != null;

        float currentMx = screenOpen ? -999 : (float) this.mx;
        float currentMy = screenOpen ? -999 : (float) this.my;

        GuiColorUtils.set(this.module);
        this.length = this.getHeight() + getLength(this.module.settingGroups);
        this.updateAnimation();

        if (!(this.y > ClickGui.height + 30.0F) && !(this.y + this.maxLength < -30.0F)) {
            if (currentMx > this.x && currentMx < this.x + this.width && currentMy > this.y && currentMy < this.y + this.getHeight()) {
                ClickGui.hoveredDescription = this.module.description;
            }
            this.shadowScissor();
            float bgY = Math.max(-15, this.y);
            float bgMaxY = Math.min(this.y + this.maxLength, ClickGui.height + 15.0F);
            this.rounded(this.x, bgY, this.width, bgMaxY - bgY, 5.0F, 8.0F, GuiColorUtils.bg2, ColorUtils.SHADOW100);
            this.scissor();

            double delta = (System.currentTimeMillis() - this.prevTime) / 1000.0;
            this.prevTime = System.currentTimeMillis();
            if (this.module.enabled) {
                this.toggleProgress = Math.min(this.toggleProgress + delta, 1.0);
            } else {
                this.toggleProgress = Math.max(this.toggleProgress - delta, 0.0);
            }

            this.renderModule(AnimUtils.easeInOutCubic(this.toggleProgress), currentMx, currentMy);
            this.renderSettings(currentMx, currentMy);

            GlStateManager._disableScissorTest();
            return Math.min(this.l, this.maxLength);
        } else {
            return Math.min(this.l, this.maxLength);
        }
    }

    private void renderSettings(float currentMx, float currentMy) {
        this.l = this.getHeight();
        float fs = GuiSettings.getInstance().fontScale.get().floatValue();

        for (int i = 0; i < this.module.settingGroups.size(); i++) {
            SettingGroup settingGroup = this.module.settingGroups.get(i);
            if (this.l >= this.maxLength) return;

            float yPos = this.y + this.l;
            if (yPos > ClickGui.height) return;

            float categoryHeight = 0.0F;
            float height = switch (GuiSettings.getInstance().settingGroup.get()) {
                case Line, None -> 40.0F * fs;
                case Shadow -> 45.0F * fs;
                case Quad -> 50.0F * fs;
            };

            for (Setting<?> setting : settingGroup.settings) {
                if (setting.isVisible()) categoryHeight += setting.getHeight();
            }

            if (yPos > -height - categoryHeight - 30.0F) {
                this.renderSettingGroup(settingGroup, i == this.module.settingGroups.size() - 1);
            }

            this.l += height;
            settingGroup.settings.forEach(s -> this.renderSetting(s, currentMx, currentMy));
        }
    }

    private void renderSetting(Setting<?> setting, float currentMx, float currentMy) {
        if (setting.isVisible()) {
            int posY = (int) (this.y + this.l);
            boolean shouldRender = this.l < this.maxLength && posY >= -setting.getHeight() && posY <= ClickGui.height + setting.getHeight();
            this.l = this.l + setting.onRender(this.stack, this.frameTime, this.width - 10.0F, this.x + 5, (int) (this.y + this.l), currentMx, currentMy, shouldRender);
        }
    }

    private void renderSettingGroup(SettingGroup group, boolean last) {
        float fs = GuiSettings.getInstance().fontScale.get().floatValue();
        float groupScale = fs * 2.0F;

        float categoryLength = 35.0F * fs;

        for (Setting<?> setting : group.settings) {
            if (setting.isVisible()) {
                categoryLength += setting.getHeight();
            }
        }

        switch (GuiSettings.getInstance().settingGroup.get()) {
            case Line:
                this.fadeLine(
                        this.x, this.y + this.l + (30.0F * fs), this.x + this.width, this.y + this.l + (30.0F * fs), GuiColorUtils.getSettingCategory(this.y + this.l + 30.0F)
                );
                this.text(
                        group.name,
                        groupScale,
                        this.x + this.width / 2.0F,
                        (int) (this.y + this.l + (20.0F * fs)),
                        true,
                        true,
                        GuiColorUtils.getSettingCategory(this.y + this.l + 30.0F)
                );
                break;
            case Shadow:
                float bottomY = this.y + this.l + categoryLength - (10.0F * fs);
                if (!last && bottomY < ClickGui.height + 50.0F) {
                    RenderUtils.topFade(this.stack, this.x - 5, this.y + this.l + categoryLength - (10.0F * fs), this.width + 10.0F, 20.0F, ColorUtils.SHADOW80I);
                }

                RenderUtils.bottomFade(this.stack, this.x - 5, this.y + this.l + (30.0F * fs), this.width + 10.0F, 20.0F, ColorUtils.SHADOW80I);
                this.text(
                        group.name,
                        groupScale,
                        this.x + this.width / 2.0F,
                        (int) (this.y + this.l + (15.0F * fs)),
                        true,
                        true,
                        GuiColorUtils.getSettingCategory(this.y + this.l + 30.0F)
                );
                break;
            case Quad:
                RenderUtils.rounded(
                        this.stack,
                        this.x + 7,
                        this.y + this.l + (12.0F * fs),
                        this.width - 14.0F,
                        categoryLength,
                        2.0F,
                        7.0F,
                        GuiColorUtils.bg2.getRGB(),
                        ColorUtils.SHADOW80I
                );
                this.text(
                        group.name,
                        groupScale,
                        this.x + this.width / 2.0F,
                        (int) (this.y + this.l + (25.0F * fs)),
                        true,
                        true,
                        GuiColorUtils.getSettingCategory(this.y + this.l + 30.0F)
                );
                break;
            case None:
                this.text(
                        group.name,
                        groupScale,
                        this.x + this.width / 2.0F,
                        (int) (this.y + this.l + (20.0F * fs)),
                        true,
                        true,
                        GuiColorUtils.getSettingCategory(this.y + this.l + 30.0F)
                );
        }
    }

    private void renderModule(double toggleProgress, float currentMx, float currentMy) {
        float moduleNameOffset = this.getModuleNameOffset();
        GuiSettings guiSettings = GuiSettings.getInstance();
        float nameY = this.getY();

        if (nameY > -50.0 && nameY < ClickGui.height + 50.0F) {
            float prevAlpha = Renderer.getAlpha();
            if (toggleProgress > 0.0) {
                Renderer.setAlpha((float) toggleProgress);
                guiSettings.textColor.render(this.stack, this.module.getDisplayName(), this.getScale(), this.getX(), nameY, false, true);
                Renderer.setAlpha(prevAlpha);
            }

            if (1.0 - toggleProgress > 0.0) {
                Renderer.setAlpha((float) (1.0 - toggleProgress));
                BlackOut.FONT.text(this.stack, this.module.getDisplayName(), this.getScale(), this.getX(), nameY, disabledColor, false, true);
                Renderer.setAlpha(prevAlpha);
            }
        }

        if (this.module.toggleable()) {
            if (this.y + moduleNameOffset > -50.0F && nameY < ClickGui.height + 50.0F) {
                this.module.bind.get().render(this.stack, this.x + this.width - 30.0F, nameY, this.x + this.width, currentMx, currentMy);
            }
        }
    }

    private float getModuleNameOffset() {
        return this.getHeight() / 2.0F;
    }

    @Override
    public void onMouse(int button, boolean pressed) {
        if (Managers.CLICK_GUI.CLICK_GUI.openedScreen != null) return;

        if (this.module.toggleable() && this.module.bind.get().onMouse(button, pressed)) {
            return;
        }

        if (!(this.my < this.y)) {
            if (this.mx > this.x && this.mx < this.x + this.width && pressed && this.my < this.y + this.getHeight()) {
                if (button == 1) {
                    this.opened = !this.opened;
                } else if (button == 0) {
                    this.module.toggle();
                    Managers.CONFIG.saveModule(this.module);
                }
            } else {
                if (this.mx > this.x && this.mx < this.x + this.width && this.my < this.y + this.maxLength || !pressed) {
                    for (SettingGroup group : this.module.settingGroups) {
                        for (Setting<?> setting : group.settings) {
                            if (setting.isVisible() && setting.onMouse(button, pressed) && pressed) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateAnimation() {
        float dist = Math.abs(this.getHeight() - this.length);
        float animationTime = MathHelper.clamp(dist / 100.0F, 1.0F, 10.0F);
        this.openProgress = MathHelper.clamp(
                this.openProgress + (this.opened ? this.frameTime * 20.0F / animationTime : -this.frameTime * 20.0F / animationTime), 0.0F, 1.0F
        );
        this.maxLength = MathHelper.lerp((float) AnimUtils.easeInOutSine(this.openProgress), this.getHeight(), this.length);
    }

    private void shadowScissor() {
        float sx = BlackOut.mc.getWindow().getWidth() / 2.0F - ClickGui.width / 2.0F * ClickGui.unscaled + ClickGui.x;
        float y1 = BlackOut.mc.getWindow().getHeight() / 2.0F - (ClickGui.height / 2.0F + 10.0F) * ClickGui.unscaled - ClickGui.y;
        float y2 = BlackOut.mc.getWindow().getHeight() / 2.0F + (ClickGui.height / 2.0F + 10.0F) * ClickGui.unscaled - ClickGui.y;
        GlStateManager._enableScissorTest();
        GlStateManager._scissorBox((int) sx, (int) y1, (int) (ClickGui.width * ClickGui.unscaled), (int) Math.abs(y1 - y2));
    }

    private void scissor() {
        float minY = Math.max(0, this.y);
        float maxY = Math.min(ClickGui.height, this.y + this.maxLength);
        float sx = BlackOut.mc.getWindow().getWidth() / 2.0F - (ClickGui.width / 2.0F - this.x + 5.0F) * ClickGui.unscaled + ClickGui.x;
        float y1 = BlackOut.mc.getWindow().getHeight() / 2.0F - (ClickGui.height / 2.0F - (ClickGui.height - maxY) + 5.0F) * ClickGui.unscaled - ClickGui.y;
        float y2 = BlackOut.mc.getWindow().getHeight() / 2.0F + (ClickGui.height / 2.0F - minY + 10.0F) * ClickGui.unscaled - ClickGui.y;
        GlStateManager._scissorBox(
                (int) sx, (int) Math.ceil(y1), (int) ((this.width + 10.0F) * ClickGui.unscaled), (int) Math.ceil(y1 > y2 ? 0.0 : Math.abs(y1 - y2))
        );
    }

    private float getX() {
        float closedX = GuiSettings.getInstance().centerXClosed.get()
                ? this.x + this.width / 2.0F - BlackOut.FONT.getWidth(this.module.getDisplayName()) / 2.0F * this.getScale()
                : MathHelper.lerp(
                GuiSettings.getInstance().moduleXClosed.get().floatValue(),
                this.x + 8,
                this.x + this.width - this.getScale() * BlackOut.FONT.getWidth(this.module.getDisplayName()) - 38.0F
        );
        float openX = GuiSettings.getInstance().centerX.get()
                ? this.x + this.width / 2.0F - BlackOut.FONT.getWidth(this.module.getDisplayName()) / 2.0F * this.getScale()
                : MathHelper.lerp(
                GuiSettings.getInstance().moduleX.get().floatValue(),
                this.x + 8,
                this.x + this.width - this.getScale() * BlackOut.FONT.getWidth(this.module.getDisplayName()) - 38.0F
        );
        return MathHelper.lerp(this.openProgress, closedX, openX);
    }

    private float getY() {
        return this.y + (this.getHeight() / 2.0F);
    }

    private float getScale() {
        float fs = GuiSettings.getInstance().fontScale.get().floatValue();
        float multiplier = MathHelper.lerp(
                this.openProgress,
                GuiSettings.getInstance().moduleScaleClosed.get().floatValue(),
                GuiSettings.getInstance().moduleScale.get().floatValue()
        );
        return Math.max(fs, fs * multiplier);
    }

    public float getHeight() {
        float currentScale = this.getScale();
        float minH = (BlackOut.FONT.getHeight() * currentScale) + (15.0F * currentScale);

        float settingH = MathHelper.lerp(
                this.openProgress,
                GuiSettings.getInstance().moduleHeightClosed.get().floatValue(),
                GuiSettings.getInstance().moduleHeight.get().floatValue()
        );

        return Math.max(minH, settingH);
    }

    public boolean isMouseOverBind() {
        if (Managers.CLICK_GUI.CLICK_GUI.openedScreen != null) return false;
        if (this.module.category != ClickGui.selectedCategory) return false;

        if (this.module.toggleable() && this.module.bind.get().isInside()) return true;

        if (this.opened) {
            for (SettingGroup sg : this.module.settingGroups) {
                for (Setting<?> s : sg.settings) {
                    if (!s.isVisible()) continue;

                    if (s instanceof StringSetting ss) {
                        if (SelectedComponent.is(ss.getId())) return true;
                    }

                    if (s instanceof KeyBindSetting kbs) {
                        if (SelectedComponent.is(kbs.getId())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void onKey(int key, boolean state) {
        if (Managers.CLICK_GUI.CLICK_GUI.openedScreen != null) return;

        this.module.bind.get().onKey(key, state);
        this.module.settingGroups.forEach(group -> group.settings.forEach(setting -> {
            if (setting.isVisible()) {
                setting.onKey(key, state);
            }
        }));
    }
}