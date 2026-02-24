package bodevelopment.client.blackout.gui.clickgui;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.KeyEvent;
import bodevelopment.client.blackout.event.events.MouseButtonEvent;
import bodevelopment.client.blackout.event.events.MouseScrollEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.gui.clickgui.components.CategoryComponent;
import bodevelopment.client.blackout.gui.clickgui.components.ModuleComponent;
import bodevelopment.client.blackout.gui.clickgui.screens.SearchScreen;
import bodevelopment.client.blackout.helpers.ScrollHelper;
import bodevelopment.client.blackout.helpers.SmoothScrollHelper;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.ParentCategory;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.GuiSettings;
import bodevelopment.client.blackout.rendering.framebuffer.GuiAlphaFrameBuffer;
import bodevelopment.client.blackout.rendering.renderer.ColorRenderer;
import bodevelopment.client.blackout.rendering.renderer.TextureRenderer;
import bodevelopment.client.blackout.rendering.texture.BOTextures;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.GuiRenderUtils;
import bodevelopment.client.blackout.util.SelectedComponent;
import bodevelopment.client.blackout.util.render.AnimUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ClickGui extends Screen {
    public static float popUpDelta = 0.0F;
    public static float scale = 0.0F;
    public static float unscaled = 0.0F;
    public static float x = 0.0F;
    public static float y = 0.0F;
    public static float width = 1000.0F;
    public static float height = 700.0F;
    public static SubCategory selectedCategory = SubCategory.OFFENSIVE;
    public final List<ModuleComponent> moduleComponents = new ArrayList<>();
    public final List<CategoryComponent> categoryComponents = new ArrayList<>();
    private final MatrixStack stack = new MatrixStack();
    private final ClickGuiButtons buttons = new ClickGuiButtons();
    public long toggleTime = 0L;
    public float moduleLength = 0.0F;
    private final ScrollHelper moduleScroll = new SmoothScrollHelper(0.5F, 20.0F, () -> 0.0F, () -> Math.max(this.moduleLength - height + 40.0F, 0.0F))
            .limit(5.0F);
    public ClickGuiScreen openedScreen = null;
    private boolean upPressed = false;
    private boolean downPressed = false;
    private long pressTime = 0L;
    private long lastCategoryChange = 0L;
    private boolean initialClickDone = false;
    private long openTime = 0L;
    private boolean moving = false;
    private boolean scaling = false;
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private double mx = 0.0;
    private double my = 0.0;
    private float categoryOffset;
    private final ScrollHelper categoryScroll = new SmoothScrollHelper(0.5F, 20.0F, () -> 0.0F, () -> Math.max(this.categoryOffset - height + 110.0F, 0.0F))
            .limit(5.0F);
    private boolean open = false;
    private float frameTime;
    private float scaleDelta;
    public static String hoveredDescription = null;
    private String lastDescription = null;
    private long hoverTime = 0L;
    private float descAlpha = 0.0F;

    public ClickGui() {
        super(Text.of("Click GUI"));
    }

    public void initGui() {
        this.moduleComponents.clear();
        Managers.MODULES.getModules().forEach(m -> this.moduleComponents.add(new ModuleComponent(this.stack, m)));
        this.categoryComponents.clear();
        SubCategory.categories.forEach(c -> this.categoryComponents.add(new CategoryComponent(this.stack, c)));
    }

    public boolean isOpen() {
        return this.open;
    }

    public void setOpen(boolean open) {
        if (!this.isOpen() && open) {
            this.openTime = System.currentTimeMillis();
        }

        this.open = open;
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.world != null) {
            this.renderBlur();
        }
    }

    public void close() {
        this.setOpen(false);
        this.toggleTime = System.currentTimeMillis();

        if (BlackOut.mc.currentScreen == this) {
            super.close();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (this.client == null) this.client = BlackOut.mc;

        if (this.open) {
            popUpDelta = (float) MathHelper.clamp(System.currentTimeMillis() - this.toggleTime, 0L, 500L) / 500.0F;
            scale = (float) AnimUtils.easeOutBack(popUpDelta);
        } else {
            popUpDelta = 1.0F - (float) MathHelper.clamp(System.currentTimeMillis() - this.toggleTime, 0L, 250L) / 250.0F;
            if (popUpDelta <= 0.0F && this.openTime != 0) {
                this.close();
                return;
            }
            scale = (float) AnimUtils.easeOutQuart(popUpDelta);
        }

        hoveredDescription = null;

        this.frameTime = delta / 20.0F;

        if (this.openedScreen == null && (this.upPressed || this.downPressed)) {
            long now = System.currentTimeMillis();
            long timeSincePress = now - pressTime;

            if (!initialClickDone) {
                changeCategory(this.downPressed);
                lastCategoryChange = now;
                initialClickDone = true;
            } else if (timeSincePress > 500L) {
                if (now - lastCategoryChange > 50L) {
                    changeCategory(this.downPressed);
                    lastCategoryChange = now;
                }
            }
        } else if (this.openedScreen != null) {
            initialClickDone = false;
            this.upPressed = false;
            this.downPressed = false;
        } else {
            initialClickDone = false;
        }

        unscaled = scale;

        float currentScale = RenderUtils.getScale();
        scale = scale / (currentScale == 0 ? 1 : currentScale);

        double screenWidth = BlackOut.mc.getWindow().getWidth();
        double screenHeight = BlackOut.mc.getWindow().getHeight();

        double startX = (screenWidth / 2.0 + x - width / 2.0F) / unscaled;
        double startY = (screenHeight / 2.0 + y - height / 2.0F) / unscaled;

        this.mx = mouseX / scale - startX;
        this.my = mouseY / scale - startY;

        this.updatePos();
        this.updateScroll();

        RenderUtils.startClickGui(this.stack, unscaled, scale, width, height, x, y);
        GlStateManager._disableScissorTest();

        if (BlackOut.mc.world != null) {
            this.renderBlur();
        }

        GuiAlphaFrameBuffer frameBuffer = Managers.FRAME_BUFFER.getGui();
        frameBuffer.start();

        RenderUtils.roundedShadow(this.stack, 0.0F, 0.0F, width, height, 10.0F, 30.0F, new Color(0, 0, 0, 100).getRGB());
        RenderUtils.roundedRight(this.stack, 200.0F, 0.0F, width - 200.0F, height, 10.0F, 0.0F, GuiColorUtils.bg1.getRGB(), ColorUtils.SHADOW100I);
        RenderUtils.rightFade(this.stack, 180.0F, -10.0F, 55.0F, height + 20.0F, new Color(0, 0, 0, 100).getRGB());

        this.renderLogo();
        this.renderModules();
        this.renderFade();

        RenderUtils.rounded(this.stack, 0.0F, 0.0F, 200.0F, height, 10.0F, 2.0F, GuiColorUtils.bg2.getRGB(), ColorUtils.SHADOW100I);
        GuiRenderUtils.renderWaveText(this.stack, "Blackout", 4.5F, 100.0F, 50.0F, true, true, true);

        if (this.scaling) {
            this.scaleDelta = Math.min(this.scaleDelta + this.frameTime * 5.0F, 1.0F);
        } else {
            this.scaleDelta = Math.max(this.scaleDelta - this.frameTime * 2.0F, 0.0F);
        }

        if (this.scaleDelta > 0.0F) {
            int clr = ColorUtils.withAlpha(Color.WHITE.getRGB(), (int) (this.scaleDelta * 100.0F));
            RenderUtils.rounded(this.stack, width - 10.0F, height - 10.0F, 20.0F, 20.0F, 15.0F, 5.0F, clr, clr);
        }

        this.renderCategories(this.frameTime);
        frameBuffer.end(Math.min(popUpDelta * 1.5F, 1.0F));

        if (hoveredDescription != null && !hoveredDescription.isEmpty()) {
            if (!hoveredDescription.equals(lastDescription)) {
                if (descAlpha <= 0.3F) {
                    hoverTime = System.currentTimeMillis();
                }
                lastDescription = hoveredDescription;
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
        if (this.openedScreen == null) {
            if (descAlpha > 0.0F && lastDescription != null) {
                float textScale = 1.6F;
                float maxBoxWidth = 350.0F;
                List<String> lines = wrapText(lastDescription, maxBoxWidth / textScale);

                float finalWidth = 0;
                for (String line : lines) {
                    finalWidth = Math.max(finalWidth, BlackOut.FONT.getWidth(line) * textScale);
                }

                float lineHeight = BlackOut.FONT.getHeight() * textScale;
                float spacing = 2.0F;
                float finalHeight = lines.size() * lineHeight + (lines.size() - 1) * spacing;

                float rectX = (float) mx + 15;
                float rectY = (float) my + 15;

                if (rectX + finalWidth + 25 > width) rectX = (float) mx - finalWidth - 25;
                if (rectY + finalHeight + 25 > height) rectY = (float) my - finalHeight - 25;

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
            }
        } else {
            descAlpha = 0;
            this.openedScreen.onRender(this.frameTime, mouseX, mouseY);
        }

        this.buttons.render(mouseX, mouseY, this.openTime, this.open ? 1.0F : popUpDelta);
        this.stack.pop();
    }

    public void setScreen(ClickGuiScreen screen) {
        if (this.openedScreen != null) {
            this.openedScreen.onClose();
        }

        this.openedScreen = screen;
    }

    private void changeCategory(boolean down) {
        List<SubCategory> categories = SubCategory.categories;
        int currentIndex = categories.indexOf(selectedCategory);
        if (down) {
            selectedCategory = categories.get((currentIndex + 1) % categories.size());
        } else {
            selectedCategory = categories.get((currentIndex - 1 + categories.size()) % categories.size());
        }
    }

    private void renderFade() {
        ColorRenderer renderer = ColorRenderer.getInstance();
        renderer.startRender(this.stack, VertexFormat.DrawMode.TRIANGLE_FAN);
        renderer.vertex(
                235.0F, height - 10.0F, 0.0F, GuiColorUtils.bg1.getRed() / 255.0F, GuiColorUtils.bg1.getGreen() / 255.0F, GuiColorUtils.bg1.getBlue() / 255.0F, 0.0F
        );
        renderer.vertex(
                235.0F,
                height + 10.0F,
                0.0F,
                GuiColorUtils.bg1.getRed() / 255.0F,
                GuiColorUtils.bg1.getGreen() / 255.0F,
                GuiColorUtils.bg1.getBlue() / 255.0F,
                GuiColorUtils.bg1.getAlpha() / 255.0F
        );

        for (int i = 90; i >= 0; i -= 9) {
            float y = (float) (Math.sin(Math.toRadians(i)) * 10.0);
            renderer.vertex(
                    width + (float) Math.cos(Math.toRadians(i)) * 10.0F,
                    height + y,
                    0.0F,
                    GuiColorUtils.bg1.getRed() / 255.0F,
                    GuiColorUtils.bg1.getGreen() / 255.0F,
                    GuiColorUtils.bg1.getBlue() / 255.0F,
                    MathHelper.getLerpProgress(y, -10.0F, 10.0F)
            );
        }

        renderer.vertex(
                width + 10.0F,
                height - 10.0F,
                0.0F,
                GuiColorUtils.bg1.getRed() / 255.0F,
                GuiColorUtils.bg1.getGreen() / 255.0F,
                GuiColorUtils.bg1.getBlue() / 255.0F,
                0.0F
        );
        renderer.endRender();
        renderer.startRender(this.stack, VertexFormat.DrawMode.TRIANGLE_FAN);
        renderer.vertex(
                235.0F,
                -10.0F,
                0.0F,
                GuiColorUtils.bg1.getRed() / 255.0F,
                GuiColorUtils.bg1.getGreen() / 255.0F,
                GuiColorUtils.bg1.getBlue() / 255.0F,
                GuiColorUtils.bg1.getAlpha() / 255.0F
        );
        renderer.vertex(
                235.0F, 10.0F, 0.0F, GuiColorUtils.bg1.getRed() / 255.0F, GuiColorUtils.bg1.getGreen() / 255.0F, GuiColorUtils.bg1.getBlue() / 255.0F, 0.0F
        );
        renderer.vertex(
                width + 10.0F, 10.0F, 0.0F, GuiColorUtils.bg1.getRed() / 255.0F, GuiColorUtils.bg1.getGreen() / 255.0F, GuiColorUtils.bg1.getBlue() / 255.0F, 0.0F
        );

        for (int i = 360; i >= 270; i -= 9) {
            float y = (float) (Math.sin(Math.toRadians(i)) * 10.0);
            renderer.vertex(
                    width + (float) Math.cos(Math.toRadians(i)) * 10.0F,
                    y,
                    0.0F,
                    GuiColorUtils.bg1.getRed() / 255.0F,
                    GuiColorUtils.bg1.getGreen() / 255.0F,
                    GuiColorUtils.bg1.getBlue() / 255.0F,
                    MathHelper.getLerpProgress(y, 10.0F, -10.0F)
            );
        }

        renderer.endRender();
    }

    private void renderLogo() {
        GuiSettings guiSettings = GuiSettings.getInstance();
        float alpha = guiSettings.logoAlpha.get().floatValue();
        if (!(alpha <= 0.0F)) {
            TextureRenderer t = BOTextures.getLogoRenderer();
            float ts = 1200.0F * guiSettings.logoScale.get().floatValue();
            float cx = width - 100.0F;
            float cy = height - 350.0F;
            t.startRender(this.stack, cx - ts / 2.0F, cy - ts / 2.0F, ts, ts, 1.0F, 1.0F, 1.0F, 1.0F, alpha, VertexFormat.DrawMode.TRIANGLE_FAN);

            for (int i = 90; i >= 0; i -= 9) {
                t.vertex(width + Math.cos(Math.toRadians(i)) * 10.0, height + Math.sin(Math.toRadians(i)) * 10.0);
            }

            for (int i = 360; i >= 270; i -= 9) {
                t.vertex(width + Math.cos(Math.toRadians(i)) * 10.0, Math.sin(Math.toRadians(i)) * 10.0);
            }

            t.vertex(200.0F, -10.0F);
            t.vertex(200.0F, height + 10.0F);
            t.endRender();
        }
    }

    private void renderBlur() {
        if (GuiSettings.getInstance() == null) return;
        int blur = GuiSettings.getInstance().blur.get();
        if (blur > 0) {
            RenderUtils.blur(blur, popUpDelta);
        }
    }

    private void updatePos() {
        if (this.moving) {
            x = x - (float) (this.offsetX - this.mx);
            y = y - (float) (this.offsetY - this.my);
        } else if (this.scaling) {
            width = Math.max((float) (this.mx - this.offsetX), 500.0F);
            height = Math.max((float) (this.my - this.offsetY), 200.0F);
        }
    }

    private void renderModules() {
        int columns = this.getColumns();
        int current = 0;
        float[] lengths = new float[columns];
        int moduleWidth = this.getModuleWidth(columns);
        int offset = this.getColumnOffset(columns, moduleWidth);

        for (ModuleComponent component : this.moduleComponents) {
            if (component.module.category == selectedCategory) {
                lengths[current] += component.onRender(
                        this.frameTime,
                        moduleWidth,
                        210 + offset * (current + 1) + moduleWidth * current,
                        (int) (30.0F + lengths[current] - this.moduleScroll.get()),
                        this.mx,
                        this.my
                )
                        + 20.0F;
                if (++current >= lengths.length) {
                    current = 0;
                }
            }
        }

        float max = 0.0F;

        for (float f : lengths) {
            max = Math.max(max, f);
        }

        this.moduleLength = max;
    }

    private int getColumns() {
        return MathHelper.clamp((int) ((width - 200.0F) / 320.0F), 1, 4);
    }

    private int getColumnOffset(int columns, float moduleWidth) {
        return (int) ((width - 200.0F - columns * moduleWidth) / (columns + 1));
    }

    private int getModuleWidth(int columns) {
        return ((int) width - 200) / columns - 60;
    }

    private void renderCategories(float frameTime) {
        ParentCategory prevParent = null;
        float currentY = 110.0F - this.categoryScroll.get();

        GlStateManager._enableScissorTest();
        float sx = BlackOut.mc.getWindow().getWidth() / 2.0F - width / 2.0F * unscaled + x;
        float y1 = BlackOut.mc.getWindow().getHeight() / 2.0F - (height / 2.0F + 10.0F) * unscaled - y;
        float y2 = BlackOut.mc.getWindow().getHeight() / 2.0F + (height / 2.0F - 100.0F) * unscaled - y;
        float scissorHeight = Math.abs(y1 - y2);
        GlStateManager._scissorBox((int) sx, (int) y1, (int) (210.0F * unscaled), (int) scissorHeight);

        float totalHeightCounter = 0.0F;

        for (CategoryComponent c : this.categoryComponents) {
            ParentCategory p = c.category.parent();

            if (prevParent != p) {
                float headerSpacing = (prevParent != null) ? 15.0F : 20.0F;
                currentY += headerSpacing;
                totalHeightCounter += headerSpacing;

                this.renderParentCategory(p, currentY);

                float headerHeight = 25.0F;
                currentY += headerHeight;
                totalHeightCounter += headerHeight;
            }
            prevParent = p;

            c.x = 15;
            c.y = (int) currentY;
            c.mx = (float) this.mx;
            c.my = (float) this.my;
            c.frameTime = frameTime;

            float renderedHeight = c.render();

            currentY += renderedHeight;
            totalHeightCounter += renderedHeight;
        }

        this.categoryOffset = totalHeightCounter;

        RenderUtils.bottomFade(this.stack, 0.0F, 100.0F, 200.0F, 20.0F, GuiColorUtils.bg2.getRGB());
        RenderUtils.topFade(this.stack, 0.0F, height - 10.0F, 200.0F, 20.0F, GuiColorUtils.bg2.getRGB());
        GlStateManager._disableScissorTest();
    }

    private void renderParentCategory(ParentCategory parent, float yPos) {
        BlackOut.FONT.text(this.stack, parent.name(), 1.6F, 15.0F, yPos, GuiColorUtils.parentCategory, false, true);
    }

    private void updateScroll() {
        this.moduleScroll.update(this.frameTime);
        this.categoryScroll.update(this.frameTime);
    }

    public void onClick(MouseButtonEvent event) {
        handleGlobalClick(event.button, event.pressed);
    }

    private void handleGlobalClick(int button, boolean pressed) {
        if (!this.open || popUpDelta < 0.1F) return;

        if (pressed) {
            if (button == 0 && this.buttons.onClick(button)) {
                SelectedComponent.reset();
                return;
            }

            if (this.openedScreen != null) {
                this.openedScreen.handleMouse(button, pressed);
                if (button == 1) { this.setScreen(null); }
                return;
            }

            if (this.mouseOnScale()) {
                if (button == 0) {
                    this.scaling = true;
                    this.offsetX = this.mx - width;
                    this.offsetY = this.my - height;
                    SelectedComponent.reset();
                }
            } else if (this.mouseOnCategories()) {
                this.categoryComponents.forEach(c -> c.onMouse(button, pressed));
                SelectedComponent.reset();
            } else if (this.mouseOnModules()) {
                SelectedComponent.reset();

                this.moduleComponents.forEach(module -> {
                    if (module.module.category == selectedCategory) {
                        module.onMouse(button, pressed);
                    }
                });
            } else if (this.mouseOnName() && button == 0) {
                this.moving = true;
                this.offsetX = this.mx;
                this.offsetY = this.my;
                SelectedComponent.reset();
            } else {
                SelectedComponent.reset();
            }
        } else {
            this.moving = false;
            this.scaling = false;

            if (this.openedScreen != null) {
                this.openedScreen.handleMouse(button, pressed);
            } else {
                this.moduleComponents.forEach(m -> {
                    if (m.module.category == selectedCategory) m.onMouse(button, pressed);
                });
            }
        }
    }

    public void scrollToModule(Module module) {
        int columns = this.getColumns();
        float[] lengths = new float[columns];
        int current = 0;

        for (ModuleComponent component : this.moduleComponents) {
            if (component.module.category == module.category) {

                boolean isTarget = (component.module == module);
                float targetProgress = isTarget ? 1.0F : 0.0F;

                float fs = GuiSettings.getInstance().fontScale.get().floatValue();
                float multiplier = MathHelper.lerp(targetProgress,
                        GuiSettings.getInstance().moduleScaleClosed.get().floatValue(),
                        GuiSettings.getInstance().moduleScale.get().floatValue());
                float scale = Math.max(fs, fs * multiplier);
                float finalHeaderHeight = Math.max((BlackOut.FONT.getHeight() * scale) + (15.0F * scale),
                        MathHelper.lerp(targetProgress,
                                GuiSettings.getInstance().moduleHeightClosed.get().floatValue(),
                                GuiSettings.getInstance().moduleHeight.get().floatValue()));

                float settingsHeight = isTarget ? ModuleComponent.getLength(component.module.settingGroups) : 0.0F;
                float totalFinalHeight = finalHeaderHeight + settingsHeight;

                if (isTarget) {
                    float moduleTop = lengths[current];
                    float targetScroll = moduleTop - (height / 2.0F) + (totalFinalHeight / 2.0F) + 30.0F;

                    this.moduleScroll.animateTo(targetScroll);
                    return;
                }

                lengths[current] += totalFinalHeight + 20.0F;
                if (++current >= columns) current = 0;
            }
        }
    }

    private boolean anyModuleBindHovered() {
        for (ModuleComponent mc : this.moduleComponents) {
            if (mc.module.category == selectedCategory) {
                if (mc.isMouseOverBind()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void updateModuleLengthManual() {
        int columns = this.getColumns();
        float[] lengths = new float[columns];
        int current = 0;
        for (ModuleComponent component : this.moduleComponents) {
            if (component.module.category == selectedCategory) {
                lengths[current] += component.getHeight() + (component.opened ? ModuleComponent.getLength(component.module.settingGroups) : 0) + 20.0F;
                if (++current >= lengths.length) current = 0;
            }
        }
        float max = 0.0F;
        for (float f : lengths) max = Math.max(max, f);
        this.moduleLength = max;
    }

    public void onKey(KeyEvent event) {
        if (!event.pressed) {
            if (event.key == GLFW.GLFW_KEY_UP) upPressed = false;
            if (event.key == GLFW.GLFW_KEY_DOWN) downPressed = false;
        }
        handleGlobalKey(event.key, event.pressed);
    }

    private void handleGlobalKey(int key, boolean pressed) {
        if (!pressed) {
            if (key == 265) upPressed = false;
            if (key == 264) downPressed = false;
            return;
        }

        if (this.openedScreen != null) {
            this.openedScreen.handleKey(key, pressed);
            return;
        }

        if (key == GLFW.GLFW_KEY_UP) { upPressed = true; pressTime = System.currentTimeMillis(); return; }
        if (key == GLFW.GLFW_KEY_DOWN) { downPressed = true; pressTime = System.currentTimeMillis(); }

        boolean isWritingText = SelectedComponent.isSelected();

        if (isWritingText || anyModuleBindHovered()) {
            this.moduleComponents.forEach(module -> {
                if (module.module.category == selectedCategory) {
                    module.onKey(key, pressed);
                }
            });

            return;
        }

        String keyName = GLFW.glfwGetKeyName(key, 0);
        boolean isModifier = (key == GLFW.GLFW_KEY_LEFT_SHIFT || key == GLFW.GLFW_KEY_RIGHT_SHIFT || key == GLFW.GLFW_KEY_LEFT_CONTROL || key == GLFW.GLFW_KEY_RIGHT_CONTROL || key == GLFW.GLFW_KEY_LEFT_ALT || key == GLFW.GLFW_KEY_RIGHT_ALT);
        boolean isForbidden = (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_TAB);

        if (keyName != null && !isModifier && !isForbidden) {
            this.setScreen(new SearchScreen(key));
        }
    }

    @Event
    public void onScroll(MouseScrollEvent event) {
        if (BlackOut.mc.currentScreen instanceof TitleScreen && !event.isCancelled()) {
            return;
        }
        if (this.openedScreen == null || !this.openedScreen.handleScroll(event.horizontal, event.vertical)) {
            if (this.mouseOnCategories()) {
                this.categoryScroll.add(event.vertical);
            } else if (this.mouseOnModules()) {
                this.moduleScroll.add(event.vertical);
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private boolean mouseOnCategories() {
        return this.my > 100.0 && this.my < height + 10.0F && this.mx > 0.0 && this.mx < 190.0;
    }

    private boolean mouseOnModules() {
        return this.my > -10.0 && this.my < height + 10.0F && this.mx > 190.0 && this.mx < width;
    }

    private boolean mouseOnName() {
        return this.mx > -10.0 && this.mx < 210.0 && this.my > -10.0 && this.my < 110.0;
    }

    private boolean mouseOnScale() {
        return RenderUtils.insideRounded(this.mx, this.my, width - 10.0F, height - 10.0F, 20.0, 20.0, 15.0);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        handleGlobalClick(button, true);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        handleGlobalClick(button, false);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            if (this.openedScreen != null) {
                this.setScreen(null);
                SelectedComponent.reset();
                return true;
            }

            if (SelectedComponent.isSelected()) {
                SelectedComponent.reset();
                return true;
            }

            this.close();
            return true;
        }

        if (keyCode == 344) {
            if (System.currentTimeMillis() - this.openTime > 100L) {
                this.close();
                return true;
            }
            return true;
        }

        handleGlobalKey(keyCode, true);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        handleGlobalKey(keyCode, false);
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private List<String> wrapText(String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
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
        if (!currentLine.isEmpty()) lines.add(currentLine.toString().trim());
        return lines;
    }

    public boolean isAnimating() {
        return this.open || popUpDelta > 0.01F;
    }
}
