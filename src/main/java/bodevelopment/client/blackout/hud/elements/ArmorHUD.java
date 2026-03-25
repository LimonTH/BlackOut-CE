package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.util.PlayerUtils;
import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BackgroundMultiSetting;
import bodevelopment.client.blackout.module.setting.multisettings.RoundedColorMultiSetting;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.render.RenderLayer;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;
import net.minecraft.world.item.ItemStack;

public class ArmorHUD extends HudElement {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgColor = this.addGroup("Color");

    private final Setting<Boolean> reversed = this.sgGeneral.booleanSetting("Invert Order", false, "Flips the rendering sequence of armor pieces.");
    private final Setting<Boolean> bg = this.sgGeneral.booleanSetting("Backdrop", true, "Renders a background panel.");
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgGeneral, this.bg::get, null);
    private final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Gaussian Blur", true, "Applies a blur effect.");
    private final Setting<Boolean> shadow = this.sgGeneral.booleanSetting("Drop Shadow", true, "Adds a subtle shadow effect.");
    private final Setting<Boolean> bar = this.sgGeneral.booleanSetting("Durability Bar", false, "Visualizes remaining durability as a bar.");
    private final Setting<Boolean> text = this.sgGeneral.booleanSetting("Percentage Text", true, "Displays durability as percentage.");
    private final Setting<Boolean> centerText = this.sgGeneral.booleanSetting("Align Center", true, "Centers the durability text.");
    private static final int BAR_BG = new Color(0, 0, 0, 120).getRGB();
    private final RoundedColorMultiSetting armorBar = RoundedColorMultiSetting.of(this.sgGeneral, "Bar Color");
    private final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgColor, "Text Color");

    public ArmorHUD() {
        super("Armor HUD", "Displays a live overview of your equipped armor pieces.");
        this.setSize(88.0F, 18.0F);
    }

    @Override
    public void render() {
        if (!PlayerUtils.isInGame()) return;
        if (!armorFound()) return;

        int armorCount = 0;
        for (int i = 0; i < 4; i++) {
            if (!BlackOut.mc.player.getInventory().armor.get(i).isEmpty()) armorCount++;
        }

        if (armorCount == 0) return;

        float bgWidth = 2.0F + (armorCount * 22.0F);
        float bgHeight = this.bar.get() ? 22.0F : 18.0F;
        this.setSize(bgWidth, bgHeight);

        this.stack.pushPose();
        this.draw(this.stack);
        this.stack.popPose();
    }

    private void draw(PoseStack stack) {
        float width = this.getWidth();
        float height = this.getHeight();

        if (this.blur.get()) {
            RenderUtils.drawLoadedBlur("hudblur", stack, r -> r.rounded(0.0F, 0.0F, width, height, 3.0F, 10));
            Renderer.onHUDBlur();
        }

        if (this.bg.get()) {
            this.background.render(stack, 0.0F, 0.0F, width, height, 3.0F, this.shadow.get() ? 3.0F : 0.0F);
        }

        BlackOut.mc.renderBuffers().bufferSource().endBatch();
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        BlackOut.mc.gameRenderer.lightTexture().turnOffLightLayer();

        int renderedIdx = 0;
        for (int i = 0; i < 4; i++) {
            ItemStack itemStack = BlackOut.mc.player.getInventory().armor.get(this.reversed.get() ? i : 3 - i);
            float xOffset = 2 + (22 * renderedIdx);

            if (itemStack.isEmpty()) continue;

            RenderUtils.renderItem(stack, itemStack, xOffset, 1.0F, 16.0F, RenderLayer.HUD, false);

            if (itemStack.isDamageableItem()) {
                float durabilityValue = (float) (itemStack.getMaxDamage() - itemStack.getDamageValue()) / itemStack.getMaxDamage();

                if (this.text.get()) {
                    int durabilityPercentage = Math.round(durabilityValue * 100.0f);
                    String textStr = durabilityPercentage + "%";
                    this.textColor.render(stack, textStr, 0.6F, xOffset + (this.centerText.get() ? 8 : 0), 13.0F, this.centerText.get(), true);
                }

                if (this.bar.get()) {
                    float barY = 18.5F;
                    RenderUtils.rounded(stack, xOffset, barY, 16.0F, 1.5F, 1.0F, 0.0F, BAR_BG, BAR_BG);

                    float barWidth = Math.max(0.5F, 16.0F * durabilityValue);
                    this.armorBar.render(stack, xOffset, barY, barWidth, 1.5F, 1.0F, 0.0F);
                }
            }
            renderedIdx++;
        }
        BlackOut.mc.renderBuffers().bufferSource().endBatch();
    }

    private boolean armorFound() {
        for (int i = 0; i < 4; i++) {
            if (!BlackOut.mc.player.getInventory().armor.get(i).isEmpty()) return true;
        }
        return false;
    }
}