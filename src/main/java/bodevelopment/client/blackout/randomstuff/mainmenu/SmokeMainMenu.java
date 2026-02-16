package bodevelopment.client.blackout.randomstuff.mainmenu;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.menu.MainMenu;
import bodevelopment.client.blackout.module.modules.client.MainMenuSettings;
import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.rendering.renderer.ShaderRenderer;
import bodevelopment.client.blackout.rendering.renderer.TextureRenderer;
import bodevelopment.client.blackout.rendering.shader.Shaders;
import bodevelopment.client.blackout.rendering.texture.BOTextures;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;
import java.util.ArrayList;

public class SmokeMainMenu implements MainMenuRenderer {
    private static final long initTime = System.currentTimeMillis();
    private static final float BUTTON_WIDTH = 360.0F;
    private static final float BUTTON_HEIGHT = 10.0F;
    private static final float BUTTON_RADIUS = 25.0F;

    private final ChangelogRenderer changelogRenderer = new ChangelogRenderer();

    @Override
    public void render(MatrixStack stack, float height, float mx, float my, String splashText) {
        boolean isGuiOpen = MainMenu.getInstance().isOpenedMenu();
        float renderMx = isGuiOpen ? -1000.0F : mx;
        float renderMy = isGuiOpen ? -1000.0F : my;

        this.renderButtons(stack, renderMx, renderMy);
        this.renderTitle(stack, splashText);

        this.changelogRenderer.render(stack, renderMx, renderMy, false, null, null, 0);

        this.renderAllIconButtons(stack, height, renderMx, renderMy);
        this.renderDevs();
    }

    private void renderAllIconButtons(MatrixStack stack, float windowHeight, float mx, float my) {
        stack.push();
        // Твои оригинальные координаты из MainMenu
        float startX = -1000.0F + 14.0F;
        float startY = windowHeight / 2.0F - 44.0F;
        stack.translate(startX, startY, 0.0F);

        // Рендерим 3 соцсети (0, 1, 2) и 4-ю кнопку — Настройки (индекс 3)
        for (int i = 0; i < 4; i++) {
            float currentX = startX + (i * 54.0F);
            // Проверка наведения: x и y + смещение 5.0F из-за параметров в rounded
            // Область 22x22, как указано в твоем методе
            boolean hovered = RenderUtils.insideRounded(mx, my, currentX + 5.0F, startY + 5.0F, 22.0F, 22.0F, 10.0F);

            this.renderSingleIconButton(stack, i, hovered);
            stack.translate(54.0F, 0.0F, 0.0F);
        }

        stack.pop();
    }

    private void renderButtons(MatrixStack stack, float mx, float my) {
        stack.push();
        stack.translate(-180.0F, -100.0F, 0.0F);
        float currentY = -100.0F;

        for (String name : MainMenu.getInstance().buttonNames) {
            // МАТЕМАТИЧЕСКИ ТОЧНАЯ ПРОВЕРКА
            // insideRounded учитывает x, y, w, h и радиус закругления.
            // Координата X у нас -180, Y берем из цикла.
            boolean hovered = RenderUtils.insideRounded(mx, my, -180.0, currentY, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_RADIUS);

            this.renderButton(stack, name, hovered);

            stack.translate(0.0F, 85.0F, 0.0F);
            currentY += 85.0F;
        }
        stack.pop();
    }

    private void renderSingleIconButton(MatrixStack stack, int i, boolean hovered) {
        TextureRenderer t = switch (i) {
            case 1 -> BOTextures.getDiscordIconRenderer();
            case 2 -> BOTextures.getYoutubeIconRenderer();
            case 3 -> BOTextures.getSettingsIconRenderer(); // Наша шестеренка
            default -> BOTextures.getGithubIconRenderer();
        };

        float width = t.getWidth() / 2.0F;
        float height = t.getHeight() / 2.0F;
        float alpha = hovered ? 1.0F : 0.6F;

        // Блюр
        RenderUtils.drawLoadedBlur("title", stack, renderer ->
                renderer.rounded(5.0F, 5.0F, 22.0F, 22.0F, 10.0F, 10, 1.0F, 1.0F, 1.0F, 1.0F));

        // Фон (подсвечивается если hovered)
        RenderUtils.rounded(stack, 5.0F, 5.0F, 22.0F, 22.0F, 10.0F, 3.0F,
                new Color(0, 0, 0, hovered ? 70 : 35).getRGB(),
                new Color(0, 0, 0, 225).getRGB());

        // Иконка
        Renderer.setAlpha(alpha);
        t.quad(stack, 0.0F, 0.0F, width, height);
        Renderer.setAlpha(1.0F);
    }

