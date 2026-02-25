package bodevelopment.client.blackout.gui.clickgui.screens;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.TextField;
import bodevelopment.client.blackout.gui.clickgui.ClickGuiScreen;
import bodevelopment.client.blackout.interfaces.functional.EpicInterface;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.setting.settings.ListSetting;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ListScreen<T> extends ClickGuiScreen {
    private final TextField textField = new TextField();
    private final Map<T, Float> hoverAnims = new HashMap<>();
    private final ListSetting<T> setting;
    private final EpicInterface<T, String> getName;
    private double progress = 0.0;
    private final float itemHeight = 35.0F;

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
        return Math.max(left, right) * itemHeight + 60.0F;
    }

    @Override
    public void render() {
        RenderUtils.rounded(this.stack, 0, 0, width, height - 40.0F, 10, 10, GuiColorUtils.bg1.getRGB(), ColorUtils.SHADOW100I);

        this.stack.push();
        this.stack.translate(0.0F, 15.0F - this.scroll.get(), 0.0F);
        this.renderListItems();
        this.stack.pop();

        this.renderSearch();
    }

    private void renderListItems() {
        float lY = 0.0F;
        float rY = 0.0F;
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
                currentAnim = MathHelper.clamp(MathHelper.lerp(frameTime * 10.0F, currentAnim, target), 0.0F, 1.0F);
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
        RenderUtils.line(this.stack, half, 0, half, Math.max(lY, rY), new Color(255, 255, 255, 15).getRGB());
    }

    @Override
    public void onMouse(int button, boolean state) {
        if (state && button == 0) {
            if (this.textField.click(button, state)) return;

            T item = findItem();
            if (item != null) {
                if (setting.get().contains(item)) setting.get().remove(item);
                else setting.get().add(item);
                Managers.CONFIG.saveAll();
                setting.checkChange();
            }
        }
    }

    private T findItem() {
        float yOffset = itemHeight / 4.0F;
        float mouseRelY = (float) (my + scroll.get() - 15.0F);
        if (mouseRelY < -yOffset) return null;

        boolean isRightSide = mx > width / 2f;
        float lY = 0.0F;
        float rY = 0.0F;

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
        this.progress = (textField.isActive() || !textField.isEmpty())
                ? Math.min(progress + frameTime * 4.0, 1.0)
                : Math.max(progress - frameTime * 4.0, 0.0);

        if (progress > 0.01) {
            float fs = 1.6F;
            this.textField.render(this.stack, fs, mx, my,
                    width / 2f - 100, height - 85, 200, 40, 10, 5,
                    ColorUtils.withAlpha(Color.WHITE, (int) (progress * 255)),
                    ColorUtils.withAlpha(GuiColorUtils.bg2, (int) (progress * 200)));
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