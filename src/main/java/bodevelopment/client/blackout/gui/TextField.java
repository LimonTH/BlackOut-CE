package bodevelopment.client.blackout.gui;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.keys.Keys;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class TextField {
    private static final Map<String, String> shiftModified = new HashMap<>();
    private static final Map<String, String> altModified = new HashMap<>();

    static {
        shiftModified.put("1", "!");
        shiftModified.put("2", "@");
        shiftModified.put("3", "#");
        shiftModified.put("4", "$");
        shiftModified.put("5", "%");
        shiftModified.put("6", "^");
        shiftModified.put("7", "&");
        shiftModified.put("8", "*");
        shiftModified.put("9", "(");
        shiftModified.put("0", ")");
        shiftModified.put("-", "_");
        shiftModified.put("=", "+");
        altModified.put("5", "€");
    }

    private String content = "";
    private int typingIndex = 0;
    private long lastType = 0L;
    private boolean active = false;
    private int heldKey = 0;
    private long prevHeld = 0L;
    private float width;
    private float height;
    private double mx;
    private double my;
    private float scale;
    private float radius;

    public void render(
            MatrixStack stack,
            float scale,
            double mx,
            double my,
            float x,
            float y,
            float width,
            float height,
            float radius,
            float shadow,
            Color textColor,
            Color bgColor
    ) {
        this.width = width;
        this.height = height;
        this.mx = mx - x;
        this.my = my - y;
        this.scale = scale;
        this.radius = radius;
        this.limitIndex();
        RenderUtils.rounded(stack, x, y, width, height, radius, shadow, bgColor.getRGB(), new Color(0, 0, 0, (int) Math.floor(bgColor.getAlpha() * 0.6)).getRGB());
        float textHeight = BlackOut.FONT.getHeight() * scale;
        float centerY = y + height / 2.0F;
        float manualY = centerY - (textHeight / 2.0F) - (scale);

        BlackOut.FONT.text(stack, this.content, scale, x, manualY, textColor, false, false);
        float offset = this.getOffset();
        if (!Keys.get(this.heldKey)) {
            this.heldKey = 0;
        }

        if (this.heldKey > 0 && System.currentTimeMillis() - this.prevHeld > 500L) {
            this.type(this.heldKey, true);
            this.prevHeld += 50L;
        }

        if (this.active && (System.currentTimeMillis() - this.lastType) % 1000L < 500L) {
            float fontHeight = BlackOut.FONT.getHeight() * scale;
            float cursorHeight = fontHeight - 2.0F;

            float cursorY = centerY - (cursorHeight / 2.0F);

            RenderUtils.quad(
                    stack,
                    x + offset,
                    cursorY,
                    scale,
                    cursorHeight,
                    textColor.getRGB()
            );
        }
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    private float getOffset() {
        float offset = 0.0F;
        int index = 0;
        if (index >= this.typingIndex) {
            return offset;
        } else {
            for (String c : this.content.split("")) {
                offset += BlackOut.FONT.getWidth(c) * this.scale;
                if (++index >= this.typingIndex) {
                    return offset;
                }
            }

            return 0.0F;
        }
    }

    public boolean click(int button, boolean state) {
        if (this.isEmpty()) {
            return false;
        } else if (this.mx < -this.radius || this.mx > this.width + this.radius || this.my < -this.radius || this.my > this.height + this.radius) {
            return false;
        } else if (button == 0 && state) {
            this.typingIndex = this.getIndex();
            this.lastType = System.currentTimeMillis();
            return true;
        } else {
            return true;
        }
    }

    private int getIndex() {
        float offset = 0.0F;
        int i = 0;
        int closestI = 0;
        double closest = 1000.0;
        double d = Math.abs(offset - this.mx);
        if (d < closest) {
            closest = d;
        }

        for (String c : this.content.split("")) {
            offset += BlackOut.FONT.getWidth(c) * this.scale;
            i++;
            d = Math.abs(offset - this.mx);
            if (d < closest) {
                closestI = i;
                closest = d;
            }
        }

        return closestI;
    }

    public void type(int key, boolean state) {
        if (!state) return;

        if ((key >= 32 && key <= 162) || (key >= 256 && key <= 348)) {

            if (key != this.heldKey) {
                this.prevHeld = System.currentTimeMillis();
            }
            this.heldKey = key;
            this.limitIndex();

            if (key == GLFW.GLFW_KEY_V && (Keys.get(GLFW.GLFW_KEY_LEFT_CONTROL) || Keys.get(GLFW.GLFW_KEY_RIGHT_CONTROL))) {
                String clipboard = BlackOut.mc.keyboard.getClipboard();
                if (clipboard != null) {
                    for (char c : clipboard.toCharArray()) {
                        this.addChar(String.valueOf(c));
                    }
                }
                return;
            }

            switch (key) {
                case GLFW.GLFW_KEY_BACKSPACE:
                    if (this.typingIndex > 0) {
                        String pre = this.content.substring(0, this.typingIndex - 1);
                        String post = this.content.substring(this.typingIndex);
                        this.content = pre + post;
                        this.typingIndex--;
                        this.lastType = System.currentTimeMillis();
                    }
                    return;
                case GLFW.GLFW_KEY_RIGHT:
                    this.lastType = System.currentTimeMillis();
                    this.typingIndex = MathHelper.clamp(this.typingIndex + 1, 0, this.content.length());
                    return;
                case GLFW.GLFW_KEY_LEFT:
                    this.lastType = System.currentTimeMillis();
                    this.typingIndex = MathHelper.clamp(this.typingIndex - 1, 0, this.content.length());
                    return;
                case GLFW.GLFW_KEY_LEFT_BRACKET:
                    this.addChar(Keys.get(GLFW.GLFW_KEY_LEFT_SHIFT) ? "{" : "[");
                    return;
                case GLFW.GLFW_KEY_RIGHT_BRACKET:
                    this.addChar(Keys.get(GLFW.GLFW_KEY_LEFT_SHIFT) ? "}" : "]");
                    return;
                case GLFW.GLFW_KEY_SPACE:
                    this.addChar(" ");
                    return;
                default:
                    if (key >= 48 && key <= 57) {
                        String num = String.valueOf(key - 48);
                        this.addChar(this.modify(num));
                        return;
                    }

                    String name = GLFW.glfwGetKeyName(key, 0);
                    if (name != null) {
                        this.addChar(this.modify(name));
                    }
                    break;
            }
        }
    }

    private void limitIndex() {
        this.typingIndex = MathHelper.clamp(this.typingIndex, 0, this.content.length());
    }

    private String modify(String string) {
        boolean shift = Keys.get(GLFW.GLFW_KEY_LEFT_SHIFT) || Keys.get(GLFW.GLFW_KEY_RIGHT_SHIFT);
        boolean alt = Keys.get(GLFW.GLFW_KEY_LEFT_ALT) || Keys.get(GLFW.GLFW_KEY_RIGHT_ALT);

        if (shift && alt) return "";

        if (shift) {
            return shiftModified.getOrDefault(string, string.toUpperCase());
        } else {
            return alt ? altModified.getOrDefault(string, string) : string.toLowerCase();
        }
    }

    private void addChar(String c) {
        if (c == null || c.isEmpty()) return;

        String pre = this.content.substring(0, this.typingIndex);
        String post = this.content.substring(this.typingIndex);

        this.content = pre + c + post;
        this.typingIndex += c.length();
        this.lastType = System.currentTimeMillis();
    }

    public boolean isEmpty() {
        return this.content.isEmpty();
    }

    public void clear() {
        this.content = "";
        this.typingIndex = 0;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
