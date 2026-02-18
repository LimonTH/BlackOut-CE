package bodevelopment.client.blackout.gui.clickgui.screens;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.TextField;
import bodevelopment.client.blackout.gui.clickgui.ClickGuiScreen;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.client.ThemeSettings;
import bodevelopment.client.blackout.module.setting.settings.ColorSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.rendering.shader.Shaders;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.SelectedComponent;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.awt.*;

public class ColorScreen extends ClickGuiScreen {
    private static final int offset2 = 200;
    private final ColorSetting colorSetting;
    private final float[] colorX = new float[7];
    private final float[] themeX = new float[3];
    private final ColorField[] themeFields = new ColorField[3];
    private final ColorField[] textFields = new ColorField[6];
    private final Color transparent = new Color(0, 0, 0, 0);
    private int selecting = 0;
    private float prevCircleX = 0.0F;
    private float prevCircleY = 0.0F;
    private float prevHueX = 0.0F;

    public ColorScreen(ColorSetting colorSetting, String label) {
        super(label, 700.0F, 400.0F, true);
        this.colorSetting = colorSetting;

        for (int i = 0; i < this.textFields.length; i++) {
            this.textFields[i] = new ColorField();
        }

        for (int i = 0; i < this.themeFields.length; i++) {
            this.themeFields[i] = new ColorField();
        }
    }

    @Override
    public void render() {
        RenderUtils.rounded(this.stack, 0, 0, width, height, 10, 10, GuiColorUtils.bg1.getRGB(), ColorUtils.SHADOW100I);

        this.renderSidebarContent();

        int screenWidth = BlackOut.mc.getWindow().getWidth();
        int screenHeight = BlackOut.mc.getWindow().getHeight();

        float realX = (screenWidth / 2f + (x - width / 2f) * unscaled);
        float realY = (screenHeight / 2f + (y - height / 2f) * unscaled);

        int scX = (int) (realX + 185 * unscaled);
        int scY = (int) realY;
        int scW = (int) (width * unscaled - 185 * unscaled);
        int scH = (int) (height * unscaled);

        int invertedY = screenHeight - (scY + scH);

        GlStateManager._enableScissorTest();
        GlStateManager._scissorBox(Math.max(0, scX), Math.max(0, invertedY), Math.max(0, scW), Math.max(0, scH));

        this.stack.push();
        this.stack.translate(200.0F, 0.0F, 0.0F);

        double rawMx = this.mx;
        this.mx -= 200.0F;

        this.renderPicker();
        if (this.colorSetting.theme > 0) {
            this.renderThemeBars();
        } else {
            this.renderBars();
        }

        this.mx = rawMx;
        this.stack.pop();

        GlStateManager._disableScissorTest();

        this.updateFieldsFocus();
    }

    private void renderSidebarContent() {
        RenderUtils.leftFade(this.stack, 175.0F, 0.0F, 10.0F, height, new Color(0, 0, 0, 80).getRGB());

        int bgClr = new Color(15, 15, 15, 255).getRGB();
        String[] labels = {"Normal", "Theme 1", "Theme 2"};

        for (int i = 0; i < 3; i++) {
            float yPos = 20.0F + (i * 40.0F);
            int currentBg = (this.colorSetting.theme == i) ? new Color(40, 40, 40).getRGB() : bgClr;

            RenderUtils.rounded(this.stack, 10, yPos, 155, 30, 4, 0, currentBg, 0);
            BlackOut.FONT.text(this.stack, labels[i], 1.8F, 20, yPos + 15, Color.WHITE, false, true);

            int preview = (i == 0) ? colorSetting.actual.getRGB() :
                    (i == 1 ? ThemeSettings.getInstance().getMain() : ThemeSettings.getInstance().getSecond());
            RenderUtils.rounded(this.stack, 130, yPos + 8, 25, 14, 3, 0, preview, 0);
        }
    }

