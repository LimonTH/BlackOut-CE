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

        animation = (float) Math.min(1, animation + (targetAnim - animation) * 0.2);
        textOffset = (float) Math.min(10, textOffset + (targetOffset - textOffset) * 0.2);

        float height = 35.0F;
        float halfHeight = height / 2.0F;

        if (animation > 0.01f) {
            int alpha = (int) (animation * GuiSettings.getInstance().selectorColor.get().alpha);
            int selCol = ColorUtils.withAlpha(GuiSettings.getInstance().selectorColor.get().getColor().getRGB(), alpha);

            RenderUtils.rounded(this.stack, this.x + 5, this.y - halfHeight + 2, 170.0F, height - 4, 6.0F, 2.0F, selCol, ColorUtils.SHADOW100I);

            if (GuiSettings.getInstance().selectorBar.get()) {
                float currentBarHeight = (height - 12.0F) * animation;
                int barColor = GuiRenderUtils.getGuiColors(1.0F).getRGB();

                float fogRadius = (float) GuiSettings.getInstance().selectorGlow.get() * 2.5F;
                int fogColor = ColorUtils.withAlpha(barColor, (int) (animation * 60));

                RenderUtils.rounded(
                        this.stack,
                        this.x + 5.5F,
                        this.y - currentBarHeight / 2.0F,
                        0.5F,
                        currentBarHeight,
                        1.0F,
                        fogRadius,
                        fogColor,
                        fogColor
                );

                int coreColor = ColorUtils.withAlpha(barColor, (int) (animation * 255));

                RenderUtils.rounded(
                        this.stack,
                        this.x + 5,
                        this.y - currentBarHeight / 2.0F,
                        1.5F,
                        currentBarHeight,
                        1.0F,
                        2.0F,
                        coreColor,
                        coreColor
                );
            }
        }

        int textColor = ColorUtils.lerpColor(animation, GuiColorUtils.category, Color.WHITE).getRGB();
        BlackOut.FONT.text(this.stack, this.category.name(), 2.0F, this.x + 15.0F + textOffset, this.y, textColor, false, true);

        return 35.0F + (animation * 14.0F);
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
        float currentHeight = 35.0F + (this.animation * 10.0F);

        return this.mx > this.x && this.mx < this.x + 180.0F
                && this.my > this.y - (currentHeight / 2.0F)
                && this.my < this.y + (currentHeight / 2.0F);
    }

    public float getAnimation() {
        return this.animation;
    }
}