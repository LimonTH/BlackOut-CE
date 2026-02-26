package bodevelopment.client.blackout.hud;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.clickgui.ClickGui;
import bodevelopment.client.blackout.gui.clickgui.components.ModuleComponent;
import bodevelopment.client.blackout.module.modules.client.GuiSettings;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.settings.EnumSetting;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;


public class HudEditorSettings {
    private final float width = 275.0F;
    private float x;
    private float y;
    private HudElement openedElement = null;
    private long openTime = 0L;
    private int openedY;
    private MatrixStack stack;
    private float frameTime;
    private float mx;
    private float my;
    private float length;
    private boolean moving;
    private float animDelta = 0.0F;

    private String lastDescription = null;
    private long hoverTime = 0L;
    private float descAlpha = 0.0F;

    public void render(MatrixStack stack, float frameTime, int mouseX, int mouseY) {
        if (this.openedElement == null) {
            animDelta = Math.max(0, animDelta - frameTime * 5f);
            if (animDelta <= 0) return;
        } else {
            animDelta = Math.min(1, animDelta + frameTime * 5f);
        }

        this.stack = stack;
        this.frameTime = frameTime;
        float prevMx = this.mx;
        float prevMy = this.my;

        this.mx = mouseX * RenderUtils.getScale();
        this.my = mouseY * RenderUtils.getScale();

        ClickGui.hoveredDescription = null;

        if (this.moving) {
            this.updateMoving(this.mx - prevMx, this.my - prevMy);
        }

        if (this.openedElement != null) {
            stack.push();
            this.length = ModuleComponent.getLength(this.openedElement.settingGroups) + 30.0F;

            RenderUtils.rounded(this.stack, this.x, this.y, 275.0F, this.length - 5.0F, 5.0F, 30.0F, GuiColorUtils.bg2.getRGB(), ColorUtils.SHADOW100I);

            if (this.mx >= this.x && this.mx <= this.x + width && this.my >= this.y && this.my <= this.y + 30.0F) {
                ClickGui.hoveredDescription = this.openedElement.description;
            }

            BlackOut.FONT.text(stack, this.openedElement.name, 2.0F, this.x + 137.5F, this.y + 15.0F, GuiColorUtils.enabled, true, true);

            this.renderSettings();
            stack.pop();

            boolean isOverDropdown = false;
            for (SettingGroup group : this.openedElement.settingGroups) {
                for (Setting<?> s : group.settings) {
                    if (s instanceof EnumSetting<?> es && es.isChoosing() && es.isVisible()) {
                        float listWidth = es.getWi() + 10.0F;
                        float listX = es.getX() + es.getWidth() - listWidth - es.getXOffset() - 5.0F;
                        float listY = es.getY() + 26.0F;
                        float listHeight = (es.getValues().length - 1) * 20.0F;

                        if (this.mx >= listX && this.mx <= listX + listWidth && this.my >= listY && this.my <= listY + listHeight) {
                            isOverDropdown = true;
                            break;
                        }
                    }
                }
                if (isOverDropdown) break;
            }

            if (isOverDropdown) {
                ClickGui.hoveredDescription = null;
            }

            this.renderEnumDropdowns();
            this.renderTooltipLogic(frameTime);
        }
    }

    private void renderSetting(Setting<?> setting) {
        if (setting.isVisible()) {
            float h = setting.getHeight();
            float curY = this.y + this.openedY;

            if (this.mx >= this.x && this.mx <= this.x + width && this.my >= curY && this.my <= curY + h) {
                if (setting.description != null && !setting.description.isEmpty()) {
                    ClickGui.hoveredDescription = setting.description;
                }
            }

            this.openedY += (int) setting.onRender(
                    this.stack,
                    this.frameTime * 20.0F,
                    275.0F,
                    this.x,
                    curY,
                    this.mx,
                    this.my,
                    true
            );
        }
    }

