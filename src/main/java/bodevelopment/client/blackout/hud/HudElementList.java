package bodevelopment.client.blackout.hud;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.ConfigType;
import bodevelopment.client.blackout.helpers.ScrollHelper;
import bodevelopment.client.blackout.helpers.SmoothScrollHelper;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.randomstuff.Pair;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.ClassUtils;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.render.AnimUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class HudElementList {
    private static final int minWidth = 150;
    private static final int minHeight = 40;
    private static final int elementOffset = 60;
    private static final int elementHeight = 35;
    private static final int elementRadius = 5;
    private final List<HudListEntry> entries = new ArrayList<>();
    private float listLength = 0.0F;
    private MatrixStack stack;
    private float mx;
    private float my;
    private float frameTime;
    private float width;
    private float height;
    private boolean open = false;
    private float openProgress = 0.0F;
    private final ScrollHelper scroll = new SmoothScrollHelper(0.5F, 20.0F, () -> 0.0F, () -> this.listLength - this.getHeight() + 40.0F + 10.0F).limit(5.0F);
    private boolean closed = false;
    private float closeProgress = 0.0F;

    public void init() {
        this.entries.clear();
        List<Pair<String, Class<? extends HudElement>>> managerElements = Managers.HUD.getElements();

        managerElements.forEach(pair -> {
            this.entries.add(new HudListEntry(pair.getRight(), pair.getLeft()));
        });

        this.listLength = this.entries.size() * 60;
        this.scroll.set(0);
    }

    public void render(MatrixStack stack, float frameTime, float mouseX, float mouseY) {
        this.frameTime = frameTime;
        this.mx = mouseX * RenderUtils.getScale();
        this.my = mouseY * RenderUtils.getScale();
        this.stack = stack;
        this.scroll.update(frameTime);
        this.updateProgress();
        this.updateScale();
        this.startAlpha();
        this.renderList();
        this.endAlpha();
    }

    public boolean onMouse(int button, boolean pressed) {
        if (button != 0) return false;
        if (!pressed) {
            this.closed = false;
            return false;
        }

        float listX = (BlackOut.mc.getWindow().getWidth() - this.width) / 2.0F;
        float listY = BlackOut.mc.getWindow().getHeight() - this.height;

        if (insideBounds(listX, listY, this.width, 40.0F)) { // Клик по шапке
            this.open = !this.open;
            return true;
        } else if (insideBounds(listX, listY + 40.0F, this.width, this.height - 40.0F)) { // Клик по списку
            this.onClickList(this.my - (listY + 40.0F));
            return true;
        }
        return false;
    }

    public boolean onScroll(double vertical) {
        float listX = (BlackOut.mc.getWindow().getWidth() - this.width) / 2.0F;
        float listY = BlackOut.mc.getWindow().getHeight() - this.height;

        if (!this.insideBounds(listX, listY, this.width, this.height)) {
            return false;
        } else {
            this.scroll.add(vertical);
            return true;
        }
    }

    private void onClickList(float offset) {
        offset += this.scroll.get() - 20.0F;

        for (HudListEntry entry : this.entries) {
            if (offset >= 5.0F && offset <= 40.0F) {
                this.clickElement(ClassUtils.instance(entry.hudElement));
                return;
            }

            offset -= 60.0F;
        }
    }

    private void clickElement(HudElement element) {
        Managers.HUD.add(element);
        Managers.HUD.HUD_EDITOR.onListClick(element);
        Managers.CONFIG.save(ConfigType.HUD);
        Managers.CONFIG.save(ConfigType.Binds);
        this.closed = true;
    }

    private void startAlpha() {
        Renderer.setAlpha(1.0F - this.closeProgress);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F - this.closeProgress);
    }

    private void endAlpha() {
        Renderer.setAlpha(1.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void updateScale() {
        this.width = this.getWidth();
        this.height = this.getHeight();
    }

    private void renderList() {
        this.stack.push();
        float width = this.getWidth();
        float height = this.getHeight();
        this.stack.translate((BlackOut.mc.getWindow().getWidth() - width) / 2.0F, BlackOut.mc.getWindow().getHeight() - height, 0.0F);
        RenderUtils.rounded(this.stack, 0.0F, 0.0F, width, height, 10.0F, 30.0F, GuiColorUtils.bg1.getRGB(), ColorUtils.SHADOW100I);
        RenderUtils.roundedTop(this.stack, 0.0F, 0.0F, width, 40.0F, 10.0F, 0.0F, GuiColorUtils.bg2.getRGB(), ColorUtils.SHADOW100I);
        this.renderListContent();
        RenderUtils.bottomFade(this.stack, -10.0F, 40.0F, width + 20.0F, 10.0F, ColorUtils.SHADOW100I);
        this.stack.pop();
    }

    private float clampLerpProgress(float val, float start, float end) {
        return MathHelper.clamp(MathHelper.getLerpProgress(val, start, end), 0.0F, 1.0F);
    }

    private void updateProgress() {
        if (this.open) {
            this.openProgress = Math.min(this.openProgress + this.frameTime, 1.0F);
        } else {
            this.openProgress = Math.max(this.openProgress - this.frameTime, 0.0F);
        }

        if (this.closed) {
            this.closeProgress = Math.min(this.closeProgress + this.frameTime * 20.0F, 1.0F);
        } else {
            this.closeProgress = Math.max(this.closeProgress - this.frameTime * 2.0F, 0.0F);
        }
    }

    private void renderListContent() {
        float listX = (BlackOut.mc.getWindow().getWidth() - this.width) / 2.0F;
        float listY = BlackOut.mc.getWindow().getHeight() - this.height;

        float y = 60.0F - this.scroll.get();
        this.scissor();
        this.stack.push();

        for (HudListEntry entry : this.entries) {
            boolean isHovered = this.insideBounds(listX + 7.5F, y + listY, this.width - 15.0F, 35.0F);
            entry.updateProgress(this.frameTime * 10.0F, isHovered);

            RenderUtils.rounded(this.stack, 7.5F, y, this.width - 15.0F, 35.0F, 5.0F, 8.0F, GuiColorUtils.bg2.getRGB(), ColorUtils.SHADOW100I);

            BlackOut.FONT.text(this.stack, entry.displayName, 2.0F, this.width / 2.0F, y + 17.5F, this.getTextColor(entry), true, true);
            y += 60.0F;
        }

        this.stack.pop();
        this.endScissor();
    }

    private void scissor() {
        var window = BlackOut.mc.getWindow();
        double sw = window.getWidth();
        double sh = window.getHeight();

        double topContentY = (sh - this.height + 40.0F);
        double cutLineTop = sh - topContentY;
        double contentHeightPx = (this.height - 40.0F);
        double cutLineBottom = cutLineTop - contentHeightPx;

        GlStateManager._enableScissorTest();

        int glX = (int) ((sw - this.width) / 2.0);
        int glY = (int) Math.max(0, cutLineBottom);
        int glW = (int) this.width;
        int glH = (int) Math.max(0, cutLineTop - cutLineBottom);

        GlStateManager._scissorBox(glX, glY, glW, glH);
    }

    private void endScissor() {
        RenderUtils.endScissor();
    }

    private Color getTextColor(HudListEntry entry) {
        return ColorUtils.lerpColor(entry.progress, Color.GRAY, Color.WHITE);
    }

    private boolean insideBounds(float x, float y, float w, float h) {
        float offsetX = this.mx - x;
        float offsetY = this.my - y;
        return offsetX >= 0.0F && offsetY >= 0.0F && offsetX <= w && offsetY <= h;
    }

    private float getWidth() {
        return 150.0F + (float) AnimUtils.easeInOutCubic(this.clampLerpProgress(this.openProgress, 0.0F, 0.5F)) * 200.0F;
    }

    private float getHeight() {
        return MathHelper.lerp(
                (float) AnimUtils.easeInOutCubic(this.clampLerpProgress(this.openProgress, 0.5F, 1.0F)), 40.0F, BlackOut.mc.getWindow().getHeight() * 0.5F
        );
    }

    private static class HudListEntry {
        private final Class<? extends HudElement> hudElement;
        private final String displayName;
        private float progress = 0.0F;

        private HudListEntry(Class<? extends HudElement> hudElement, String displayName) {
            this.hudElement = hudElement;
            this.displayName = displayName;
        }

        private void updateProgress(float frameTime, boolean close) {
            if (close) {
                this.progress = Math.min(this.progress + frameTime, 1.0F);
            } else {
                this.progress = Math.max(this.progress - frameTime, 0.0F);
            }
        }
    }
}
