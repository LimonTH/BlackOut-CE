package bodevelopment.client.blackout.gui.clickgui.screens;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.addon.AddonLoader;
import bodevelopment.client.blackout.addon.BlackoutAddon;
import bodevelopment.client.blackout.gui.clickgui.ClickGuiScreen;
import bodevelopment.client.blackout.rendering.renderer.TextureRenderer;
import bodevelopment.client.blackout.rendering.texture.BOTextures;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.FileUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import org.apache.commons.lang3.mutable.MutableDouble;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AddonScreen extends ClickGuiScreen {
    private static final int LINE_COLOR = new Color(50, 50, 50, 255).getRGB();
    private final Map<BlackoutAddon, MutableDouble> hoverAnims = new HashMap<>();

    public AddonScreen() {
        super("Addons", 800.0F, 500.0F, true);
        AddonLoader.addons.forEach(a -> hoverAnims.put(a, new MutableDouble(0)));
    }

    @Override
    protected float getLength() {
        return AddonLoader.addons.size() * 70.0F + 120.0F;
    }

    @Override
    public void render() {
        RenderUtils.rounded(this.stack, 0, 0, width, height - 40.0F, 10, 10, GuiColorUtils.bg1.getRGB(), ColorUtils.SHADOW100I);

        this.stack.push();
        this.stack.translate(0.0F, 15.0F - this.scroll.get(), 0.0F);

        for (int i = 0; i < AddonLoader.addons.size(); i++) {
            BlackoutAddon addon = AddonLoader.addons.get(i);
            MutableDouble anim = hoverAnims.computeIfAbsent(addon, k -> new MutableDouble(0));

            float yPos = i * 70.0F;
            boolean hovered = mx > 10 && mx < width - 10 && (my + scroll.get() - 15) > yPos && (my + scroll.get() - 15) < yPos + 70;

            if (hovered) anim.setValue(Math.min(anim.getValue() + frameTime * 10, 1.0));
            else anim.setValue(Math.max(anim.getValue() - frameTime * 5, 0.0));

            if (anim.getValue() > 0) {
                int alpha = (int) (anim.getValue() * 25);
                RenderUtils.rounded(this.stack, 5, 5, width - 10, 60, 8, 0, ColorUtils.withAlpha(Color.WHITE.getRGB(), alpha), 0);
            }

            renderAddonRow(addon, i == 0);
        }
        this.stack.pop();

        this.renderButtons();
    }

    private void renderAddonRow(BlackoutAddon addon, boolean first) {
        if (!first) {
            RenderUtils.line(this.stack, -10.0F, 0.0F, this.width + 10.0F, 0.0F, LINE_COLOR);
        }
        BlackOut.FONT.text(this.stack, addon.getName(), 2.5F, 10.0F, 35.0F, Color.WHITE, false, true);

        String info = "v" + addon.getVersion() + " by " + addon.getAuthor();
        BlackOut.FONT.text(this.stack, info, 1.8F, 10.0F, 55.0F, Color.GRAY, false, true);

        String modulesCount = (addon.modules != null ? addon.modules.size() : 0) + " Modules";
        float tw = BlackOut.FONT.getWidth(modulesCount) * 1.8F;
        BlackOut.FONT.text(this.stack, modulesCount, 1.8F, width - tw - 20.0F, 35.0F, GuiColorUtils.parentCategory, false, true);

        this.stack.translate(0.0F, 70.0F, 0.0F);
    }

    private void renderButtons() {
        RenderUtils.roundedBottom(this.stack, 0.0F, this.height - 105.0F, this.width, 65.0F, 10.0F, 0.0F, GuiColorUtils.bg2.getRGB(), 0);
        RenderUtils.topFade(this.stack, -10.0F, this.height - 125.0F, this.width + 20.0F, 20.0F, GuiColorUtils.bg2.getRGB());
        RenderUtils.line(this.stack, -10.0F, this.height - 105.0F, this.width + 10.0F, this.height - 105.0F, LINE_COLOR);

        renderIconButton("Folder", BOTextures.getFolderIconRenderer(), width / 2.0F - 60.0F, height - 70.0F);
        renderIconButton("Cloud", BOTextures.getCloudIconRenderer(), width / 2.0F + 60.0F, height - 70.0F);
    }

    private void renderIconButton(String name, TextureRenderer icon, float x, float y) {
        boolean hovered = Math.abs(mx - x) < 40 && Math.abs(my - y) < 30;
        float s = hovered ? 1.1F : 1.0F;

        this.stack.push();
        this.stack.translate(x, y, 0);
        this.stack.scale(s, s, 1.0F);

        float ratio = icon.getWidth() / 36.0F;
        float iconW = icon.getWidth() / ratio;
        float iconH = icon.getHeight() / ratio;

        icon.quad(this.stack, -iconW / 2.0F, -iconH / 2.0F - 10.0F, iconW, iconH, hovered ? Color.WHITE.getRGB() : Color.LIGHT_GRAY.getRGB());
        BlackOut.FONT.text(this.stack, name, 1.6F, 0.0F, 18.0F, hovered ? Color.WHITE : Color.GRAY, true, true);

        this.stack.pop();
    }

    @Override
    public void onMouse(int button, boolean state) {
        if (state && button == 0) {
            if (my > height - 105 && my < height - 40) {
                if (Math.abs(mx - (width / 2.0F - 60.0F)) < 40) {
                    FileUtils.openDirectory(new File(BlackOut.RUN_DIRECTORY, "mods"));
                } else if (Math.abs(mx - (width / 2.0F + 60.0F)) < 40) {
                    FileUtils.openLink("https://github.com/LimonTH/Blackout-CE"); // TODO: НЕТ ССЫЛКИ НА АДДОНЫ(АДДОНОВ НЕТ =)) Стоит заглушка на репозиторий
                }
            }
        }
    }
}