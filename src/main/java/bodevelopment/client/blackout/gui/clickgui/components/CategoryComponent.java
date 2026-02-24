package bodevelopment.client.blackout.gui.clickgui.components;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.clickgui.ClickGui;
import bodevelopment.client.blackout.gui.clickgui.Component;
import bodevelopment.client.blackout.gui.clickgui.screens.ConsoleScreen;
import bodevelopment.client.blackout.gui.clickgui.screens.FriendsScreen;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.client.GuiSettings;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.GuiRenderUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

public class CategoryComponent extends Component {
    public final SubCategory category;
    private float animation = 0f;
    private float textOffset = 0f;

    public CategoryComponent(MatrixStack stack, SubCategory category) {
        super(stack);
        this.category = category;
    }

    @Override
    public float render() {
        boolean active = isActive();
        boolean hovered = isHovered();

        float targetAnim = active ? 1f : (hovered ? 0.3f : 0f);
        float targetOffset = active ? 5f : (hovered ? 3f : 0f);

        float speed = frameTime * 15f;
        animation = MathHelper.stepTowards(animation, targetAnim, speed);
        textOffset = MathHelper.stepTowards(textOffset, targetOffset, speed);

        float fontScale = GuiSettings.getInstance().fontScale.get().floatValue();
        float categoryScale = fontScale * 2.0F;

        float baseHeight = (BlackOut.FONT.getHeight() * categoryScale) + (15.0F * fontScale);
        float halfHeight = baseHeight / 2.0F;

        if (animation > 0.01f) {
            int alpha = (int) (animation * GuiSettings.getInstance().selectorColor.get().alpha);
            int selCol = ColorUtils.withAlpha(GuiSettings.getInstance().selectorColor.get().getColor().getRGB(), alpha);

            RenderUtils.rounded(this.stack, this.x + 5, this.y - halfHeight + (2 * fontScale), 170.0F, baseHeight - (4 * fontScale), 6.0F, 2.0F, selCol, ColorUtils.SHADOW100I);

            if (GuiSettings.getInstance().selectorBar.get()) {
                float currentBarHeight = (baseHeight - (10.0F * fontScale)) * animation;
                int barColor = GuiRenderUtils.getGuiColors(1.0F).getRGB();

                float fogRadius = (float) GuiSettings.getInstance().selectorGlow.get() * 2.5F;
                int fogColor = ColorUtils.withAlpha(barColor, (int) (animation * 60));
                this.stack.translate(0, 0, 1.0);
                RenderUtils.rounded(this.stack, this.x + 5.5F, this.y - currentBarHeight / 2.0F, 0.5F, currentBarHeight, 1.0F, fogRadius, fogColor, fogColor);

                int coreColor = ColorUtils.withAlpha(barColor, (int) (animation * 255));
                RenderUtils.rounded(this.stack, this.x + 5, this.y - currentBarHeight / 2.0F, 1.5F, currentBarHeight, 1.0F, 2.0F, coreColor, coreColor);
            }
        }

        int textColor = ColorUtils.lerpColor(animation, GuiColorUtils.category, Color.WHITE).getRGB();

        BlackOut.FONT.text(this.stack, this.category.name(), categoryScale, this.x + 15.0F + (textOffset * fontScale), this.y, textColor, false, true);

        return baseHeight;
    }

    private boolean isActive() {
        boolean isLogicSelected = (ClickGui.selectedCategory == this.category && Managers.CLICK_GUI.CLICK_GUI.openedScreen == null);

        boolean isScreenSelected = category.name().equalsIgnoreCase("Friends") && Managers.CLICK_GUI.CLICK_GUI.openedScreen instanceof FriendsScreen;
        if (category.name().equalsIgnoreCase("Console") && Managers.CLICK_GUI.CLICK_GUI.openedScreen instanceof ConsoleScreen)
            isScreenSelected = true;

        return isLogicSelected || isScreenSelected;
    }

    @Override
    public void onMouse(int button, boolean pressed) {
        if (button == 0 && pressed && isHovered()) {
            ClickGui.selectedCategory = this.category;
            Managers.CLICK_GUI.CLICK_GUI.setScreen(null);
        }
    }

    private boolean isHovered() {
        if (Managers.CLICK_GUI.CLICK_GUI.openedScreen != null) return false;

        float fontScale = GuiSettings.getInstance().fontScale.get().floatValue();
        float categoryScale = fontScale * 2.0F;
        float baseHeight = (BlackOut.FONT.getHeight() * categoryScale) + (15.0F * fontScale);

        return this.mx > this.x && this.mx < this.x + 180.0F
                && this.my > this.y - (baseHeight / 2.0F)
                && this.my < this.y + (baseHeight / 2.0F);
    }

    public float getAnimation() {
        return this.animation;
    }
}