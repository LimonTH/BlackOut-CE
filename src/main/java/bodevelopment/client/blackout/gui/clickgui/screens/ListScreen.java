package bodevelopment.client.blackout.gui.clickgui.screens;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.gui.TextField;
import bodevelopment.client.blackout.gui.clickgui.ClickGuiScreen;
import bodevelopment.client.blackout.interfaces.functional.EpicInterface;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.client.GuiSettings;
import bodevelopment.client.blackout.module.setting.settings.ColorSetting;
import bodevelopment.client.blackout.module.setting.settings.ListSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.render.Render2DUtils;
import net.minecraft.util.Mth;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListScreen<T> extends ClickGuiScreen {
    private static final float COLOR_RECT_W = 25.0F;
    private static final float COLOR_RECT_H = 8.0F;
    private static final int COLOR_RECT_RADIUS = 3;
    private static final float LIST_TOP = 55.0F;

    private final TextField textField = new TextField();
    private final Map<T, Float> hoverAnims = new HashMap<>();
    private final ListSetting<T> setting;
    private final EpicInterface<T, String> getName;
    private final float itemHeight = 35.0F;
    private double progress = 0.0;
    private float addAllHover = 0.0F;
    private float clearAllHover = 0.0F;

    public ListScreen(ListSetting<T> setting, EpicInterface<T, String> getName) {
        super(setting.name, 750.0F, 550.0F, true);
        this.setting = setting;
        this.getName = getName;
    }

    @Override
    protected float getLength() {
        int left = 0, right = 0;
        for (T item : this.setting.list) {
            if (this.validSearch(this.getName.get(item))) {
                if (this.setting.get().contains(item)) right++;
                else left++;
            }
        }
        return Math.max(left, right) * itemHeight + LIST_TOP + 20.0F;
    }

    @Override
    public void render() {
        Render2DUtils.rounded(this.stack, 0, 0, width, height - 40.0F, 10, 10, GuiColorUtils.bg1.getRGB(), ColorUtils.SHADOW100I);

        this.stack.pushPose();
        this.stack.translate(0.0F, 15.0F - this.scroll.get(), 0.0F);
        this.renderButtons();
        this.renderListItems();
        this.stack.popPose();

        this.renderSearch();
    }

    private void renderButtons() {
        float btnW = 80.0F;
        float btnH = 20.0F;
        float btnY = 4.0F;
        float scrollOffset = this.scroll.get();
        float mouseRelY = (float) (my + scrollOffset - 15.0F);

        float addX = 15.0F;
        float clearX = this.width - 15.0F - btnW;

        boolean addAllHovered = mx > addX && mx < addX + btnW
                && mouseRelY >= btnY && mouseRelY < btnY + btnH;
        boolean clearAllHovered = mx > clearX && mx < clearX + btnW
                && mouseRelY >= btnY && mouseRelY < btnY + btnH;

        this.addAllHover = Mth.clamp(Mth.lerp(frameTime * 10.0F, this.addAllHover, addAllHovered ? 1.0F : 0.0F), 0.0F, 1.0F);
        this.clearAllHover = Mth.clamp(Mth.lerp(frameTime * 10.0F, this.clearAllHover, clearAllHovered ? 1.0F : 0.0F), 0.0F, 1.0F);

        int addColor = ColorUtils.lerpColor(this.addAllHover, new Color(50, 55, 50, 160), new Color(60, 180, 80, 200)).getRGB();
        int clearColor = ColorUtils.lerpColor(this.clearAllHover, new Color(55, 50, 50, 160), new Color(200, 60, 60, 200)).getRGB();

        Render2DUtils.line(this.stack, 0, btnY + btnH + 12, this.width, btnY + btnH + 12, new Color(255, 255, 255, 15).getRGB());

        Render2DUtils.rounded(this.stack, addX, btnY, btnW, btnH, 4, 4, addColor, 0);
        Render2DUtils.rounded(this.stack, clearX, btnY, btnW, btnH, 4, 4, clearColor, 0);

        BlackOut.FONT.text(this.stack, "Add All", 1.5F, addX + btnW / 2.0F, btnY + btnH / 2.0F, Color.WHITE, true, true);
        BlackOut.FONT.text(this.stack, "Clear All", 1.5F, clearX + btnW / 2.0F, btnY + btnH / 2.0F, Color.WHITE, true, true);
    }

    private boolean isMouseOverColorRect(T item, boolean selected, float currentY) {
        float half = this.width / 2.0F;
        float PAD = 12.0F;
        float rectGap = 10.0F;
        RenderShape shape = this.setting.getShapeSupplier() != null ? this.setting.getShapeSupplier().get() : RenderShape.Full;
        boolean showLine = shape.outlines;
        boolean showSide = shape.sides;
        float totalW = (showLine ? COLOR_RECT_W : 0) + (showSide ? COLOR_RECT_W : 0) + ((showLine && showSide) ? rectGap : 0);
        float mouseRelY = (float) (my + scroll.get() - 15.0F);
        float rectStartX = selected ? half + PAD : half - totalW - PAD;
        float rectEndX = rectStartX + totalW;
        float rectRenderY = currentY - (COLOR_RECT_H / 2.0F) - 5.5F;
        float rectStartY = rectRenderY;
        float rectEndY = rectRenderY + COLOR_RECT_H;
        return mx >= rectStartX && mx <= rectEndX && mouseRelY >= rectStartY && mouseRelY <= rectEndY;
    }

    private void renderListItems() {
        float lY = LIST_TOP;
        float rY = LIST_TOP;
        float half = this.width / 2.0F;
        float halfItem = itemHeight / 2.0F;

        for (T item : this.setting.list) {
            String name = this.getName.get(item);
            if (!this.validSearch(name)) continue;

            boolean selected = this.setting.get().contains(item);
            float currentY = selected ? rY : lY;

            float target = 0.0F;
            boolean rectHovered = false;
            float mouseRelY = (float) (my + scroll.get() - 15.0F);

            if (currentY - scroll.get() > -itemHeight && currentY - scroll.get() < height) {
                boolean mouseInColumn = selected ? (mx > half) : (mx <= half);

                rectHovered = mouseInColumn && isMouseOverColorRect(item, selected, currentY);
                if (!rectHovered && mouseInColumn && mouseRelY >= currentY - halfItem && mouseRelY < currentY + halfItem) {
                    target = 1.0F;
                }

                float currentAnim = hoverAnims.getOrDefault(item, 0.0F);
                currentAnim = Mth.clamp(Mth.lerp(frameTime * 10.0F, currentAnim, target), 0.0F, 1.0F);
                hoverAnims.put(item, currentAnim);
                int textColor = ColorUtils.lerpColor(currentAnim, Color.GRAY, Color.WHITE).getRGB();

                float PAD = 12.0F;
                boolean hasColors = this.setting.supportsItemColors();

                if (hasColors) {
                    RenderShape shape = this.setting.getShapeSupplier() != null ? this.setting.getShapeSupplier().get() : RenderShape.Full;
                    boolean hasLine = this.setting.getDefaultLineColor() != null;
                    boolean hasSide = this.setting.getDefaultSideColor() != null;
                    boolean showLine = shape.outlines && hasLine;
                    boolean showSide = shape.sides && hasSide;

                    Color lineCol = showLine ? this.setting.getItemData(item, "lineColor") : null;
                    Color sideCol = showSide ? this.setting.getItemData(item, "sideColor") : null;
                    int lineColor = lineCol != null ? lineCol.getRGB() : (showLine ? this.setting.getDefaultLineColor().getRGB() : 0);
                    int sideColor = sideCol != null ? sideCol.getRGB() : (showSide ? this.setting.getDefaultSideColor().getRGB() : 0);
                    if ((lineColor >> 24 & 0xFF) < 30) lineColor = lineColor & 0x00FFFFFF | (30 << 24);
                    if ((sideColor >> 24 & 0xFF) < 30) sideColor = sideColor & 0x00FFFFFF | (30 << 24);

                    float rectRenderY = currentY - (COLOR_RECT_H / 2.0F) - 5.5F;
                    float rectGap = 10.0F;
                    float rectStartY = rectRenderY;
                    float rectEndY = rectRenderY + COLOR_RECT_H;

                    if (selected) {
                        float tw = BlackOut.FONT.getWidth(name) * 1.8F;
                        float textX = width - tw - 25.0F - (currentAnim * 3);
                        float rectX = half + PAD;

                        if (showLine) {
                            boolean lineHovered = mouseInColumn && mx >= rectX && mx <= rectX + COLOR_RECT_W && mouseRelY >= rectStartY && mouseRelY < rectEndY;
                            int lineBorder = lineHovered ? ColorUtils.lerpColor(0.5F, new Color(lineColor), Color.WHITE).getRGB() : lineColor;
                            Render2DUtils.rounded(this.stack, rectX, rectRenderY, COLOR_RECT_W, COLOR_RECT_H, COLOR_RECT_RADIUS, 4, lineColor, lineBorder);
                        }
                        if (showSide) {
                            float sideX = rectX + (showLine ? COLOR_RECT_W + rectGap : 0);
                            boolean sideHovered = mouseInColumn && mx >= sideX && mx <= sideX + COLOR_RECT_W && mouseRelY >= rectStartY && mouseRelY < rectEndY;
                            int sideBorder = sideHovered ? ColorUtils.lerpColor(0.5F, new Color(sideColor), Color.WHITE).getRGB() : sideColor;
                            Render2DUtils.rounded(this.stack, sideX, rectRenderY, COLOR_RECT_W, COLOR_RECT_H, COLOR_RECT_RADIUS, 4, sideColor, sideBorder);
                        }
                        BlackOut.FONT.text(this.stack, name, 1.8F, textX, currentY, textColor, false, true);
                    } else {
                        float textX = 25.0F + (currentAnim * 3);
                        float totalW = (showLine ? COLOR_RECT_W : 0) + (showSide ? COLOR_RECT_W : 0) + ((showLine && showSide) ? rectGap : 0);
                        float rectX = half - totalW - PAD;

                        if (showLine) {
                            boolean lineHovered = mouseInColumn && mx >= rectX && mx <= rectX + COLOR_RECT_W && mouseRelY >= rectStartY && mouseRelY < rectEndY;
                            int lineBorder = lineHovered ? ColorUtils.lerpColor(0.5F, new Color(lineColor), Color.WHITE).getRGB() : lineColor;
                            Render2DUtils.rounded(this.stack, rectX, rectRenderY, COLOR_RECT_W, COLOR_RECT_H, COLOR_RECT_RADIUS, 4, lineColor, lineBorder);
                        }
                        if (showSide) {
                            float sideX = rectX + (showLine ? COLOR_RECT_W + rectGap : 0);
                            boolean sideHovered = mouseInColumn && mx >= sideX && mx <= sideX + COLOR_RECT_W && mouseRelY >= rectStartY && mouseRelY < rectEndY;
                            int sideBorder = sideHovered ? ColorUtils.lerpColor(0.5F, new Color(sideColor), Color.WHITE).getRGB() : sideColor;
                            Render2DUtils.rounded(this.stack, sideX, rectRenderY, COLOR_RECT_W, COLOR_RECT_H, COLOR_RECT_RADIUS, 4, sideColor, sideBorder);
                        }
                        BlackOut.FONT.text(this.stack, name, 1.8F, textX, currentY, textColor, false, true);
                    }
                } else {
                    if (selected) {
                        float tw = BlackOut.FONT.getWidth(name) * 1.8F;
                        float textX = width - tw - 25.0F - (currentAnim * 3);
                        BlackOut.FONT.text(this.stack, name, 1.8F, textX, currentY, textColor, false, true);
                    } else {
                        float textX = 25.0F + (currentAnim * 3);
                        BlackOut.FONT.text(this.stack, name, 1.8F, textX, currentY, textColor, false, true);
                    }
                }
            }

            if (selected) rY += itemHeight;
            else lY += itemHeight;
        }
        Render2DUtils.line(this.stack, half, LIST_TOP - 5.0F, half, Math.max(lY, rY), new Color(255, 255, 255, 15).getRGB());
    }

    @Override
    public void onMouse(int button, boolean state) {
        if (state && button == 0) {
            if (this.textField.click(button, state)) return;

            if (this.clickButton()) return;

            if (this.setting.supportsItemColors()) {
                ColorRectHit hit = findColorClick();
                if (hit != null) {
                    openColorPicker(hit.item, hit.key);
                    return;
                }
            }

            T item = findItem();
            if (item != null) {
                if (setting.get().contains(item)) setting.get().remove(item);
                else setting.get().add(item);
                Managers.CONFIG.saveAll();
                setting.checkChange();
            }
        }
    }

    private ColorRectHit findColorClick() {
        float mouseRelY = (float) (my + scroll.get() - 15.0F);
        float half = this.width / 2.0F;
        float PAD = 12.0F;
        float rectGap = 10.0F;
        RenderShape shape = this.setting.getShapeSupplier() != null ? this.setting.getShapeSupplier().get() : RenderShape.Full;
        boolean showLine = shape.outlines;
        boolean showSide = shape.sides;
        float lY = LIST_TOP;
        float rY = LIST_TOP;

        for (T item : this.setting.list) {
            if (!validSearch(getName.get(item))) continue;

            boolean selected = setting.get().contains(item);
            float currentY = selected ? rY : lY;

            float totalW = (showLine ? COLOR_RECT_W : 0) + (showSide ? COLOR_RECT_W : 0) + ((showLine && showSide) ? rectGap : 0);
            float rectStartX = selected ? half + PAD : half - totalW - PAD;
            float rectRenderY = currentY - (COLOR_RECT_H / 2.0F) - 5.5F;
            float rectStartY = rectRenderY;
            float rectEndY = rectRenderY + COLOR_RECT_H;

            if (showLine && mx >= rectStartX && mx <= rectStartX + COLOR_RECT_W && mouseRelY >= rectStartY && mouseRelY <= rectEndY) {
                return new ColorRectHit(item, "lineColor");
            }
            float sideOffset = rectStartX + (showLine ? COLOR_RECT_W + rectGap : 0);
            if (showSide && mx >= sideOffset && mx <= sideOffset + COLOR_RECT_W && mouseRelY >= rectStartY && mouseRelY <= rectEndY) {
                return new ColorRectHit(item, "sideColor");
            }

            if (selected) rY += itemHeight;
            else lY += itemHeight;
        }
        return null;
    }

    private void openColorPicker(T item, String key) {
        Color current = this.setting.getItemData(item, key);
        if (current == null) {
            Color def = key.equals("sideColor") ? this.setting.getDefaultSideColor() : this.setting.getDefaultLineColor();
            current = def != null ? def : Color.WHITE;
        }
        BlackOutColor boColor = new BlackOutColor(current.getRed(), current.getGreen(), current.getBlue(), current.getAlpha());
        String label = key.equals("sideColor") ? "Side Color" : "Line Color";
        ColorSetting tempSetting = new ColorSetting(label + " for " + getName.get(item), boColor, "Custom color for this item.", null);
        ColorScreen cs = new ColorScreen(tempSetting, label + " for " + getName.get(item));
        cs.onChange = () -> {
            BlackOutColor c = tempSetting.get();
            this.setting.withItemData(item, key, new Color(c.red, c.green, c.blue, c.alpha));
            Managers.CONFIG.saveAll();
        };
        Managers.CLICK_GUI.openScreen(cs);
    }

    private boolean clickButton() {
        float btnW = 80.0F;
        float btnH = 20.0F;
        float btnY = 4.0F;
        float scrollOffset = this.scroll.get();
        float mouseRelY = (float) (my + scrollOffset - 15.0F);

        if (mouseRelY < btnY || mouseRelY >= btnY + btnH) return false;

        float addX = 15.0F;
        float clearX = this.width - 15.0F - btnW;

        if (mx > addX && mx < addX + btnW) {
            addAll();
            return true;
        }

        if (mx > clearX && mx < clearX + btnW) {
            clearAll();
            return true;
        }

        return false;
    }

    private void addAll() {
        List<T> selected = this.setting.get();
        for (T item : this.setting.list) {
            if (this.validSearch(this.getName.get(item)) && !selected.contains(item)) {
                selected.add(item);
            }
        }
        Managers.CONFIG.saveAll();
        this.setting.checkChange();
    }

    private void clearAll() {
        List<T> selected = this.setting.get();
        List<T> toRemove = new ArrayList<>();
        for (T item : selected) {
            if (this.validSearch(this.getName.get(item))) {
                toRemove.add(item);
            }
        }
        selected.removeAll(toRemove);
        Managers.CONFIG.saveAll();
        this.setting.checkChange();
    }

    private T findItem() {
        float halfItem = itemHeight / 2.0F;
        float mouseRelY = (float) (my + scroll.get() - 15.0F);
        if (mouseRelY < LIST_TOP - halfItem) return null;

        boolean isRightSide = mx > width / 2f;
        float lY = LIST_TOP;
        float rY = LIST_TOP;

        for (T item : setting.list) {
            if (!validSearch(getName.get(item))) continue;

            boolean itemIsSelected = setting.get().contains(item);
            float currentY = itemIsSelected ? rY : lY;

            if (itemIsSelected == isRightSide) {
                if (mouseRelY >= currentY - halfItem && mouseRelY < currentY + halfItem) {
                    if (this.setting.supportsItemColors() && isMouseOverColorRect(item, itemIsSelected, currentY)) {
                    } else {
                        return item;
                    }
                }
            }

            if (itemIsSelected) rY += itemHeight;
            else lY += itemHeight;
        }
        return null;
    }

    private void renderSearch() {
        float fs = GuiSettings.getInstance().fontScale.get().floatValue();

        this.progress = (textField.isActive() || !textField.isEmpty())
                ? Math.min(progress + frameTime * 4.0, 1.0)
                : Math.max(progress - frameTime * 4.0, 0.0);

        if (progress > 0.01) {
            float textScale = 1.8F * fs;

            float fieldWidth = 350.0F * fs;

            this.textField.render(
                    this.stack,
                    textScale,
                    mx, my,
                    width / 2f - (fieldWidth / 2f),
                    height - 110.0F,
                    fieldWidth,
                    0.0F,
                    15.0F,
                    10.0F,
                    ColorUtils.withAlpha(Color.WHITE, (int) (progress * 255)),
                    ColorUtils.withAlpha(GuiColorUtils.bg2, (int) (progress * 220))
            );
        }
    }

    private boolean validSearch(String s) {
        return s.toLowerCase().contains(textField.getContent().toLowerCase());
    }

    @Override
    public void onKey(int key, boolean state) {
        if (state) {
            if (key == 256) return;
            textField.type(key, state);
        }
    }

    private class ColorRectHit {
        final T item;
        final String key;

        ColorRectHit(T item, String key) {
            this.item = item;
            this.key = key;
        }
    }
}