    private void updateFieldsFocus() {
        for (ColorField field : (this.colorSetting.theme == 0 ? this.textFields : this.themeFields)) {
            field.textField.setActive(SelectedComponent.is(field.selectedId));
        }
    }

    private void handleSliders() {
        switch (this.selecting) {
            case 1: {
                float clickSat = (float) MathHelper.clamp(MathHelper.getLerpProgress(this.mx, 0.0, 500.0), 0.0, 1.0);
                float clickBri = (float) (1.0 - MathHelper.clamp(MathHelper.getLerpProgress(this.my, 10.0, 210.0), 0.0, 1.0));
                float[] HSB = this.getHSB(false);
                int rgb = Color.HSBtoRGB(HSB[0], clickSat, clickBri);
                int red = ColorHelper.Argb.getRed(rgb);
                int green = ColorHelper.Argb.getGreen(rgb);
                int blue = ColorHelper.Argb.getBlue(rgb);
                this.colorSetting.get().set(red, green, blue);
                break;
            }
            case 2: {
                float clickHue = (float) MathHelper.clamp(MathHelper.getLerpProgress(this.mx, 0.0, 500.0), 0.0, 1.0);
                float[] HSB = this.getHSB(false);
                int rgb = Color.HSBtoRGB(clickHue, HSB[1], HSB[2]);
                int red = ColorHelper.Argb.getRed(rgb);
                int green = ColorHelper.Argb.getGreen(rgb);
                int blue = ColorHelper.Argb.getBlue(rgb);
                this.colorSetting.get().set(red, green, blue);
                break;
            }
            case 3: {
                float progress = (float) MathHelper.clamp(MathHelper.getLerpProgress(this.mx, 255.0, 500.0), 0.0, 1.0) * 255.0F;
                this.colorSetting.get().setRed((int) progress);
                break;
            }
            case 4: {
                float progress = (float) MathHelper.clamp(MathHelper.getLerpProgress(this.mx, 255.0, 500.0), 0.0, 1.0) * 255.0F;
                this.colorSetting.get().setGreen((int) progress);
                break;
            }
            case 5: {
                float progress = (float) MathHelper.clamp(MathHelper.getLerpProgress(this.mx, 255.0, 500.0), 0.0, 1.0) * 255.0F;
                this.colorSetting.get().setBlue((int) progress);
                break;
            }
            case 6: {
                float progress = (float) MathHelper.clamp(MathHelper.getLerpProgress(this.mx, 0.0, 245.0), 0.0, 1.0) * 255.0F;
                this.colorSetting.get().setAlpha((int) progress);
                break;
            }
            case 7: {
                float progress = (float) MathHelper.clamp(MathHelper.getLerpProgress(this.mx, 0.0, 245.0), 0.0, 1.0);
                float[] HSB = this.getHSB(false);
                int rgb = Color.HSBtoRGB(HSB[0], progress, HSB[2]);
                int red = ColorHelper.Argb.getRed(rgb);
                int green = ColorHelper.Argb.getGreen(rgb);
                int blue = ColorHelper.Argb.getBlue(rgb);
                this.colorSetting.get().set(red, green, blue);
                break;
            }
            case 8: {
                float progress = (float) MathHelper.clamp(MathHelper.getLerpProgress(this.mx, 0.0, 245.0), 0.0, 1.0);
                float[] HSB = this.getHSB(false);
                int rgb = Color.HSBtoRGB(HSB[0], HSB[1], progress);
                int red = ColorHelper.Argb.getRed(rgb);
                int green = ColorHelper.Argb.getGreen(rgb);
                int blue = ColorHelper.Argb.getBlue(rgb);
                this.colorSetting.get().set(red, green, blue);
            }
        }
    }

