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

import java.awt.*;

public class ListScreen<T> extends ClickGuiScreen {
    private final TextField textField = new TextField();
    private final ListSetting<T> setting;
    private final EpicInterface<T, String> getName;
    private double progress = 0.0;

    public ListScreen(ListSetting<T> setting, EpicInterface<T, String> getName) {
        super(setting.name, 750.0F, 550.0F, true);
        this.setting = setting;
        this.getName = getName;
    }

    @Override
    protected float getLength() {
        int left = 0;
        int right = 0;
        for (T item : this.setting.list) {
            if (this.validSearch(this.getName.get(item))) {
                if (this.setting.get().contains(item)) right++;
                else left++;
            }
        }
        return Math.max(left, right) * 30.0F + 60.0F;
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

        for (T item : this.setting.list) {
            String name = this.getName.get(item);
            if (!this.validSearch(name)) continue;

            boolean selected = this.setting.get().contains(item);
            float currentY = selected ? rY : lY;

            if (currentY - scroll.get() > -30 && currentY - scroll.get() < height) {
                double dist = Math.abs(this.my - (currentY - this.scroll.get() + 15.0F));
                float hoverAnim = (float) Math.max(0.0, 1.0 - (dist / 25.0));
                int color = ColorUtils.lerpColor(hoverAnim, Color.GRAY, Color.WHITE).getRGB();

                if (selected) {
                    float tw = BlackOut.FONT.getWidth(name) * 1.8F;
                    BlackOut.FONT.text(this.stack, name, 1.8F, width - tw - 25.0F - (hoverAnim * 3), rY, color, false, true);
                    rY += 30.0F;
                } else {
                    BlackOut.FONT.text(this.stack, name, 1.8F, 25.0F + (hoverAnim * 3), lY, color, false, true);
                    lY += 30.0F;
                }
            } else {
                if (selected) rY += 30.0F;
                else lY += 30.0F;
            }
        }
        RenderUtils.line(this.stack, half, 0, half, Math.max(lY, rY), new Color(255, 255, 255, 15).getRGB());
    }

    private void renderSearch() {
        this.progress = (textField.isActive() || !textField.isEmpty())
                ? Math.min(progress + frameTime * 4.0, 1.0)
                : Math.max(progress - frameTime * 4.0, 0.0);

        if (progress > 0.01) {
            this.textField.render(this.stack, 1.6F, mx, my,
                    width / 2f - 100, height - 85, 200, 0, 10, 5,
                    ColorUtils.withAlpha(Color.WHITE, (int) (progress * 255)),
                    ColorUtils.withAlpha(GuiColorUtils.bg2, (int) (progress * 200)));
        }
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
            }
        }
    }

    private T findItem() {
        if (my < 0 || my > height - 10) return null;

        boolean right = mx > width / 2f;
        double clickY = my + scroll.get() - 15.0F;
        int targetIdx = (int) (clickY / 30.0F);

        int currentIdx = 0;
        for (T item : setting.list) {
            if (!validSearch(getName.get(item))) continue;
            if (setting.get().contains(item) == right) {
                if (currentIdx == targetIdx) return item;
                currentIdx++;
            }
        }
        return null;
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
}