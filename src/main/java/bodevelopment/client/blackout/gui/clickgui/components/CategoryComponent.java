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

    private final float EXPANSION_AMOUNT = 8.0f;

    public CategoryComponent(MatrixStack stack, SubCategory category) {
        super(stack);
        this.category = category;
    }

    @Override
    public float render() {
        boolean active = isActive();
        boolean hovered = isHovered();

        float targetAnim = active ? 1f : (hovered ? 0.3f : 0f);
        float targetOffset = active ? 6f : (hovered ? 3f : 0f);

        float speed = frameTime * 15f;
        animation = MathHelper.stepTowards(animation, targetAnim, speed);
        textOffset = MathHelper.stepTowards(textOffset, targetOffset, speed);

        float fontScale = GuiSettings.getInstance().fontScale.get().floatValue();
        float categoryScale = fontScale * 2.0F;

        float baseInnerHeight = (BlackOut.FONT.getHeight() * categoryScale) + (4.0F * fontScale);
        float shaderPadding = (6.0F + 2.0F) * fontScale;
        float currentExpansion = animation * EXPANSION_AMOUNT * fontScale;
        float rectH = baseInnerHeight + currentExpansion;
        float totalSlotHeight = rectH + (shaderPadding * 2);

        if (animation > 0.01f) {
            int alpha = (int) (animation * GuiSettings.getInstance().selectorColor.get().alpha);
            int selCol = ColorUtils.withAlpha(GuiSettings.getInstance().selectorColor.get().getColor().getRGB(), alpha);

            float rectX = this.x + 5;
            float rectY = this.y + shaderPadding;
            float rectW = 170.0F;

            RenderUtils.rounded(this.stack, rectX, rectY, rectW, rectH, 6.0F, 2.0F, selCol, ColorUtils.SHADOW100I);

            if (GuiSettings.getInstance().selectorBar.get()) {
                float barHeight = rectH - (6.0F * fontScale);
                int barColor = GuiRenderUtils.getGuiColors(1.0F).getRGB();
                float fogRadius = (float) GuiSettings.getInstance().selectorGlow.get() * 2.5F;
                int fogColor = ColorUtils.withAlpha(barColor, (int) (animation * 60));

                this.stack.translate(0, 0, 1.0);
                float barY = rectY + (rectH - barHeight) / 2.0F;
                RenderUtils.rounded(this.stack, this.x + 5.5F, barY, 0.5F, barHeight, 1.0F, fogRadius, fogColor, fogColor);
                RenderUtils.rounded(this.stack, this.x + 5, barY, 1.5F, barHeight, 1.0F, 2.0F, ColorUtils.withAlpha(barColor, (int)(animation * 255)), ColorUtils.withAlpha(barColor, (int)(animation * 255)));
                this.stack.translate(0, 0, -1.0);
            }
        }

        int textColor = ColorUtils.lerpColor(animation, GuiColorUtils.category, Color.WHITE).getRGB();

        BlackOut.FONT.text(this.stack, this.category.name(), categoryScale, this.x + 15.0F + (textOffset * fontScale), this.y + (totalSlotHeight / 2.0f), textColor, false, true);

        return totalSlotHeight - (2.0F * fontScale);
    }

    public boolean isHovered() {
        if (Managers.CLICK_GUI.CLICK_GUI.openedScreen != null) return false;

        float fontScale = GuiSettings.getInstance().fontScale.get().floatValue();
        float categoryScale = fontScale * 2.0F;
        float shaderPadding = (6.0F + 2.0F) * fontScale;
        float baseInnerHeight = (BlackOut.FONT.getHeight() * categoryScale) + (4.0F * fontScale);

        float currentRectH = baseInnerHeight + (animation * EXPANSION_AMOUNT * fontScale);
        float currentTotalHeight = currentRectH + (shaderPadding * 2) - (2.0F * fontScale);

        return this.mx > this.x && this.mx < this.x + 180.0F
                && this.my > this.y && this.my < this.y + currentTotalHeight;
    }

    private boolean isActive() {
        boolean isLogicSelected = (ClickGui.selectedCategory == this.category && Managers.CLICK_GUI.CLICK_GUI.openedScreen == null);
        boolean isScreenSelected = (category.name().equalsIgnoreCase("Friends") && Managers.CLICK_GUI.CLICK_GUI.openedScreen instanceof FriendsScreen) ||
                (category.name().equalsIgnoreCase("Console") && Managers.CLICK_GUI.CLICK_GUI.openedScreen instanceof ConsoleScreen);

        return isLogicSelected || isScreenSelected;
    }

    @Override
    public void onMouse(int button, boolean pressed) {
        if (button == 0 && pressed && isHovered()) {
            ClickGui.selectedCategory = this.category;
            Managers.CLICK_GUI.CLICK_GUI.setScreen(null);
        }
    }

    public float getAnimation() {
        return this.animation;
    }
}