    private void handleThemeSliders() {
        switch (this.selecting) {
            case 1:
                this.colorSetting.saturation = (float) MathHelper.clamp(MathHelper.getLerpProgress(this.mx, 0.0, 500.0), 0.0, 1.0) * 2.0F - 1.0F;
                break;
            case 2:
                this.colorSetting.brightness = (float) MathHelper.clamp(MathHelper.getLerpProgress(this.mx, 0.0, 500.0), 0.0, 1.0) * 2.0F - 1.0F;
                break;
            case 3:
                this.colorSetting.alpha = (int) (MathHelper.clamp(MathHelper.getLerpProgress(this.mx, 0.0, 500.0), 0.0, 1.0) * 255.0);
        }
    }

    @Override
    protected boolean insideScrollBounds() {
        return this.mx > -10.0 && this.mx < 175.0 && this.my > -50.0 && this.my < this.height + 10.0F;
    }

    @Override
    public void onMouse(int button, boolean state) {
        if (button == 0) {
            boolean b = false;

            for (ColorField textField : this.colorSetting.theme == 0 ? this.textFields : this.themeFields) {
                if (textField.textField.click(0, state)) {
                    b = true;
                    SelectedComponent.setId(textField.selectedId);
                    break;
                }
            }

            if (state && !b) {
                if (this.colorSetting.theme == 0) {
                    if (this.inside(200, 700, 10, 210)) {
                        this.selecting = 1;
                    }

                    if (this.inside(200, 700, 225, 238)) {
                        this.selecting = 2;
                    }

                    if (this.inside(455, 700, 260, 273)) {
                        this.selecting = 3;
                    }

                    if (this.inside(455, 700, 300, 313)) {
                        this.selecting = 4;
                    }

                    if (this.inside(455, 700, 340, 353)) {
                        this.selecting = 5;
                    }

                    if (this.inside(200, 445, 340, 353)) {
                        this.selecting = 6;
                    }

                    if (this.inside(200, 445, 260, 273)) {
                        this.selecting = 7;
                    }

                    if (this.inside(200, 445, 300, 313)) {
                        this.selecting = 8;
                    }
                } else {
                    if (this.inside(200, 700, 260, 273)) {
                        this.selecting = 1;
                    }

                    if (this.inside(200, 700, 300, 313)) {
                        this.selecting = 2;
                    }

                    if (this.inside(200, 700, 340, 353)) {
                        this.selecting = 3;
                    }
                }

                if (this.selecting > 0) {
                    Managers.CONFIG.saveAll();
                } else if (this.inside(0, 200, 20, 50)) {
                    this.colorSetting.theme = 0;
                    Managers.CONFIG.saveAll();
                } else if (this.inside(0, 200, 60, 90)) {
                    this.colorSetting.theme = 1;
                    Managers.CONFIG.saveAll();
                } else if (this.inside(0, 200, 100, 130)) {
                    this.colorSetting.theme = 2;
                    Managers.CONFIG.saveAll();
                }
            } else {
                this.selecting = 0;
            }
        }
    }

    @Override
    public void onKey(int key, boolean state) {
        if (key == 257) {
            ColorField field = null;

            for (ColorField colorField : this.textFields) {
                if (SelectedComponent.is(colorField.selectedId)) {
                    field = colorField;
                    break;
                }
            }

            for (ColorField colorFieldx : this.themeFields) {
                if (SelectedComponent.is(colorFieldx.selectedId)) {
                    field = colorFieldx;
                    break;
                }
            }

            if (field != null) {
                SelectedComponent.reset();
            }
        } else {
            for (ColorField field : this.textFields) {
                field.textField.type(key, state);
            }

            for (ColorField field : this.themeFields) {
                field.textField.type(key, state);
            }
        }
    }