    private void renderSettingGroup(SettingGroup group, boolean last) {
        float categoryLength = 35.0F;
        for (Setting<?> setting : group.settings) {
            if (setting.isVisible()) categoryLength += setting.getHeight();
        }

        float groupY = this.y + this.openedY;

        switch (GuiSettings.getInstance().settingGroup.get()) {
            case Line:
                RenderUtils.fadeLine(this.stack, this.x, groupY + 30, this.x + 275.0F, groupY + 30, GuiColorUtils.getSettingCategory(groupY + 30).getRGB());
                BlackOut.FONT.text(this.stack, group.name, 1.8F, this.x + 137.5F, groupY + 20, GuiColorUtils.getSettingCategory(groupY + 30), true, true);
                this.openedY += 40;
                break;
            case Shadow: {
                int shadowColor = new Color(0, 0, 0, 80).getRGB();
                if (!last) {
                    RenderUtils.topFade(this.stack, this.x - 5.0F, groupY + categoryLength - 10.0F, 285.0F, 20.0F, shadowColor);
                }
                RenderUtils.bottomFade(this.stack, this.x - 5.0F, groupY + 30, 285.0F, 20.0F, shadowColor);
                BlackOut.FONT.text(this.stack, group.name, 1.8F, this.x + 137.5F, groupY + 15, GuiColorUtils.getSettingCategory(groupY + 30), true, true);
                this.openedY += 45;
                break;
            }
            case Quad: {
                int shadowColor = new Color(0, 0, 0, 80).getRGB();
                RenderUtils.rounded(this.stack, this.x + 3.0F, groupY + 12, 269.0F, categoryLength, 2.0F, 7.0F, GuiColorUtils.bg2.getRGB(), shadowColor);
                BlackOut.FONT.text(this.stack, group.name, 1.8F, this.x + 137.5F, groupY + 25, GuiColorUtils.getSettingCategory(groupY + 30), true, true);
                this.openedY += 50;
                break;
            }
            case None:
                BlackOut.FONT.text(this.stack, group.name, 1.8F, this.x + 137.5F, groupY + 20, GuiColorUtils.getSettingCategory(groupY + 30), true, true);
                this.openedY += 40;
        }
    }

    public boolean onMouse(int button, boolean pressed) {
        if (this.openedElement == null) {
            this.moving = false;
            return false;
        } else if (button != 0) {
            return button == 1 && this.handleRightClick(pressed);
        } else if (!pressed) {
            this.moving = false;
            this.openedElement.settingGroups.forEach(group -> group.settings.forEach(setting -> {
                if (setting.isVisible()) {
                    setting.onMouse(button, false);
                }
            }));
            return false;
        } else if (this.mx < this.x || this.mx > this.x + 275.0F || this.my < this.y || this.my > this.y + this.length) {
            return false;
        } else if (this.my < this.y + 30.0F) {
            this.moving = true;
            return true;
        } else {
            this.openedElement.settingGroups.forEach(group -> group.settings.forEach(setting -> {
                if (setting.isVisible()) {
                    setting.onMouse(button, true);
                }
            }));
            return true;
        }
    }

    public void onKey(int key, boolean pressed) {
        if (this.openedElement != null) {
            this.openedElement.settingGroups.forEach(group -> group.settings.forEach(setting -> {
                if (setting.isVisible()) {
                    setting.onKey(key, pressed);
                }
            }));
        }
    }

    private boolean handleRightClick(boolean pressed) {
        if (!pressed) {
            return false;
        } else if (!(this.mx < this.x) && !(this.mx > this.x + 275.0F) && !(this.my < this.y) && !(this.my > this.y + this.length)) {
            if (this.my < this.y + 30.0F) {
                this.openedElement = null;
            }

            return true;
        } else {
            return false;
        }
    }

    public void onRemoved(HudElement element) {
        if (element.equals(this.openedElement)) {
            this.openedElement = null;
        }
    }

    private void updateMoving(float deltaX, float deltaY) {
        var window = net.minecraft.client.MinecraftClient.getInstance().getWindow();
        float screenW = (float) window.getWidth();
        float screenH = (float) window.getHeight();

        this.x += deltaX;
        this.y += deltaY;

        this.x = MathHelper.clamp(this.x, 0, screenW - 275.0F);

        float currentHeight = Math.max(this.length, 30.0F);
        this.y = MathHelper.clamp(this.y, 0, screenH - currentHeight);
    }

    public void set(HudElement hudElement) {
        if (hudElement == null) {
            this.openedElement = null;
            return;
        }
        this.openTime = System.currentTimeMillis();
        this.openedElement = hudElement;

        var window = net.minecraft.client.MinecraftClient.getInstance().getWindow();
        float screenW = (float) window.getWidth();
        float screenH = (float) window.getHeight();

        float expectedLength = ModuleComponent.getLength(hudElement.settingGroups) + 30.0F;

        this.x = MathHelper.clamp(this.mx, 0, screenW - 275.0F);
        this.y = MathHelper.clamp(this.my, 0, screenH - expectedLength);
    }

