package bodevelopment.client.blackout.randomstuff.mainmenu;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.menu.MainMenu;
import bodevelopment.client.blackout.module.modules.client.MainMenuSettings;
import bodevelopment.client.blackout.module.modules.client.ThemeSettings;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.rendering.renderer.TextureRenderer;
import bodevelopment.client.blackout.rendering.texture.BOTextures;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;

public class ThemeMainMenu implements MainMenuRenderer {
    private final ChangelogRenderer changelogRenderer = new ChangelogRenderer();
    private static final float BUTTON_WIDTH = 360.0F;
    private static final float BUTTON_HEIGHT = 10.0F;
    private static final float BUTTON_RADIUS = 25.0F;

    @Override
    public void render(MatrixStack stack, float height, float mx, float my, String splashText) {
        boolean isGuiOpen = MainMenu.getInstance().isOpenedMenu();
        float renderMx = isGuiOpen ? -1000.0F : mx;
        float renderMy = isGuiOpen ? -1000.0F : my;

        this.renderButtons(stack, renderMx, renderMy);
        this.renderTitle(stack, splashText);
        ThemeSettings theme = ThemeSettings.getInstance();
        MainMenuSettings settings = MainMenuSettings.getInstance();

        this.changelogRenderer.render(stack, renderMx, renderMy,
                true,
                new Color(theme.getMain(180)),
                new Color(theme.getSecond(180)),
                settings.speed.get().floatValue());
        this.renderAllIconButtons(stack, height, renderMx, renderMy);
        this.renderDevs();
    }

    private void renderAllIconButtons(MatrixStack stack, float windowHeight, float mx, float my) {
        stack.push();
        float startX = -1000.0F + 14.0F;
        float startY = windowHeight / 2.0F - 44.0F;
        stack.translate(startX, startY, 0.0F);

        for (int i = 0; i < 4; i++) {
            float currentX = startX + (i * 54.0F);
            // Проверка наведения для иконок
            boolean hovered = RenderUtils.insideRounded(mx, my, currentX + 5.0F, startY + 5.0F, 22.0F, 22.0F, 10.0F);

            this.renderSingleIconButton(stack, i, hovered);
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

        float alpha = hovered ? 1.0F : 0.6F;
        ThemeSettings theme = ThemeSettings.getInstance();
        MainMenuSettings settings = MainMenuSettings.getInstance();

        // Блюр
        RenderUtils.drawLoadedBlur("title", stack, renderer ->
                renderer.rounded(5.0F, 5.0F, 22.0F, 22.0F, 10.0F, 10, 1.0F, 1.0F, 1.0F, 1.0F));

        // Градиентная рамка в стиле темы
        RenderUtils.tenaRounded(stack, 5.0F, 5.0F, 22.0F, 22.0F, 10.0F, 1.5F,
                theme.getMain(hovered ? 255 : 150), theme.getSecond(hovered ? 255 : 150), settings.speed.get().floatValue());

        // Фон
        RenderUtils.rounded(stack, 5.0F, 5.0F, 22.0F, 22.0F, 10.0F, 3.0F,
                new Color(0, 0, 0, hovered ? 70 : 35).getRGB(),
                new Color(0, 0, 0, 225).getRGB());

        // Иконка
        Renderer.setAlpha(alpha);
        t.quad(stack, 0.0F, 0.0F, t.getWidth() / 2.0F, t.getHeight() / 2.0F);
        Renderer.setAlpha(1.0F);
    }

    private void renderButtons(MatrixStack stack, float mx, float my) {
        stack.push();
        stack.translate(-180.0F, -100.0F, 0.0F);
        float currentY = -100.0F;

        for (String name : MainMenu.getInstance().buttonNames) {
            boolean hovered = RenderUtils.insideRounded(mx, my, -180.0, currentY, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_RADIUS);
            this.renderButton(stack, name, hovered);
            stack.translate(0.0F, 85.0F, 0.0F);
            currentY += 85.0F;
        }
        stack.pop();
    }

    private void renderButton(MatrixStack stack, String name, boolean hovered) {
        ThemeSettings theme = ThemeSettings.getInstance();
        MainMenuSettings settings = MainMenuSettings.getInstance();

        RenderUtils.drawLoadedBlur("title", stack, renderer ->
                renderer.rounded(0.0F, 0.0F, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_RADIUS, 10, 1.0F, 1.0F, 1.0F, 1.0F));

        // Градиентная обводка темы
        RenderUtils.tenaRounded(stack, 0.0F, 0.0F, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_RADIUS, 10.0F,
                theme.getMain(hovered ? 255 : 150), theme.getSecond(hovered ? 255 : 150), settings.speed.get().floatValue());

        // Темная подложка
        RenderUtils.rounded(stack, 0.0F, 0.0F, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_RADIUS, 10.0F,
                new Color(0, 0, 0, hovered ? 60 : 35).getRGB(),
                new Color(0, 0, 0, 225).getRGB());

        BlackOut.FONT.text(stack, name, 3.0F, 180.0F, 5.0F, Color.WHITE.getRGB(), true, true);
    }

    private void renderDevs() {
        String devText = "Made by KassuK & OLEPOSSU | Continued by Limon_TH";
        float x = 1000.0F;
        float y = MainMenu.getInstance().getWindowHeight() / 2.0F;
        float scale = 2.0F;

        BlackOut.FONT.text(
                MainMenu.getInstance().getMatrixStack(),
                devText,
                scale,
                x - BlackOut.FONT.getWidth(devText) * scale - 10.0F,
                y - BlackOut.FONT.getHeight() * scale - 10.0F,
                new Color(255, 255, 255, 80).getRGB(),
                false,
                false
        );
    }

    @Override
    public int onClick(float mx, float my) {
        // Кнопки по центру
        float yCenter = -100.0F;
        for (int i = 0; i < 5; i++) {
            if (RenderUtils.insideRounded(mx, my, -180.0, yCenter, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_RADIUS))
                return i;
            yCenter += 85.0F;
        }

        // Иконки снизу
        float windowHeight = MainMenu.getInstance().getWindowHeight();
        float startX = -1000.0F + 14.0F;
        float startY = windowHeight / 2.0F - 44.0F;

        for (int i = 0; i < 4; i++) {
            float currentX = startX + (i * 54.0F);
            if (RenderUtils.insideRounded(mx, my, currentX + 5.0F, startY + 5.0F, 22.0F, 22.0F, 10.0F)) {
                if (i == 3) return 6; // Settings
                return 10 + i; // Socials
            }
        }
        return -1;
    }

    @Override
    public void renderBackground(MatrixStack stack, float width, float height, float mx, float my) {
        MainMenuSettings mainMenuSettings = MainMenuSettings.getInstance();
        ThemeSettings themeSettings = ThemeSettings.getInstance();
        RenderUtils.fadeRounded(stack, 0.0F, 0.0F, width, height, 0.0F, 0.0F,
                themeSettings.getMain(), themeSettings.getSecond(), 0.2F, mainMenuSettings.speed.get().floatValue() / 10.0F);

        RenderUtils.loadBlur("title", mainMenuSettings.blur.get());
        RenderUtils.drawLoadedBlur("title", stack, renderer ->
                renderer.quadShape(0.0F, 0.0F, width, height, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F));
    }

    private void renderTitle(MatrixStack stack, String splashText) {
        BlackOut.BOLD_FONT.text(stack, "BlackOut", 8.5F, 0.0F, -250.0F, Color.WHITE.getRGB(), true, true);
        BlackOut.FONT.text(stack, splashText, 2.5F, 0.0F, -200.0F, Color.WHITE.getRGB(), true, true);
    }
}