    private void setValue(ColorField colorField, int id) {
        int val = 0;

        try {
            val = Integer.parseInt(colorField.textField.getContent());
        } catch (NumberFormatException ignored) {
        }

        val = MathHelper.clamp(val, 0, 255);
        switch (id) {
            case 0:
                this.colorSetting.get().setRed(val);
                break;
            case 1:
                this.colorSetting.get().setGreen(val);
                break;
            case 2:
                this.colorSetting.get().setBlue(val);
                break;
            case 3:
                this.colorSetting.get().setAlpha(val);
                break;
            case 4: {
                float[] hsb = this.getHSB(false);
                int rgb = Color.HSBtoRGB(hsb[0], val / 255.0F, hsb[2]);
                this.colorSetting.get().set(ColorHelper.Argb.getRed(rgb), ColorHelper.Argb.getGreen(rgb), ColorHelper.Argb.getBlue(rgb));
                break;
            }
            case 5: {
                float[] hsb = this.getHSB(false);
                int rgb = Color.HSBtoRGB(hsb[0], hsb[1], val / 255.0F);
                this.colorSetting.get().set(ColorHelper.Argb.getRed(rgb), ColorHelper.Argb.getGreen(rgb), ColorHelper.Argb.getBlue(rgb));
                break;
            }
            case 6:
                try {
                    this.colorSetting.saturation = Float.parseFloat(colorField.textField.getContent());
                } catch (NumberFormatException ignored) {
                }
                break;
            case 7:
                try {
                    this.colorSetting.brightness = Float.parseFloat(colorField.textField.getContent());
                } catch (NumberFormatException ignored) {
                }
                break;
            case 8:
                try {
                    this.colorSetting.alpha = val;
                } catch (NumberFormatException ignored) {
                }
        }
    }

    private boolean inside(int minX, int maxX, int minY, int maxY) {
        return this.mx >= minX && this.mx <= maxX && this.my >= minY && this.my <= maxY;
    }

    private void renderHueBar(float x, float y, float w, float h) {
        float hue = this.getHSB(false)[0];
        RenderUtils.roundedShadow(this.stack, x, y, w, h, 0.0F, 10.0F, new Color(0, 0, 0, 100).getRGB());
        this.renderHueQuad(x, y, w, h);
        float hueX;
        if (this.selecting == 2) {
            hueX = (float) MathHelper.clamp(this.mx, 0.0, 500.0);
        } else {
            hueX = MathHelper.lerp(hue, x, x + w);
        }

        this.prevHueX = MathHelper.clampedLerp(this.prevHueX, hueX, this.frameTime * 20.0F);
        RenderUtils.roundedShadow(this.stack, this.prevHueX, y, 0.0F, h, 0.0F, 10.0F, new Color(0, 0, 0, 100).getRGB());
        RenderUtils.quad(
                this.stack, this.prevHueX - 3.0F, y - 2.0F, 6.0F, h + 4.0F, Color.HSBtoRGB(MathHelper.getLerpProgress(this.prevHueX, 0.0F, 500.0F), 1.0F, 1.0F)
        );
    }

    private void renderBars() {
        this.renderHueBar(0.0F, 225.0F, 500.0F, 13.0F);
        this.renderBar(255.0F, 260.0F, 245.0F, 0, this.colorSetting.get().red / 255.0F, "Red");
        this.renderBar(255.0F, 300.0F, 245.0F, 1, this.colorSetting.get().green / 255.0F, "Green");
        this.renderBar(255.0F, 340.0F, 245.0F, 2, this.colorSetting.get().blue / 255.0F, "Blue");
        this.renderBar(0.0F, 340.0F, 245.0F, 3, this.colorSetting.get().alpha / 255.0F, "Alpha");
        this.renderBar(0.0F, 260.0F, 245.0F, 4, this.getHSB(false)[1], "Saturation");
        this.renderBar(0.0F, 300.0F, 245.0F, 5, this.getHSB(false)[2], "Brightness");
    }

    private void renderThemeBars() {
        this.renderThemeBar(0.0F, 260.0F, 500.0F, 0, this.colorSetting.saturation, this.colorSetting.saturation / 2.0F + 0.5F, "Saturation");
        this.renderThemeBar(0.0F, 300.0F, 500.0F, 1, this.colorSetting.brightness, this.colorSetting.brightness / 2.0F + 0.5F, "Brightness");
        this.renderThemeBar(0.0F, 340.0F, 500.0F, 2, this.colorSetting.alpha, this.colorSetting.alpha / 255.0F, "Alpha");
    }

