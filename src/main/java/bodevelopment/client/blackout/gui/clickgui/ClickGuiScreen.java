package bodevelopment.client.blackout.gui.clickgui;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.helpers.ScrollHelper;
import bodevelopment.client.blackout.helpers.SmoothScrollHelper;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.rendering.framebuffer.GuiAlphaFrameBuffer;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.render.AnimUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

public class ClickGuiScreen {
    protected final MatrixStack stack;
    protected final float width;
    protected final float height;
    protected final ScrollHelper scroll;
    private final long openTime;
    private final String label;
    protected float x = 0;
    protected float y = 0;
    protected double mx = 0.0;
    protected double my = 0.0;
    protected float frameTime = 0.0F;
    protected float scale = 0.0F;
    protected float unscaled = 0.0F;
    protected float alpha = 0.0F;
    private boolean moving = false;
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private float length;

    public ClickGuiScreen(String label, float width, float height, boolean smooth) {
        this.openTime = System.currentTimeMillis();
        this.stack = new MatrixStack();
        this.width = width;
        this.height = height + 40.0F;
        this.scroll = smooth
                ? new SmoothScrollHelper(0.5F, 20.0F, () -> 0.0F, () -> Math.max(this.length - height, 0.0F)).limit(5.0F)
                : new ScrollHelper(0.5F, 20.0F, () -> 0.0F, () -> Math.max(this.length - height, 0.0F)).limit(5.0F);
        this.label = label;
    }

    public void onRender(float frameTime, double mouseX, double mouseY) {
        GuiAlphaFrameBuffer frameBuffer = Managers.FRAME_BUFFER.getGui();
        frameBuffer.start();

        float popUpDelta = (float) MathHelper.clamp(System.currentTimeMillis() - this.openTime, 0L, 500L) / 500.0F;
        popUpDelta = (float) AnimUtils.easeOutBack(popUpDelta);
        this.unscaled = popUpDelta;
        float currentScale = RenderUtils.getScale();
        this.scale = this.unscaled / (currentScale == 0 ? 1 : currentScale);

        RenderUtils.startClickGui(this.stack, this.unscaled, this.scale, this.width, this.height, this.x, this.y);

        this.frameTime = frameTime;
        var window = BlackOut.mc.getWindow();
        double sw = window.getWidth();
        double sh = window.getHeight();

        double startX = (sw / 2.0 + this.x - this.width / 2.0F) / this.unscaled;
        double startY = (sh / 2.0 + this.y - this.height / 2.0F) / this.unscaled;
        this.mx = mouseX / this.scale - startX;
        this.my = mouseY / this.scale - startY;

        this.length = this.getLength();
        this.updatePos();
        this.scroll.update(frameTime);

        this.rounded(0.0F, -40.0F, this.width, this.height, 10.0F, 20.0F, GuiColorUtils.bg2, ColorUtils.SHADOW100);
        this.text(this.label, 2.5F, this.width / 2.0F, -25.0F, true, true, Color.GRAY);

        double cutLineTop = sh - ((sh / 2.0) + this.y - (this.height / 2.0 * this.unscaled) + (0.0 * this.unscaled));
        double contentHeightPx = (this.height - 40.0F) * this.unscaled;
        double cutLineBottom = cutLineTop - contentHeightPx;

        if (this.unscaled > 0.05F) {
            GlStateManager._enableScissorTest();

            int glY = (int) Math.round(cutLineBottom);
            int glH = (int) Math.round(cutLineTop - cutLineBottom);

            GlStateManager._scissorBox(0, Math.max(0, glY), (int) sw, Math.max(0, glH));
            this.render();

            RenderUtils.endScissor();
        } else {
            this.render();
        }

        RenderUtils.bottomFade(this.stack, -10.0F, 0.0F, this.width + 20.0F, 20.0F, new Color(0, 0, 0, 100).getRGB());

        this.stack.pop();
        frameBuffer.end(this.getAlpha());
    }

    /**
     * Исправленная прозрачность.
     * Теперь она берется напрямую из ClickGui, что позволяет окнам исчезать
     * вместе с интерфейсом даже в главном меню.
     */
    private float getAlpha() {
        if (Managers.CLICK_GUI.CLICK_GUI.isAnimating()) {
            return Math.min(ClickGui.popUpDelta * 1.5F, 1.0F);
        }
        return 1.0F;
    }

    private void endAlpha() {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        Renderer.setAlpha(1.0F);
    }

    protected float getLength() {
        return 0.0F;
    }

    private void updatePos() {
        if (this.moving) {
            this.x = (float) (this.x + (this.mx - this.offsetX));
            this.y = (float) (this.y + (this.my - this.offsetY));
        }
    }

    public void render() {
    }

    public void onMouse(int button, boolean state) {
    }

    public void onKey(int key, boolean state) {
    }

    public void onClose() {
    }

    public boolean handleMouse(int button, boolean state) {
        boolean m = false;
        if (!state) {
            this.onMouse(button, false);
            m = true;
            this.moving = false;
        }

        if (!this.insideBounds()) {
            return false;
        } else {
            if (this.my < 0.0 && state) {
                if (button == 0) {
                    this.moving = true;
                    this.offsetX = this.mx;
                    this.offsetY = this.my;
                } else if (button == 1) {
                    Managers.CLICK_GUI.openScreen(null);
                }
            }

            if (!m) {
                this.onMouse(button, true);
            }

            return true;
        }
    }

    public boolean handleScroll(double horizontal, double vertical) {
        if (!this.insideBounds()) {
            return false;
        } else {
            if (this.insideScrollBounds()) {
                this.scroll.add(vertical);
            }

            return true;
        }
    }

    public boolean handleKey(int key, boolean state) {
        if (!this.insideBounds()) {
            return false;
        } else {
            if (this.insideKeyBounds()) {
                this.onKey(key, state);
            }

            return true;
        }
    }

    protected boolean insideBounds() {
        return this.mx > -10.0 && this.mx < this.width + 10.0F && this.my > -50.0 && this.my < this.height - 30.0F;
    }

    protected boolean insideScrollBounds() {
        return this.insideBounds();
    }

    protected boolean insideKeyBounds() {
        return this.insideBounds();
    }

    public void rounded(float x, float y, float width, float height, float radius, float shadowRad, Color color, Color shadowColor) {
        RenderUtils.rounded(this.stack, x, y, width, height, radius, shadowRad, color.getRGB(), shadowColor.getRGB());
    }

    public void text(String string, float scale, float x, float y, boolean xCenter, boolean yCenter, Color color) {
        BlackOut.FONT.text(this.stack, string, scale, x, y, color, xCenter, yCenter);
    }

    public void line(float x1, float y1, float x2, float y2, Color color) {
        RenderUtils.line(this.stack, x1, y1, x2, y2, color.getRGB());
    }

    public void quad(float x, float y, float w, float h, Color color) {
        RenderUtils.quad(this.stack, x, y, w, h, color.getRGB());
    }

    public void fadeLine(float x1, float y1, float x2, float y2, Color color) {
        RenderUtils.fadeLine(this.stack, x1, y1, x2, y2, color.getRGB());
    }
}