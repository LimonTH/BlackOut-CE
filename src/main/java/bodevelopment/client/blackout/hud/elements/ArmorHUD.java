package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BackgroundMultiSetting;
import bodevelopment.client.blackout.module.setting.multisettings.RoundedColorMultiSetting;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;

import java.awt.*;

public class ArmorHUD extends HudElement {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgColor = this.addGroup("Color");

    // TODO: armorBG нигде не используется
    private final Setting<Boolean> reversed = this.sgGeneral.booleanSetting("Invert Order", false, "Flips the rendering sequence of armor pieces from Helmet-to-Boots to Boots-to-Helmet.");
    private final Setting<Boolean> bg = this.sgGeneral.booleanSetting("Backdrop", true, "Renders a background panel behind the armor icons.");
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgGeneral, this.bg::get, null);
    private final Setting<Boolean> armorBG = this.sgGeneral.booleanSetting("Slot Overlay", true, "Renders a distinct background for each individual armor slot.");
    private final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Gaussian Blur", true, "Applies a blur effect behind the element for visual depth.");
    private final Setting<Boolean> shadow = this.sgGeneral.booleanSetting("Drop Shadow", true, "Adds a subtle shadow effect to the background panel.");
    private final Setting<Boolean> bar = this.sgGeneral.booleanSetting("Durability Bar", false, "Visualizes remaining durability as a horizontal progress bar.");
    private final Setting<Boolean> text = this.sgGeneral.booleanSetting("Percentage Text", true, "Displays the remaining durability as a numerical percentage.");
    private final Setting<Boolean> centerText = this.sgGeneral.booleanSetting("Align Center", true, "Centers the durability text relative to the armor icon.");
    private final RoundedColorMultiSetting armorBar = RoundedColorMultiSetting.of(this.sgGeneral, "Bar Color");

    private final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgColor, "Text Color");

    public ArmorHUD() {
        super("Armor HUD", "Displays a live overview of your equipped armor pieces, including item icons and precise durability monitoring.");
        this.setSize(80.0F, 19.0F);
    }

    public void render() {
        if (BlackOut.mc.player == null || BlackOut.mc.world == null) return;
        if (!armorFound()) return;

        float bgHeight = this.bar.get() ? 24.0F : 18.0F;
        this.setSize(88.0F, bgHeight);

        this.stack.push();
        this.draw(this.stack);
        this.stack.pop();
    }

    private void draw(MatrixStack stack) {
        float width = 88.0F;
        float height = this.getHeight();

        if (this.blur.get()) {
            RenderUtils.drawLoadedBlur("hudblur", stack, r -> r.rounded(0.0F, 0.0F, width, height, 3.0F, 10));
            Renderer.onHUDBlur();
        }

        if (this.bg.get()) {
            this.background.render(stack, 0.0F, 0.0F, width, height, 3.0F, this.shadow.get() ? 3.0F : 0.0F);
        }

        BlackOut.mc.getBufferBuilders().getEntityVertexConsumers().draw();
        RenderSystem.enableDepthTest();

        for (int i = 0; i < 4; i++) {
            ItemStack itemStack = BlackOut.mc.player.getInventory().armor.get(this.reversed.get() ? i : 3 - i);
            float xOffset = 2 + (22 * i);

            if (itemStack.isEmpty()) continue;

            RenderUtils.renderItem(stack, itemStack, xOffset, 1.0F, 16.0F, 500.0F, false);

            if (itemStack.isDamageable()) {
                float durabilityValue = (float) (itemStack.getMaxDamage() - itemStack.getDamage()) / itemStack.getMaxDamage();
                int durabilityPercentage = Math.round(durabilityValue * 100.0f);

                if (this.text.get()) {
                    String textStr = durabilityPercentage + "%";
                    this.textColor.render(stack, textStr, 0.6F, xOffset + (this.centerText.get() ? 8 : 0), 14.0F, this.centerText.get(), true);
                }

                if (this.bar.get()) {
                    Color bgColor = new Color(0, 0, 0, 120);
                    RenderUtils.rounded(stack, xOffset, height - 3, 16, 1.5F, 1.0F, 0.0F, bgColor.getRGB(), bgColor.getRGB());
                    this.armorBar.render(stack, xOffset, height - 3, 16 * durabilityValue, 1.5F, 1.0F, 0.0F);
                }
            }
        }
    }

    private boolean armorFound() {
        for (int i = 0; i < 4; i++) {
            ItemStack itemStack = BlackOut.mc.player.getInventory().armor.get(i);
            if (!itemStack.isEmpty()) {
                return true;
            }
        }

        return false;
    }
}