    private void renderBG() {
        RenderUtils.rounded(this.stack, this.x, this.y, 275.0F, this.length - 5.0F, 5.0F, 30.0F, GuiColorUtils.bg2.getRGB(), ColorUtils.SHADOW100I);
    }

    private void renderSettings() {
        this.openedY = 30;
        GuiColorUtils.set(this.openedElement);

        for (int i = 0; i < this.openedElement.settingGroups.size(); i++) {
            SettingGroup settingGroup = this.openedElement.settingGroups.get(i);
            this.renderSettingGroup(settingGroup, i == this.openedElement.settingGroups.size() - 1);
            settingGroup.settings.forEach(this::renderSetting);
        }
    }

    private void renderEnumDropdowns() {
        for (SettingGroup group : this.openedElement.settingGroups) {
            for (Setting<?> s : group.settings) {
                if (s instanceof EnumSetting<?> es && es.isChoosing() && es.isVisible()) {
                    es.renderDropdown();
                }
            }
        }
    }

    private void renderTooltipLogic(float frameTime) {
        String hovered = ClickGui.hoveredDescription;

        if (hovered != null && !hovered.isEmpty()) {
            if (!hovered.equals(lastDescription)) {
                if (descAlpha <= 0.3F) hoverTime = System.currentTimeMillis();
                lastDescription = hovered;
            }
        } else {
            lastDescription = null;
            hoverTime = 0;
        }

        long waitTime = 600L;
        if (lastDescription != null && (System.currentTimeMillis() - hoverTime > waitTime || descAlpha > 0.3F)) {
            descAlpha = MathHelper.clamp(descAlpha + frameTime * 7.0F, 0.0F, 1.0F);
        } else {
            descAlpha = MathHelper.clamp(descAlpha - frameTime * 8.0F, 0.0F, 1.0F);
        }

        if (descAlpha > 0.0F && lastDescription != null) {
            this.drawTooltip(lastDescription);
        }
    }

    private java.util.List<String> wrapText(String text, float maxWidth) {
        java.util.List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (BlackOut.FONT.getWidth(currentLine + word) > maxWidth) {
                lines.add(currentLine.toString().trim());
                currentLine = new StringBuilder(word + " ");
            } else {
                currentLine.append(word).append(" ");
            }
        }
        lines.add(currentLine.toString().trim());
        return lines;
    }

    private void drawTooltip(String text) {
        float textScale = 1.6F;
        float maxBoxWidth = 350.0F;
        List<String> lines = wrapText(text, maxBoxWidth / textScale);

        float finalWidth = 0;
        for (String line : lines) {
            finalWidth = Math.max(finalWidth, BlackOut.FONT.getWidth(line) * textScale);
        }

        float lineHeight = BlackOut.FONT.getHeight() * textScale;
        float spacing = 2.0F;
        float finalHeight = lines.size() * lineHeight + (lines.size() - 1) * spacing;

        float rectX = this.mx + 15;
        float rectY = this.my + 15;

        this.stack.push();
        this.stack.translate(0, 0, 900);

        float smoothAlpha = descAlpha * descAlpha;
        int alphaInt = (int) (smoothAlpha * 255);
        int bgColor = ColorUtils.withAlpha(GuiColorUtils.bg2.getRGB(), (int) (smoothAlpha * 235));
        int textColor = ColorUtils.withAlpha(Color.WHITE.getRGB(), alphaInt);

        RenderUtils.rounded(this.stack, rectX, rectY, finalWidth + 12, finalHeight + 10, 5.0F, 5.0F, bgColor, ColorUtils.SHADOW100I);

        float currentY = rectY + 3.0F;
        for (String line : lines) {
            BlackOut.FONT.text(this.stack, line, textScale, rectX + 6, currentY, textColor, false, false);
            currentY += lineHeight + spacing;
        }
        this.stack.pop();
    }

    public HudElement getOpenedElement() {
        return this.openedElement;
    }

    public void setOpenedElement(HudElement element) {
        this.openedElement = element;
    }
}