    private void renderButton(MatrixStack stack, String name, boolean hovered) {
        // Рендерим плашку
        RenderUtils.drawLoadedBlur("title", stack, renderer -> renderer.rounded(0.0F, 0.0F, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_RADIUS, 10, 1.0F, 1.0F, 1.0F, 1.0F));
        RenderUtils.rounded(stack, 0.0F, 0.0F, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_RADIUS, 10.0F,
                new Color(0, 0, 0, hovered ? 65 : 35).getRGB(),
                new Color(0, 0, 0, 225).getRGB());

        // Текст (центрируем по Y, чтобы он был ровно внутри плашки 10.0F)
        Color textColor = hovered ? Color.WHITE : new Color(200, 200, 200, 255);
        BlackOut.FONT.text(stack, name, 3.0F, 180.0F, 5.0F, textColor.getRGB(), true, true);
    }

    private void renderDevs() {
        // Обновленная строка авторов
        String devText = "Made by KassuK & OLEPOSSU | Continued by Limon_TH";

        // Координаты из твоего старого кода
        float x = 1000.0F;
        float y = MainMenu.getInstance().getWindowHeight() / 2.0F;

        // Масштаб 2.0F, как и было
        float scale = 2.0F;

        // Рендерим текст
        // Я оставил логику (x - ширина * масштаб - отступ), чтобы текст идеально сидел в углу
        BlackOut.FONT.text(
                MainMenu.getInstance().getMatrixStack(),
                devText,
                scale,
                x - BlackOut.FONT.getWidth(devText) * scale - 5.0F,
                y - BlackOut.FONT.getHeight() * scale - 5.0F,
                new Color(255, 255, 255, 70).getRGB(),
                false,
                false
        );
    }

    @Override
    public int onClick(float mx, float my) {
        // 1. Клик по центральным кнопкам
        float yCenter = -100.0F;
        for (int i = 0; i < 5; i++) {
            if (RenderUtils.insideRounded(mx, my, -180.0, yCenter, 360.0, 10.0, 25.0)) return i;
            yCenter += 85.0F;
        }

        // 2. Клик по ряду иконок снизу
        float windowHeight = MainMenu.getInstance().getWindowHeight();
        float startX = -1000.0F + 14.0F;
        float startY = windowHeight / 2.0F - 44.0F;

        for (int i = 0; i < 4; i++) {
            float currentX = startX + (i * 54.0F);
            // Используем те же координаты, что и в рендере (5.0F смещение)
            if (RenderUtils.insideRounded(mx, my, currentX + 5.0F, startY + 5.0F, 22.0F, 22.0F, 10.0F)) {
                // Если это 4-я кнопка (индекс 3), возвращаем 6 (настройки)
                // Если 0-2, то соцсети
                if (i == 3) return 6;
                return 10 + i;
            }
        }

        return -1;
    }

    // --- ОСТАЛЬНЫЕ МЕТОДЫ (Рендер фона, Чейнджлога, Заголовка) ---
    @Override
    public void renderBackground(MatrixStack stack, float width, float height, float mx, float my) {
        MainMenuSettings mainMenuSettings = MainMenuSettings.getInstance();
        ShaderRenderer shaderRenderer = ShaderRenderer.getInstance();
        shaderRenderer.quad(stack, 0.0F, 0.0F, width, height, 1.0F, 1.0F, 1.0F, 1.0F, Shaders.smoke, new ShaderSetup(setup -> {
            setup.time(initTime);
            setup.color("clr1", mainMenuSettings.color.get().getRGB());
            setup.color("clr2", mainMenuSettings.color2.get().getRGB());
            setup.set("speed", mainMenuSettings.speed.get().floatValue());
        }), VertexFormats.POSITION);
        RenderUtils.loadBlur("title", mainMenuSettings.blur.get());
        RenderUtils.drawLoadedBlur("title", stack, renderer -> renderer.quadShape(0.0F, 0.0F, width, height, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F));
    }

    private void renderTitle(MatrixStack stack, String splashText) {
        BlackOut.BOLD_FONT.text(stack, "BlackOut", 8.5F, 0.0F, -250.0F, Color.WHITE.getRGB(), true, true);
        BlackOut.FONT.text(stack, splashText, 2.5F, 0.0F, -200.0F, Color.WHITE.getRGB(), true, true);
    }
}