package bodevelopment.client.blackout.gui.clickgui;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.clickgui.screens.AddonScreen;
import bodevelopment.client.blackout.gui.clickgui.screens.ConfigScreen;
import bodevelopment.client.blackout.gui.clickgui.screens.ConsoleScreen;
import bodevelopment.client.blackout.gui.clickgui.screens.FriendsScreen;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.client.GuiSettings;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.rendering.renderer.TextureRenderer;
import bodevelopment.client.blackout.rendering.texture.BOTextures;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.render.AnimUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClickGuiButtons {
    public static final List<Button> buttons = new ArrayList<>();
    private static final int BUTTON_WIDTH = 70;
    private static final int BUTTON_SEPARATION = 20;

    static {
        addSetting(ConfigScreen::new, BOTextures.getFolderIconRenderer());
        addSetting(FriendsScreen::new, BOTextures.getPersonIconRenderer());
        addSetting(ConsoleScreen::new, BOTextures.getConsoleIconRenderer());
        addSetting(AddonScreen::new, BOTextures.getPlusIconRenderer());
    }

    private static void addSetting(Supplier<? extends ClickGuiScreen> screen, TextureRenderer icon) {
        buttons.add(new Button(screen, icon));
    }

    public void render(int mouseX, int mouseY, long openTime, float closeDelta) {
        MatrixStack stack = RenderUtils.emptyStack;
        stack.push();
        RenderUtils.unGuiScale(stack);

        double screenWidth = BlackOut.mc.getWindow().getWidth();
        double screenHeight = BlackOut.mc.getWindow().getHeight();

        stack.translate((screenWidth - this.getWidth()) / 2.0,
                screenHeight - 105.0 - BUTTON_SEPARATION, 0.0);

        float delta = (float) (System.currentTimeMillis() - openTime - 200L) / 500.0F;

        for (Button button : buttons) {
            this.renderButton(stack, button.icon(), MathHelper.clamp(delta -= 0.15F, 0.0F, 1.0F), closeDelta);
        }

        stack.pop();
    }

    public boolean onClick(int button) {
        if (button != 0) return false;

        double[] xArr = new double[1];
        double[] yArr = new double[1];
        GLFW.glfwGetCursorPos(BlackOut.mc.getWindow().getHandle(), xArr, yArr);

        double rawX = xArr[0];
        double rawY = yArr[0];

        double screenWidth = BlackOut.mc.getWindow().getWidth();
        double screenHeight = BlackOut.mc.getWindow().getHeight();

        double startX = (screenWidth - this.getWidth()) / 2.0;
        double startY = screenHeight - 105.0 - BUTTON_SEPARATION;

        for (int i = 0; i < buttons.size(); i++) {
            double centerX = startX + 35.0 + (i * 90.0);
            double centerY = startY + 35.0;

            double dx = rawX - centerX;
            double dy = rawY - centerY;

            if (Math.sqrt(dx * dx + dy * dy) <= 35.0) {
                Managers.CLICK_GUI.CLICK_GUI.setScreen(buttons.get(i).supplier.get());
                return true;
            }
        }

        return false;
    }

    private double getWidth() {
        return buttons.size() * BUTTON_WIDTH + (buttons.size() - 1) * BUTTON_SEPARATION;
    }

    private void renderButton(MatrixStack stack, TextureRenderer icon, float delta, float closeDelta) {
        float prevAlpha = Renderer.getAlpha();
        Renderer.setAlpha((float) Math.sqrt(delta) * closeDelta);

        float anim = (float) AnimUtils.easeOutBack(delta);
        float offset = anim * -50.0F + 50.0F;
        float half = 35.0F;

        RenderUtils.rounded(stack, half, half + offset, 0.0F, 0.0F, half, 15.0F, GuiColorUtils.bg2.getRGB(), ColorUtils.SHADOW100I);

        float ratio = icon.getWidth() / 36.0F;
        float width = icon.getWidth() / ratio;
        float height = icon.getHeight() / ratio;

        icon.quad(stack, half - width / 2.0F, half - width / 2.0F + offset, width, height, Color.WHITE.getRGB());

        stack.translate(90.0F, 0.0F, 0.0F);
        Renderer.setAlpha(prevAlpha);
    }

    public record Button(Supplier<? extends ClickGuiScreen> supplier, TextureRenderer icon) {
    }
}