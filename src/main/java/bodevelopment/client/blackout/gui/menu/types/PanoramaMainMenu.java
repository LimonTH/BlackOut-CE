package bodevelopment.client.blackout.gui.menu.types;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.menu.MainMenu;
import bodevelopment.client.blackout.module.modules.client.MainMenuSettings;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.mainmenu.ChangelogRenderer;
import bodevelopment.client.blackout.randomstuff.mainmenu.MainMenuRenderer;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.rendering.renderer.TextureRenderer;
import bodevelopment.client.blackout.rendering.texture.BOTextures;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.awt.*;

public class PanoramaMainMenu implements MainMenuRenderer {
    private static final float BUTTON_WIDTH = 360.0F;
    private static final float BUTTON_HEIGHT = 10.0F;
    private static final float BUTTON_RADIUS = 25.0F;
    private final CubeMapRenderer PANORAMA_CUBE_MAP = new CubeMapRenderer(Identifier.of("textures/gui/title/background/panorama"));
    private final RotatingCubeMapRenderer backgroundRenderer = new RotatingCubeMapRenderer(this.PANORAMA_CUBE_MAP);
    private final ChangelogRenderer changelogRenderer = new ChangelogRenderer();

    @Override
    public void render(MatrixStack stack, float height, float mx, float my, String text1, String text2, float progress) {
        boolean isGuiOpen = MainMenu.getInstance().isOpenedMenu();
        boolean isExiting = MainMenu.getInstance().isExiting();

        float renderMx = (isGuiOpen || isExiting) ? -5000.0F : mx;
        float renderMy = (isGuiOpen || isExiting) ? -5000.0F : my;

        this.renderButtons(stack, renderMx, renderMy);
        BlackOut.BOLD_FONT.text(stack, BlackOut.NAME, 8.5F, 0.0F, -250.0F, Color.WHITE.getRGB(), true, true);
        this.renderAnimatedSplash(stack, text1, text2, progress);
        this.changelogRenderer.render(stack, renderMx, renderMy, false, null, null, 0);

        this.renderAllIconButtons(stack, height, renderMx, renderMy);
        this.renderDevs();
    }

    private void renderAnimatedSplash(MatrixStack stack, String text1, String text2, float progress) {
        float yPos = -200.0F;
        float scale = 2.5F;
        float offsetRange = 15.0F;

        if (progress >= 1.0F || text2.isEmpty()) {
            BlackOut.FONT.text(stack, text1, scale, 0.0F, yPos, Color.WHITE.getRGB(), true, true);
        } else {
            int alpha1 = (int) ((1.0F - progress) * 255);
            if (alpha1 > 5) {
                stack.push();
                stack.translate(0, -progress * offsetRange, 0);
                BlackOut.FONT.text(stack, text1, scale, 0.0F, yPos, new Color(255, 255, 255, alpha1).getRGB(), true, true);
                stack.pop();
            }

            int alpha2 = (int) (progress * 255);
            if (alpha2 > 5) {
                stack.push();
                stack.translate(0, offsetRange - (progress * offsetRange), 0);
                BlackOut.FONT.text(stack, text2, scale, 0.0F, yPos, new Color(255, 255, 255, alpha2).getRGB(), true, true);
                stack.pop();
            }
        }
    }

    private void renderButtons(MatrixStack stack, float mx, float my) {
        stack.push();
        stack.translate(-180.0F, -100.0F, 0.0F);
        float currentY = -100.0F;

        for (String name : MainMenu.getInstance().buttonNames) {
            boolean hovered = RenderUtils.insideRounded(mx, my, -180.0, currentY, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_RADIUS);

            stack.push();
            if (hovered) {
                stack.scale(1.03F, 1.03F, 1.0F);
                stack.translate(-5.4F, -0.3F, 0.0F);
            }

            this.renderButton(stack, name, hovered);
            stack.pop();

            stack.translate(0.0F, 85.0F, 0.0F);
            currentY += 85.0F;
        }
        stack.pop();
    }

    private void renderButton(MatrixStack stack, String name, boolean hovered) {
        RenderUtils.drawLoadedBlur("title", stack, renderer ->
                renderer.rounded(0.0F, 0.0F, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_RADIUS, 10, 1.0F, 1.0F, 1.0F, 1.0F));

        RenderUtils.rounded(stack, 0.0F, 0.0F, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_RADIUS, 10.0F,
                new Color(0, 0, 0, hovered ? 100 : 60).getRGB(),
                new Color(0, 0, 0, 240).getRGB());

        Color textColor = hovered ? Color.WHITE : new Color(190, 190, 190, 255);
        BlackOut.FONT.text(stack, name, 3.0F, 180.0F, 5.0F, textColor.getRGB(), true, true);
    }

