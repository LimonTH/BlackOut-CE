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
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;

import java.awt.*;

public class ArmorHUD extends HudElement {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgColor = this.addGroup("Color");
    private final Setting<Boolean> reversed = this.sgGeneral.b("Reversed", false, ".");
    private final Setting<Boolean> bg = this.sgGeneral.b("Background", true, "Renders a background");
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgGeneral, this.bg::get, null);
    private final Setting<Boolean> armorBG = this.sgGeneral.b("Armor BG", true, ".");
    private final Setting<Boolean> blur = this.sgGeneral.b("Blur", true, "Renders a Blur effect");
    private final Setting<Boolean> shadow = this.sgGeneral.b("Shadow", true, "Renders a Shadow");
    private final Setting<Boolean> bar = this.sgGeneral.b("% Bar", false, "Renders a bar");
    private final Setting<Boolean> text = this.sgGeneral.b("% Text", true, ".");
    private final Setting<Boolean> centerText = this.sgGeneral.b("Center Text", true, ".");
    private final RoundedColorMultiSetting armorBar = RoundedColorMultiSetting.of(this.sgGeneral, "Armor Bar");
    private final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgColor, "Text");

    public ArmorHUD() {
        super("Armor HUD", ".");
        this.setSize(80.0F, 19.0F);
    }

    @Override
    public void render() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null && armorFound()) {
            this.setSize(80.0F, 19.0F);
            this.stack.push();
            this.draw(this.stack);
            this.stack.pop();
        }
    }

    private void draw(MatrixStack stack) {
        float bgHeight = this.bar.get() ? 20.0F : 14.0F;
        if (this.blur.get()) {
            RenderUtils.drawLoadedBlur("hudblur", stack, renderer -> renderer.rounded(0.0F, 0.0F, 80.0F, bgHeight, 3.0F, 10));
            Renderer.onHUDBlur();
        }

        if (this.bg.get()) {
            this.background.render(stack, 0.0F, 0.0F, 80.0F, bgHeight, 3.0F, this.shadow.get() ? 3.0F : 0.0F);
        }

        for (int i = 0; i < 4; i++) {
            ItemStack itemStack = BlackOut.mc.player.getInventory().armor.get(this.reversed.get() ? i : 3 - i);
            if (this.armorBG.get()) {
                if (this.background.isStatic()) {
                    this.background.render(stack, 2 + 22 * i, 2.0F, 10.0F, 10.0F, 3.0F, this.shadow.get() ? 3.0F : 0.0F);
                } else {
                    RenderUtils.rounded(stack, 2 + 22 * i, 2.0F, 10.0F, 10.0F, 3.0F, 0.0F, new Color(0, 0, 0, 100).getRGB(), Color.BLACK.getRGB());
                }
            }

            if (!itemStack.isEmpty()) {
                RenderUtils.renderItem(stack, itemStack.getItem(), -1 + 22 * i, -1.0F, 16.0F);

                boolean isUnbreakable = itemStack.get(DataComponentTypes.UNBREAKABLE) != null;

                if (!isUnbreakable && itemStack.isDamageable()) {
                    float maxDamage = (float) itemStack.getMaxDamage();
                    float currentDamage = (float) itemStack.getDamage();
                    float durabilityValue = (maxDamage - currentDamage) / maxDamage;
                    int durabilityPercentage = Math.round(durabilityValue * 100.0f);

                    if (this.text.get()) {
                        this.textColor.render(stack, durabilityPercentage + " %", 0.6F, 22 * i + (this.centerText.get() ? 7 : 0), 12.0F, this.centerText.get(), true);
                    }

                    if (this.bar.get()) {
                        Color background = new Color(0, 0, 0, 85);
                        RenderUtils.rounded(stack, 22 * i, 19.0F, 14.0F, 0.3F, 1.0F, 0.0F, background.getRGB(), background.getRGB());

                        this.armorBar.render(stack, 22 * i, 19.0F, 14.0F * durabilityValue, 0.3F, 1.0F, 0.0F);
                    }
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
