package bodevelopment.client.blackout.keys;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.ConfigType;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.render.AnimUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;

public class KeyBind {
    public Pressable value;
    private float x;
    private float y;
    private double mx;
    private double my;
    private float width;
    private int holdingKey;
    private boolean holdingMouse;
    private long prevTime;
    private double bindProgress = 0.0;
    private double pulse = 0.0;

    private long lastRenderTime;

    public KeyBind(Pressable value) {
        this.value = value;
    }

    public void render(MatrixStack stack, float x, float y, float maxX, double mx, double my) {
        this.lastRenderTime = System.currentTimeMillis();
        this.x = x;
        this.y = y;
        this.mx = mx;
        this.my = my;
        String name;
        if (this.value == null) {
            name = "";
            this.width = 17.0F;
        } else {
            name = this.value.getName();
            this.width = Math.max(BlackOut.FONT.getWidth(name) * 1.5F, 17.0F);
        }

        this.x = this.x - Math.max(this.x + this.width - maxX, 0.0F);
        double frameTime = (System.currentTimeMillis() - this.prevTime) / 1000.0;
        this.binding(frameTime);
        this.prevTime = System.currentTimeMillis();
        double progress;
        float rad;
        if (this.pulse > 0.0) {
            progress = this.pulse;
            rad = (float) (this.pulse * 5.0);
        } else {
            progress = AnimUtils.easeOutCubic(this.bindProgress) / 2.0;
            rad = (float) (this.bindProgress * 3.0);
        }

        Color shadowColor = ColorUtils.lerpColor(this.pulse, ColorUtils.SHADOW100, Color.WHITE);
        Color insideColor = ColorUtils.lerpColor(progress, GuiColorUtils.bindBG, Color.WHITE);
        this.pulse = Math.max(this.pulse - frameTime, 0.0);
        RenderUtils.rounded(stack, this.x - this.width / 2.0F, this.y - 9.0F, this.width, 17.0F, 4.0F, rad, insideColor.getRGB(), shadowColor.getRGB());
        BlackOut.FONT.text(stack, name, 1.5F, this.x, this.y, GuiColorUtils.bindText.getRGB(), true, true);
    }

    private void binding(double frameTime) {
        if (this.holdingKey < 0) {
            this.bindProgress = Math.max(this.bindProgress - frameTime * 3.0, 0.0);
        } else {
            if (this.holdingMouse) {
                if (!MouseButtons.get(this.holdingKey)) {
                    this.stopBinding();
                    return;
                }
            } else if (!Keys.get(this.holdingKey)) {
                this.stopBinding();
                return;
            }

            this.bindProgress = Math.min(this.bindProgress + frameTime * 2.0, 1.0);
            if (this.bindProgress >= 1.0) {
                if (this.holdingMouse) {
                    this.setMouse(this.holdingKey);
                } else {
                    this.setKey(this.holdingKey);
                }

                Managers.CONFIG.save(ConfigType.Binds);
                this.stopBinding();
                this.pulse = 1.0;
            }
        }
    }

    private void stopBinding() {
        this.holdingKey = -1;
    }

    public boolean onMouse(int key, boolean pressed) {
        if (!this.isInside()) {
            return false;
        } else {
            if (pressed && this.holdingKey < 0) {
                this.holdingKey = key;
                this.holdingMouse = true;
            }

            return true;
        }
    }

    public boolean onKey(int key, boolean pressed) {
        if (!this.isInside()) {
            return false;
        } else {
            if (pressed && this.holdingKey < 0) {
                this.holdingKey = key;
                this.holdingMouse = false;
            }

            return true;
        }
    }

    public boolean isInside() {
        if (System.currentTimeMillis() - this.lastRenderTime > 100L) {
            return false;
        }

        return this.mx > this.x - this.width / 2.0F - 4.0F &&
                this.mx < this.x + this.width / 2.0F + 4.0F &&
                this.my > this.y - 13.0F &&
                this.my < this.y + 12.0F;
    }

    public void setMouse(int key) {
        this.value = new MouseButton(key);
    }

    public int getKey() {
        return this.value == null ? -1 : this.value.key;
    }

    public void setKey(int key) {
        this.value = key != 259 && key != 261 && key != 256 ? new Key(key) : null;
    }

    public String getName() {
        return this.value == null ? "" : this.value.getName();
    }

    public boolean isKey(int key) {
        return this.value instanceof Key k && k.key == key;
    }

    public boolean isMouse(int key) {
        return this.value instanceof MouseButton m && m.key == key;
    }

    public boolean isBinding() {
        return this.holdingKey >= 0;
    }

    public boolean isPressed() {
        return this.value != null && this.value.isPressed();
    }
}