    private void renderThemeBar(float x, float y, float w, int id, float number, float p, String name) {
        this.themeX[id] = MathHelper.lerp(Math.min(this.frameTime * 20.0F, 1.0F), this.themeX[id], p);
        Color left;
        Color right = switch (id) {
            case 1 -> {
                left = Color.BLACK;
                yield Color.WHITE;
            }
            case 2 -> {
                left = new Color(255, 255, 255, 0);
                yield Color.WHITE;
            }
            default -> {
                left = Color.WHITE;
                yield Color.RED;
            }
        };

        RenderUtils.roundedShadow(this.stack, x, y, w, 13.0F, 0.0F, 10.0F, ColorUtils.SHADOW100I);
        this.renderQuad(
                x,
                y,
                w,
                13.0F,
                left.getRed() / 255.0F,
                left.getGreen() / 255.0F,
                left.getBlue() / 255.0F,
                left.getAlpha() / 255.0F,
                right.getRed() / 255.0F,
                right.getGreen() / 255.0F,
                right.getBlue() / 255.0F,
                right.getAlpha() / 255.0F
        );
        RenderUtils.roundedShadow(this.stack, x + w * this.themeX[id], y - 2.0F, 0.0F, 17.0F, 0.0F, 10.0F, ColorUtils.SHADOW100I);
        RenderUtils.quad(this.stack, x - 3.0F + w * this.themeX[id], y - 2.0F, 6.0F, 17.0F, ColorUtils.lerpColor(this.themeX[id], left, right).getRGB());
        BlackOut.FONT.text(this.stack, name, 1.5F, x, y - 10.0F, Color.WHITE, false, true);
        ColorField field = this.themeFields[id];
        if (!SelectedComponent.is(field.selectedId)) {
            field.textField.setContent(id == 2 ? String.valueOf((int) number) : String.format("%.2f", number));
        } else {
            this.setValue(field, id + 6);
        }

        field.textField.render(this.stack, 1.5F, this.mx, this.my, x + 210.0F, y - 12.0F, 22.0F, 3.0F, 5.0F, 3.0F, Color.WHITE, this.transparent);
    }

