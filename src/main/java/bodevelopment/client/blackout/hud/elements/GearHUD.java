package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BackgroundMultiSetting;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.InvUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.List;

public class GearHUD extends HudElement {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgColor = this.addGroup("Color");

    // TODO: shadow нигде не используется
    private final Setting<Double> textScale = this.sgScale.doubleSetting("Label Scale", 1.0, 0.0, 5.0, 0.05, "The size multiplier for the numerical item counters.");
    private final Setting<Boolean> bg = this.sgGeneral.booleanSetting("Backdrop", true, "Renders a background panel behind the gear list.");
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgGeneral, this.bg::get, null);
    private final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Gaussian Diffusion", true, "Applies a blur effect to the background for increased UI depth.");
    private final Setting<Boolean> shadow = this.sgGeneral.booleanSetting("Drop Shadow", true, "Adds a subtle shadow to the background for better structural definition.");
    private final Setting<List<Item>> items = this.sgGeneral.itemListSetting("Tracked Items", "The specific inventory items to monitor and display in the HUD.", Items.END_CRYSTAL, Items.TOTEM_OF_UNDYING);
    private final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgColor, "Counter Color");

    public GearHUD() {
        super("Gear HUD", "Monitors and displays the total count of specific items within the player's inventory, such as crystals or totems.");
        this.setSize(32.0F, 64.0F);
    }

    @Override
    public void render() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            float textWidth = 10.0F;

            for (Item item : this.items.get()) {
                textWidth = Math.max(textWidth, BlackOut.FONT.getWidth(String.valueOf(this.getAmount(item))) * this.textScale.get().floatValue());
            }

            textWidth += 2.0F;
            float backgroundWidth = textWidth + 16.0F;
            float length = this.items.get().size() * 16 + this.items.get().size() * 6 - 6;
            this.setSize(backgroundWidth, length);
            this.stack.push();
            if (this.blur.get()) {
                RenderUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(0.0F, 0.0F, backgroundWidth, length, 3.0F, 10));
                Renderer.onHUDBlur();
            }

            if (this.bg.get()) {
                this.background.render(this.stack, 0.0F, 0.0F, backgroundWidth, length, 3.0F, 3.0F);
            }

            for (Item item : this.items.get()) {
                int amount = this.getAmount(item);
                this.textColor.render(this.stack, String.valueOf(amount), this.textScale.get().floatValue(), textWidth / 2.0F, 8.0F, true, true);
                RenderUtils.renderItem(this.stack, item.getDefaultStack(), textWidth, 0.0F, 16.0F, 500.0F, true);
                this.stack.translate(0.0F, 22.0F, 0.0F);
            }

            this.stack.pop();
        }
    }

    private int getAmount(Item item) {
        return InvUtils.count(true, true, stack -> stack.getItem() == item);
    }
}