    private void renderAllIconButtons(MatrixStack stack, float windowHeight, float mx, float my) {
        stack.push();
        float startX = -1000.0F + 14.0F;
        float startY = windowHeight / 2.0F - 44.0F;
        stack.translate(startX, startY, 0.0F);

        for (int i = 0; i < 4; i++) {
            float currentX = startX + (i * 54.0F);
            boolean hovered = RenderUtils.insideRounded(mx, my, currentX + 5.0F, startY + 5.0F, 22.0F, 22.0F, 10.0F);

            stack.push();
            if (hovered) {
                stack.scale(1.1F, 1.1F, 1.0F);
                stack.translate(-1.0F, -1.0F, 0.0F);
            }
            this.renderSingleIconButton(stack, i, hovered);
            stack.pop();

            stack.translate(54.0F, 0.0F, 0.0F);
        }
        stack.pop();
    }

    private void renderSingleIconButton(MatrixStack stack, int i, boolean hovered) {
        TextureRenderer t = switch (i) {
            case 1 -> BOTextures.getDiscordIconRenderer();
            case 2 -> BOTextures.getYoutubeIconRenderer();
            case 3 -> BOTextures.getSettingsIconRenderer();
            default -> BOTextures.getGithubIconRenderer();
        };

        RenderUtils.drawLoadedBlur("title", stack, renderer -> renderer.rounded(5.0F, 5.0F, 22.0F, 22.0F, 10.0F, 10, 1.0F, 1.0F, 1.0F, 1.0F));

        RenderUtils.rounded(stack, 5.0F, 5.0F, 22.0F, 22.0F, 10.0F, 3.0F,
                new Color(255, 255, 255, hovered ? 40 : 10).getRGB(),
                new Color(0, 0, 0, 210).getRGB());

        Renderer.setAlpha(hovered ? 1.0F : 0.65F);
        t.quad(stack, 0.0F, 0.0F, t.getWidth() / 2.0F, t.getHeight() / 2.0F);
        Renderer.setAlpha(1.0F);
    }

    private void renderDevs() {
        String devText = "Made by KassuK & OLEPOSSU | Continued by Limon_TH";
        float x = 1000.0F;
        float y = MainMenu.getInstance().getWindowHeight() / 2.0F;
        float scale = 2.0F;

        BlackOut.FONT.text(MainMenu.getInstance().getMatrixStack(), devText, scale,
                x - BlackOut.FONT.getWidth(devText) * scale - 10.0F,
                y - BlackOut.FONT.getHeight() * scale - 10.0F,
                new Color(255, 255, 255, 60).getRGB(), false, false);
    }

    @Override
    public int onClick(float mx, float my) {
        float y = -100.0F;
        for (int i = 0; i < 5; i++) {
            if (RenderUtils.insideRounded(mx, my, -180.0, y, 360.0, 10.0, 25.0)) return i;
            y += 85.0F;
        }

        float windowHeight = MainMenu.getInstance().getWindowHeight();
        float startX = -1000.0F + 14.0F;
        float startY = windowHeight / 2.0F - 44.0F;

        for (int i = 0; i < 4; i++) {
            float currentX = startX + (i * 54.0F);
            if (RenderUtils.insideRounded(mx, my, currentX + 5.0F, startY + 5.0F, 22.0F, 22.0F, 10.0F)) {
                if (i == 3) return 6;
                return 10 + i;
            }
        }
        return -1;
    }

    @Override
    public void renderBackground(MatrixStack stack, float width, float height, float mx, float my) {
        MainMenuSettings mainMenuSettings = MainMenuSettings.getInstance();
        BlackOutColor color = mainMenuSettings.shitfuckingmenucolor.get();
        boolean exiting = MainMenu.getInstance().isExiting();

        DrawContext context = new DrawContext(BlackOut.mc, BlackOut.mc.getBufferBuilders().getEntityVertexConsumers());
        RenderSystem.setShaderColor(color.red / 255.0F, color.green / 255.0F, color.blue / 255.0F, color.alpha / 255.0F);

        this.backgroundRenderer.render(
                context,
                (int) width,
                (int) height,
                1.0F,
                BlackOut.mc.getRenderTickCounter().getTickDelta(true)
        );

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int blurRadius = (int) (double) mainMenuSettings.blur.get();
        if (blurRadius > 0) {
            if (!exiting) {
                RenderUtils.loadBlur("title", blurRadius);
            }
            RenderUtils.drawLoadedBlur("title", stack, renderer -> renderer.quadShape(0.0F, 0.0F, width, height, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F));
        }
    }
}