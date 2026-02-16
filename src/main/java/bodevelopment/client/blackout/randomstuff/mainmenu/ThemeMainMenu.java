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
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;

public class ThemeMainMenu implements MainMenuRenderer {
    private static final ArrayList<String> changelog = new ArrayList<>();
    private static final float BUTTON_WIDTH = 360.0F;
    private static final float BUTTON_HEIGHT = 10.0F;
    private static final float BUTTON_RADIUS = 25.0F;
    private static float longest = 0.0F;
    private float changelogX = 450.0F;
    private float changelogY = -200.0F;
    private float targetX = 450.0F;
    private float targetY = -200.0F;
    private boolean dragging = false;
    private float dragOffsetX, dragOffsetY;

    public static void initChangelog() {
        changelog.clear();
        changelog.add("Ported to Minecraft 1.21.1");
        changelog.add("Global rendering overhaul");
        changelog.add("Improved world-logic communication");
        changelog.add("Synchronized interface logic");
        changelog.add("Fixed UI in Main Menu and world");
        changelog.add("Fixed ID bugs in HUD manager");

        float maxW = 0;
        for (String s : changelog) {
            float w = BlackOut.FONT.getWidth(s) * 1.5F;
            if (w > maxW) maxW = w;
        }
        longest = maxW + 60.0F;
    }

    @Override
    public void render(MatrixStack stack, float height, float mx, float my, String splashText) {
        boolean isGuiOpen = MainMenu.getInstance().isOpenedMenu();
        float renderMx = isGuiOpen ? -1000.0F : mx;
        float renderMy = isGuiOpen ? -1000.0F : my;

        this.renderButtons(stack, renderMx, renderMy);
        this.renderTitle(stack, splashText);
        this.renderChangelog(stack, renderMx, renderMy);
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

    private void renderChangelog(MatrixStack stack, float mx, float my) {
        if (changelog.isEmpty()) {
            initChangelog();
        }
        float fontHeight = BlackOut.FONT.getHeight() * 1.5F;
        int maxLines = 15;
        float width = Math.max(longest, 350.0F);
        float height = BlackOut.BOLD_FONT.getHeight() + Math.min(changelog.size(), maxLines) * fontHeight + 50.0F;

        // Драг-логика
        boolean mousePressed = GLFW.glfwGetMouseButton(BlackOut.mc.getWindow().getHandle(), 0) == 1;
        if (this.dragging) {
            if (mousePressed) {
                this.targetX = mx - this.dragOffsetX;
                this.targetY = my - this.dragOffsetY;
            } else {
                this.dragging = false;
            }
        } else if (mousePressed) {
            if (mx >= this.changelogX && mx <= this.changelogX + width && my >= this.changelogY && my <= this.changelogY + 40.0F) {
                this.dragging = true;
                this.dragOffsetX = mx - this.changelogX;
                this.dragOffsetY = my - this.changelogY;
            }
        }
        this.changelogX = MathHelper.lerp(0.2F, this.changelogX, this.targetX);
        this.changelogY = MathHelper.lerp(0.2F, this.changelogY, this.targetY);

        stack.push();
        stack.translate(this.changelogX, this.changelogY, 0.0F);

        ThemeSettings theme = ThemeSettings.getInstance();
        MainMenuSettings settings = MainMenuSettings.getInstance();

        RenderUtils.roundedShadow(stack, 0.0F, 0.0F, width, height, 15.0F, 15.0F, new Color(0, 0, 0, 120).getRGB());
        RenderUtils.drawLoadedBlur("title", stack, renderer -> renderer.rounded(0.0F, 0.0F, width, height, 15.0F, 10, 1.0F, 1.0F, 1.0F, 1.0F));

        // Фон и градиентная рамка чейнджлога
        RenderUtils.rounded(stack, 0.0F, 0.0F, width, height, 15.0F, 2.0F, new Color(20, 20, 20, 160).getRGB(), new Color(0, 0, 0, 200).getRGB());
        RenderUtils.tenaRounded(stack, 0.0F, 0.0F, width, height, 15.0F, 1.5F, theme.getMain(180), theme.getSecond(180), settings.speed.get().floatValue());

        BlackOut.BOLD_FONT.text(stack, "Update Notes", 2.2F, width / 2.0F, 12.0F, Color.WHITE.getRGB(), true, false);
        RenderUtils.rounded(stack, 15.0F, 38.0F, width - 30.0F, 1.5F, 1.0F, 0.0F, new Color(255, 255, 255, 50).getRGB(), 0);

        for (int j = 0; j < Math.min(changelog.size(), maxLines); j++) {
            BlackOut.FONT.text(stack, "• " + changelog.get(j), 1.5F, 18.0F, 50.0F + (j * fontHeight), new Color(225, 225, 225).getRGB(), false, false);
        }
        stack.pop();
    }
}