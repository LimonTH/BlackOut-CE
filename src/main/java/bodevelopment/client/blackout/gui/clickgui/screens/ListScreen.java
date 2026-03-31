package bodevelopment.client.blackout.gui.clickgui.screens;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.TextField;
import bodevelopment.client.blackout.gui.clickgui.ClickGuiScreen;
import bodevelopment.client.blackout.interfaces.functional.EpicInterface;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.client.GuiSettings;
import bodevelopment.client.blackout.module.setting.settings.ListSetting;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.render.Render2DUtils;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Mth;

public class ListScreen<T> extends ClickGuiScreen {
    private static final float BUTTON_HEIGHT = 30.0F;
    private static final float BUTTON_Y = -5.0F;

    private final TextField textField = new TextField();
    private final Map<T, Float> hoverAnims = new HashMap<>();
    private final ListSetting<T> setting;
    private final EpicInterface<T, String> getName;
    private double progress = 0.0;
    private final float itemHeight = 35.0F;
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
        return Math.max(left, right) * itemHeight + 60.0F + BUTTON_HEIGHT;
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
        float half = this.width / 2.0F;
        float btnW = half - 30.0F;
        float scrollOffset = this.scroll.get();

        boolean addAllHovered = mx > 15.0F && mx < 15.0F + btnW
                && my + scrollOffset - 15.0F >= BUTTON_Y && my + scrollOffset - 15.0F < BUTTON_Y + BUTTON_HEIGHT;
        boolean clearAllHovered = mx > half + 15.0F && mx < half + 15.0F + btnW
                && my + scrollOffset - 15.0F >= BUTTON_Y && my + scrollOffset - 15.0F < BUTTON_Y + BUTTON_HEIGHT;

        this.addAllHover = Mth.clamp(Mth.lerp(frameTime * 10.0F, this.addAllHover, addAllHovered ? 1.0F : 0.0F), 0.0F, 1.0F);
        this.clearAllHover = Mth.clamp(Mth.lerp(frameTime * 10.0F, this.clearAllHover, clearAllHovered ? 1.0F : 0.0F), 0.0F, 1.0F);

        int addColor = ColorUtils.lerpColor(this.addAllHover, new Color(80, 80, 80, 120), new Color(60, 180, 80, 160)).getRGB();
        int clearColor = ColorUtils.lerpColor(this.clearAllHover, new Color(80, 80, 80, 120), new Color(200, 60, 60, 160)).getRGB();

        Render2DUtils.rounded(this.stack, 15.0F, BUTTON_Y, btnW, BUTTON_HEIGHT, 6, 8, addColor, 0);
        Render2DUtils.rounded(this.stack, half + 15.0F, BUTTON_Y, btnW, BUTTON_HEIGHT, 6, 8, clearColor, 0);

        BlackOut.FONT.text(this.stack, "Add All", 1.8F, 15.0F + btnW / 2.0F, BUTTON_Y + BUTTON_HEIGHT / 2.0F, Color.WHITE, true, true);
        BlackOut.FONT.text(this.stack, "Clear All", 1.8F, half + 15.0F + btnW / 2.0F, BUTTON_Y + BUTTON_HEIGHT / 2.0F, Color.WHITE, true, true);
    }

    private void renderListItems() {
        float lY = BUTTON_HEIGHT + 15.0F;
        float rY = BUTTON_HEIGHT + 15.0F;
        float half = this.width / 2.0F;

        float yOffset = itemHeight / 4.0F;

        for (T item : this.setting.list) {
            String name = this.getName.get(item);
            if (!this.validSearch(name)) continue;

            boolean selected = this.setting.get().contains(item);
            float currentY = selected ? rY : lY;

            float target = 0.0F;
            float mouseRelY = (float) (my + scroll.get() - 15.0F);

            if (currentY - scroll.get() > -itemHeight && currentY - scroll.get() < height) {
                boolean mouseInColumn = selected ? (mx > half) : (mx <= half);

                if (mouseInColumn && mouseRelY >= currentY - yOffset && mouseRelY < currentY + itemHeight - yOffset) {
                    target = 1.0F;
                }

                float currentAnim = hoverAnims.getOrDefault(item, 0.0F);
                currentAnim = Mth.clamp(Mth.lerp(frameTime * 10.0F, currentAnim, target), 0.0F, 1.0F);
                hoverAnims.put(item, currentAnim);
                int color = ColorUtils.lerpColor(currentAnim, Color.GRAY, Color.WHITE).getRGB();

                if (selected) {
                    float tw = BlackOut.FONT.getWidth(name) * 1.8F;
                    BlackOut.FONT.text(this.stack, name, 1.8F, width - tw - 25.0F - (currentAnim * 3), currentY, color, false, true);
                } else {
                    BlackOut.FONT.text(this.stack, name, 1.8F, 25.0F + (currentAnim * 3), currentY, color, false, true);
                }
            }

            if (selected) rY += itemHeight;
            else lY += itemHeight;
        }
        Render2DUtils.line(this.stack, half, BUTTON_HEIGHT + 15.0F, half, Math.max(lY, rY), new Color(255, 255, 255, 15).getRGB());
    }

    @Override
    public void onMouse(int button, boolean state) {
        if (state && button == 0) {
            if (this.textField.click(button, state)) return;

            if (this.clickButton()) return;

            T item = findItem();
            if (item != null) {
                if (setting.get().contains(item)) setting.get().remove(item);
                else setting.get().add(item);
                Managers.CONFIG.saveAll();
                setting.checkChange();
            }
        }
    }

    private boolean clickButton() {
        float half = this.width / 2.0F;
        float btnW = half - 30.0F;
        float scrollOffset = (float) this.scroll.get();
        float mouseRelY = (float) (my + scrollOffset - 15.0F);

        if (mouseRelY < BUTTON_Y || mouseRelY >= BUTTON_Y + BUTTON_HEIGHT) return false;

        if (mx > 15.0F && mx < 15.0F + btnW) {
            addAll();
            return true;
        }

        if (mx > half + 15.0F && mx < half + 15.0F + btnW) {
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
        float yOffset = itemHeight / 4.0F;
        float mouseRelY = (float) (my + scroll.get() - 15.0F);
        if (mouseRelY < BUTTON_HEIGHT + 15.0F - yOffset) return null;

        boolean isRightSide = mx > width / 2f;
        float lY = BUTTON_HEIGHT + 15.0F;
        float rY = BUTTON_HEIGHT + 15.0F;

        for (T item : setting.list) {
            if (!validSearch(getName.get(item))) continue;

            boolean itemIsSelected = setting.get().contains(item);
            float currentY = itemIsSelected ? rY : lY;

            if (itemIsSelected == isRightSide) {
                if (mouseRelY >= currentY - yOffset && mouseRelY < currentY + itemHeight - yOffset) {
                    return item;
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
            if (key == 256) {
                Managers.CLICK_GUI.CLICK_GUI.setScreen(null);
                return;
            }
            textField.type(key, state);
        }
    }
}
