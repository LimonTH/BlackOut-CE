package bodevelopment.client.blackout.gui.clickgui;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;

public class Component {
    protected final MatrixStack stack;
    public int x = 0;
    public int y = 0;
    public double mx = 0.0;
    public double my = 0.0;
    public float width = 0.0F;
    public float frameTime = 0.0F;

    public Component(MatrixStack stack) {
        this.stack = stack;
    }

    public float onRender(float frameTime, float width, int x, int y, double mx, double my) {
        this.frameTime = frameTime;
        this.x = x;
        this.y = y;
        this.mx = mx;
        this.my = my;
        this.width = width;
        return this.render();
    }

    public float render() {
        return 0.0F;
    }

    public boolean onMouse(int button, boolean state) {
        return false;
    }

    public void onKey(int key, boolean state) {
    }

    public void rounded(float x, float y, float width, float height, float radius, float shadowRad, Color color, Color shadow) {
        RenderUtils.rounded(this.stack, x, y, width, height, radius, shadowRad, color.getRGB(), shadow.getRGB());
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
