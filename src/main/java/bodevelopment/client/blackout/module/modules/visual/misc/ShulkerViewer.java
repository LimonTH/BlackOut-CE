package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.interfaces.mixin.IHandledScreen;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

import java.awt.*;

public class ShulkerViewer extends Module {
    private static ShulkerViewer INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Double> scale = this.sgGeneral.doubleSetting("UI Magnification", 1.0, 0.5, 2.0, 0.1, "Adjusts the dimensions of the preview window relative to the interface.");
    private final Setting<Boolean> showEmpty = this.sgGeneral.booleanSetting("Display Empty Slots", false, "Determines whether unoccupied inventory slots are rendered within the preview.");
    private final Setting<Boolean> shadow = this.sgGeneral.booleanSetting("Ambient Occlusion", true, "Renders a soft drop shadow behind the preview window for depth simulation.");
    private final Setting<Integer> round = this.sgGeneral.intSetting("Corner Radius", 5, 0, 15, 1, "Defines the curvature of the preview window corners.");
    private final Setting<BlackOutColor> bgColor = this.sgGeneral.colorSetting("Surface Color", new BlackOutColor(GuiColorUtils.bg1.getRed(), GuiColorUtils.bg1.getGreen(), GuiColorUtils.bg1.getBlue(), 200), "The base background color and opacity of the content panel.");
    public ShulkerViewer() {
        super("Shulker Viewer", "Projects a high-fidelity itemized preview of Shulker Box contents directly within the inventory interface.", SubCategory.MISC_VISUAL, true);
        INSTANCE = this;
    }

    public void renderOnTop(DrawContext context, int mouseX, int mouseY) {
        if (BlackOut.mc.player == null || BlackOut.mc.currentScreen == null) return;

        ItemStack hoveredStack = getHoveredStack(BlackOut.mc.currentScreen);
        if (hoveredStack == null || hoveredStack.isEmpty()) return;
        if (!(hoveredStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock)) return;

        ContainerComponent container = hoveredStack.get(DataComponentTypes.CONTAINER);
        if (container == null) return;

        DefaultedList<ItemStack> items = DefaultedList.ofSize(27, ItemStack.EMPTY);
        container.copyTo(items);

        renderGui(context, mouseX, mouseY, items, hoveredStack.getName().getString());
    }

    private void renderGui(DrawContext context, int mouseX, int mouseY, DefaultedList<ItemStack> items, String name) {
        context.draw();

        float s = scale.get().floatValue();
        float width = (162 + 10) * s;
        float headerHeight = 15 * s;
        float height = (54 + 10) * s + headerHeight;

        float posX = mouseX + 12;
        float posY = mouseY - height / 2;

        if (posX + width > BlackOut.mc.getWindow().getScaledWidth()) posX = mouseX - width - 12;
        if (posY + height > BlackOut.mc.getWindow().getScaledHeight()) posY = BlackOut.mc.getWindow().getScaledHeight() - height - 5;
        if (posY < 5) posY = 5;

        MatrixStack stack = context.getMatrices();

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();

        stack.push();
        stack.translate(0, 0, 1000.0F);

        if (shadow.get()) {
            RenderUtils.roundedShadow(stack, posX, posY, width, height, round.get().floatValue(), 15.0F, new Color(0, 0, 0, 100).getRGB());
        }
        RenderUtils.rounded(stack, posX, posY, width, height, round.get().floatValue(), 1.0F, bgColor.get().getRGB(), ColorUtils.SHADOW100I);

        float textScale = 1.2F * s;
        BlackOut.FONT.text(stack, name, textScale, posX + 6 * s, posY + 8 * s, Color.WHITE.getRGB(), false, true);

        for (int i = 0; i < items.size(); i++) {
            ItemStack itemStack = items.get(i);
            if (itemStack.isEmpty() && !showEmpty.get()) continue;

            int row = i / 9;
            int col = i % 9;
            float itemX = posX + (5 + col * 18) * s;
            float itemY = posY + headerHeight + (2 + row * 18) * s;

            RenderUtils.renderItem(stack, itemStack, itemX, itemY, 16.0F * s, 1000.0F, true);
        }

        stack.pop();

        RenderSystem.enableDepthTest();
        context.draw();
    }

    private ItemStack getHoveredStack(Screen screen) {
        if (screen instanceof HandledScreen<?> handledScreen && handledScreen instanceof IHandledScreen accessor) {
            if (accessor.blackout_Client$getFocusedSlot() != null) {
                return accessor.blackout_Client$getFocusedSlot().getStack();
            }
        }
        return null;
    }

    public boolean isHoveringShulker() {
        if (BlackOut.mc.currentScreen == null) return false;
        ItemStack stack = getHoveredStack(BlackOut.mc.currentScreen);
        return stack != null && !stack.isEmpty() &&
                stack.getItem() instanceof BlockItem bi &&
                bi.getBlock() instanceof ShulkerBoxBlock;
    }

    public static ShulkerViewer getInstance() {
        return INSTANCE;
    }
}