    private void renderBar(float x, float y, float w, int id, float p, String name) {
        this.colorX[id] = MathHelper.lerp(Math.min(this.frameTime * 20.0F, 1.0F), this.colorX[id], p);
        BlackOutColor left = this.colorSetting.get().copy();
        BlackOutColor right = this.colorSetting.get().copy();
        switch (id) {
            case 0:
                left.setRed(0);
                right.setRed(255);
                left.setAlpha(255);
                right.setAlpha(255);
                break;
            case 1:
                left.setGreen(0);
                right.setGreen(255);
                left.setAlpha(255);
                right.setAlpha(255);
                break;
            case 2:
                left.setBlue(0);
                right.setBlue(255);
                left.setAlpha(255);
                right.setAlpha(255);
                break;
            case 3:
                left.setAlpha(0);
                right.setAlpha(255);
                break;
            case 4: {
                left.setRed(255);
                left.setGreen(255);
                left.setBlue(255);
                float[] HSB = this.getHSB(false);
                int rgb = Color.HSBtoRGB(HSB[0], 1.0F, HSB[2]);
                int red = ColorHelper.Argb.getRed(rgb);
                int green = ColorHelper.Argb.getGreen(rgb);
                int blue = ColorHelper.Argb.getBlue(rgb);
                right.setRed(red);
                right.setGreen(green);
                right.setBlue(blue);
                left.setAlpha(255);
                right.setAlpha(255);
                break;
            }
            case 5: {
                left.setRed(0);
                left.setGreen(0);
                left.setBlue(0);
                float[] HSB = this.getHSB(false);
                int rgb = Color.HSBtoRGB(HSB[0], HSB[1], 1.0F);
                int red = ColorHelper.Argb.getRed(rgb);
                int green = ColorHelper.Argb.getGreen(rgb);
                int blue = ColorHelper.Argb.getBlue(rgb);
                right.setRed(red);
                right.setGreen(green);
                right.setBlue(blue);
                left.setAlpha(255);
                right.setAlpha(255);
            }
        }

        RenderUtils.roundedShadow(this.stack, x, y, w, 13.0F, 0.0F, 10.0F, new Color(0, 0, 0, 100).getRGB());
        this.renderQuad(
                x,
                y,
                w,
                13.0F,
                left.red / 255.0F,
                left.green / 255.0F,
                left.blue / 255.0F,
                left.alpha / 255.0F,
                right.red / 255.0F,
                right.green / 255.0F,
                right.blue / 255.0F,
                right.alpha / 255.0F
        );
        RenderUtils.roundedShadow(this.stack, x + w * this.colorX[id], y - 2.0F, 0.0F, 17.0F, 0.0F, 10.0F, new Color(0, 0, 0, 100).getRGB());
        RenderUtils.quad(this.stack, x - 3.0F + w * this.colorX[id], y - 2.0F, 6.0F, 17.0F, left.lerp(this.colorX[id], right).getColor().getRGB());
        BlackOut.FONT.text(this.stack, name, 1.5F, x, y - 10.0F, Color.WHITE, false, true);
        ColorField field = this.textFields[id];
        if (!SelectedComponent.is(field.selectedId)) {
            field.textField.setContent(String.valueOf(Math.round(p * 255.0F)));
        } else {
            this.setValue(field, id);
        }

        field.textField.render(this.stack, 1.5F, this.mx, this.my, x + 210.0F, y - 12.0F, 22.0F, 3.0F, 5.0F, 3.0F, Color.WHITE, this.transparent);
    }

    private void renderQuad(float x, float y, float w, float h, float rl, float gl, float bl, float al, float rr, float gr, float br, float ar) {
        Matrix4f matrix4f = this.stack.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, x + w, y, 0.0F).color(rr, gr, br, ar);
        bufferBuilder.vertex(matrix4f, x, y, 0.0F).color(rl, gl, bl, al);
        bufferBuilder.vertex(matrix4f, x, y + h, 0.0F).color(rl, gl, bl, al);
        bufferBuilder.vertex(matrix4f, x + w, y + h, 0.0F).color(rr, gr, br, ar);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    private void renderHueQuad(float x, float y, float w, float h) {
        Matrix4f matrix4f = this.stack.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, x + w / 6.0F * 6.0F, y, 0.0F).color(1.0F, 0.0F, 0.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w / 6.0F * 5.0F, y, 0.0F).color(1.0F, 0.0F, 1.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w / 6.0F * 5.0F, y + h, 0.0F).color(1.0F, 0.0F, 1.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w / 6.0F * 6.0F, y + h, 0.0F).color(1.0F, 0.0F, 0.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w / 6.0F * 5.0F, y, 0.0F).color(1.0F, 0.0F, 1.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w / 6.0F * 4.0F, y, 0.0F).color(0.0F, 0.0F, 1.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w / 6.0F * 4.0F, y + h, 0.0F).color(0.0F, 0.0F, 1.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w / 6.0F * 5.0F, y + h, 0.0F).color(1.0F, 0.0F, 1.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w / 6.0F * 4.0F, y, 0.0F).color(0.0F, 0.0F, 1.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w / 6.0F * 3.0F, y, 0.0F).color(0.0F, 1.0F, 1.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w / 6.0F * 3.0F, y + h, 0.0F).color(0.0F, 1.0F, 1.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w / 6.0F * 4.0F, y + h, 0.0F).color(0.0F, 0.0F, 1.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w / 6.0F * 3.0F, y, 0.0F).color(0.0F, 1.0F, 1.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w / 6.0F * 2.0F, y, 0.0F).color(0.0F, 1.0F, 0.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w / 6.0F * 2.0F, y + h, 0.0F).color(0.0F, 1.0F, 0.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w / 6.0F * 3.0F, y + h, 0.0F).color(0.0F, 1.0F, 1.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w / 6.0F * 2.0F, y, 0.0F).color(0.0F, 1.0F, 0.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w / 6.0F, y, 0.0F).color(1.0F, 1.0F, 0.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w / 6.0F, y + h, 0.0F).color(1.0F, 1.0F, 0.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w / 6.0F * 2.0F, y + h, 0.0F).color(0.0F, 1.0F, 0.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w / 6.0F, y, 0.0F).color(1.0F, 1.0F, 0.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x, y, 0.0F).color(1.0F, 0.0F, 0.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x, y + h, 0.0F).color(1.0F, 0.0F, 0.0F, 1.0F);
        bufferBuilder.vertex(matrix4f, x + w / 6.0F, y + h, 0.0F).color(1.0F, 1.0F, 0.0F, 1.0F);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    private void renderPicker() {
        int rgb = Color.HSBtoRGB(this.getHSB(true)[0], 1.0F, 1.0F);
        int red = ColorHelper.Argb.getRed(rgb);
        int green = ColorHelper.Argb.getGreen(rgb);
        int blue = ColorHelper.Argb.getBlue(rgb);
        this.renderPickerQuad(10.0F, 500.0F, 200.0F, red / 255.0F, green / 255.0F, blue / 255.0F);
        float circleX;
        float circleY;
        if (this.colorSetting.theme == 0 && this.selecting == 1) {
            circleX = (float) MathHelper.clamp(this.mx, 0.0, 500.0);
            circleY = (float) MathHelper.clamp(this.my, 10.0, 210.0);
        } else {
            float[] HSB = this.getHSB(false);
            circleX = HSB[1] * 500.0F;
            circleY = MathHelper.lerp(HSB[2], 210, 10);
        }

        this.prevCircleX = MathHelper.clampedLerp(this.prevCircleX, circleX, this.frameTime * 20.0F);
        this.prevCircleY = MathHelper.clampedLerp(this.prevCircleY, circleY, this.frameTime * 20.0F);
        BlackOutColor color = this.colorSetting.get();
        RenderUtils.rounded(
                this.stack, this.prevCircleX, this.prevCircleY, 0.0F, 0.0F, 10.0F, 4.0F, ColorUtils.withAlpha(color.getRGB(), 255), ColorUtils.SHADOW100I
        );
    }

    private void renderPickerQuad(float oy, float w, float h, float red, float green, float blue) {
        Renderer.setMatrices(this.stack);
        Matrix4f matrix4f = Renderer.emptyMatrix;
        RenderSystem.enableBlend();
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        bufferBuilder.vertex(matrix4f, w, oy, 0.0F);
        bufferBuilder.vertex(matrix4f, 0.0F, oy, 0.0F);
        bufferBuilder.vertex(matrix4f, 0.0F, oy + h, 0.0F);
        bufferBuilder.vertex(matrix4f, w, oy + h, 0.0F);
        Shaders.picker.render(bufferBuilder, new ShaderSetup(setup -> {
            setup.set("pos", 0.0F, oy, w, h);
            setup.set("clr", red, green, blue);
        }));
        RenderSystem.disableBlend();
    }

    private float[] getHSB(boolean unmodified) {
        BlackOutColor value = unmodified ? this.colorSetting.getUnmodified() : this.colorSetting.get();
        return Color.RGBtoHSB(value.red, value.green, value.blue, new float[3]);
    }

    private static class ColorField {
        private final TextField textField = new TextField();
        private final int selectedId = SelectedComponent.nextId();
    }
}
