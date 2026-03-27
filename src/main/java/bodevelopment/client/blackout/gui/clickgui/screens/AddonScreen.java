package bodevelopment.client.blackout.gui.clickgui.screens;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.addon.AddonLoader;
import bodevelopment.client.blackout.addon.BlackoutAddon;
import bodevelopment.client.blackout.gui.clickgui.ClickGuiScreen;
import bodevelopment.client.blackout.randomstuff.ShaderSetup;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.rendering.renderer.TextureRenderer;
import bodevelopment.client.blackout.rendering.shader.Shaders;
import bodevelopment.client.blackout.rendering.texture.BOTextures;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.FileUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL14;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AddonScreen extends ClickGuiScreen {
    private static final int LINE_COLOR = new Color(50, 50, 50, 255).getRGB();
    private final Map<BlackoutAddon, MutableDouble> hoverAnims = new HashMap<>();

    public AddonScreen() {
        super("Addons", 800.0F, 500.0F, true);
        AddonLoader.addons.forEach(a -> {
            hoverAnims.put(a, new MutableDouble(0));
            if (a.getIcon() == null) {
                RenderSystem.recordRenderCall(a::uploadIcon);
            }
        });
    }

    @Override
    protected float getLength() {
        return AddonLoader.addons.size() * 70.0F + 120.0F;
    }

    @Override
    public void render() {
        RenderUtils.rounded(this.stack, 0, 0, width, height - 40.0F, 10, 10, GuiColorUtils.bg1.getRGB(), ColorUtils.SHADOW100I);

        this.stack.pushPose();
        this.stack.translate(0.0F, 15.0F - this.scroll.get(), 0.0F);

        for (int i = 0; i < AddonLoader.addons.size(); i++) {
            BlackoutAddon addon = AddonLoader.addons.get(i);
            MutableDouble anim = hoverAnims.computeIfAbsent(addon, k -> new MutableDouble(0));

            float yPos = i * 70.0F;
            boolean hovered = mx > 10 && mx < width - 10 && (my + scroll.get() - 15) > yPos && (my + scroll.get() - 15) < yPos + 70;

            if (hovered) anim.setValue(Math.min(anim.getValue() + frameTime * 10, 1.0));
            else anim.setValue(Math.max(anim.getValue() - frameTime * 5, 0.0));

            this.stack.pushPose();
            this.stack.translate(0, yPos, 0);

            if (anim.getValue() > 0) {
                int alpha = (int) (anim.getValue() * 25);

                float r = 8.0F;
                RenderUtils.rounded(this.stack, 5 + r, 5 + r, width - 10 - r * 2, 60 - r * 2, r, 0, ColorUtils.withAlpha(Color.WHITE.getRGB(), alpha), 0);
            }

            renderAddonRow(addon, i == 0);

            this.stack.popPose();
        }
        this.stack.popPose();

        this.renderButtons();
    }

    private void renderAddonRow(BlackoutAddon addon, boolean first) {
        if (!first) {
            RenderUtils.line(this.stack, -10.0F, 0.0F, this.width + 10.0F, 0.0F, LINE_COLOR);
        }

        float textOffsetX = 15.0F;

        TextureRenderer icon = addon.getIcon();
        if (icon != null && icon.getWidth() > 0 && this.unscaled >= 1.0F) {
            float iconSize = 36.0F;
            float iconX = 15.0F;
            float iconY = (70.0F - iconSize) / 2.0F;
            renderIcon(icon, iconX, iconY, iconSize, iconSize);
            textOffsetX = 60.0F;
        }

        BlackOut.FONT.text(this.stack, addon.getName(), 2.5F, textOffsetX, 25.0F, Color.WHITE, false, true);

        String info = "v" + addon.getVersion() + " by " + addon.getAuthor();
        BlackOut.FONT.text(this.stack, info, 1.8F, textOffsetX, 45.0F, Color.GRAY, false, true);

        float rightOffset = 20.0F;

        if (addon.getUrl() != null) {
            TextureRenderer linkIcon = BOTextures.getGithubIconRenderer();
            float linkSize = 20.0F;
            float linkX = width - linkSize - rightOffset;
            float linkY = (70.0F - linkSize) / 2.0F;
            linkIcon.quad(this.stack, linkX, linkY, linkSize, linkSize, Color.LIGHT_GRAY.getRGB());
            rightOffset += 30.0F;
        }

        String modulesCount = (addon.modules != null ? addon.modules.size() : 0) + " Modules";
        float tw = BlackOut.FONT.getWidth(modulesCount) * 1.8F;
        BlackOut.FONT.text(this.stack, modulesCount, 1.8F, width - tw - rightOffset, 25.0F, GuiColorUtils.parentCategory, false, true);
    }

    private void renderIcon(TextureRenderer icon, float x, float y, float w, float h) {
        Matrix4f matrix = this.stack.last().pose();

        Renderer.setMatrices(this.stack);
        RenderSystem.enableBlend();
        GL13C.glActiveTexture(33984);
        GL13C.glBindTexture(GL13C.GL_TEXTURE_2D, icon.getId());

        var buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.addVertex(matrix, x, y, 0).setUv(0, 0);
        buf.addVertex(matrix, x, y + h, 0).setUv(0, 1);
        buf.addVertex(matrix, x + w, y + h, 0).setUv(1, 1);
        buf.addVertex(matrix, x + w, y, 0).setUv(1, 0);

        Shaders.texture.set("clr", 1.0f, 1.0f, 1.0f, 1.0f);
        Shaders.texture.set("uTexture", 0);
        Shaders.texture.render(buf, new ShaderSetup());

        GL13C.glBindTexture(GL13C.GL_TEXTURE_2D, GlStateManager.TEXTURES[0].binding);
        GL13C.glActiveTexture(33984 | GlStateManager.activeTexture);
        GL14.glBlendFuncSeparate(770, 771, 1, 1);
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

        this.stack.pushPose();
        this.stack.translate(x, y, 0);
        this.stack.scale(s, s, 1.0F);

        float ratio = icon.getWidth() / 36.0F;
        float iconW = icon.getWidth() / ratio;
        float iconH = icon.getHeight() / ratio;

        icon.quad(this.stack, -iconW / 2.0F, -iconH / 2.0F - 10.0F, iconW, iconH, hovered ? Color.WHITE.getRGB() : Color.LIGHT_GRAY.getRGB());
        BlackOut.FONT.text(this.stack, name, 1.6F, 0.0F, 18.0F, hovered ? Color.WHITE : Color.GRAY, true, true);

        this.stack.popPose();
    }

    @Override
    public void onMouse(int button, boolean state) {
        if (state && button == 0) {
            if (my > height - 105 && my < height - 40) {
                if (Math.abs(mx - (width / 2.0F - 60.0F)) < 40) {
                    FileUtils.openDirectory(new File(BlackOut.RUN_DIRECTORY, "mods"));
                } else if (Math.abs(mx - (width / 2.0F + 60.0F)) < 40) {
                    FileUtils.openLink("https://github.com/LimonTH/Blackout-CE-addon-template");
                }
            }

            float scrolledMy = (float) (my + scroll.get() - 15.0);
            for (int i = 0; i < AddonLoader.addons.size(); i++) {
                BlackoutAddon addon = AddonLoader.addons.get(i);
                if (addon.getUrl() == null) continue;

                float yPos = i * 70.0F;
                float linkSize = 20.0F;
                float linkX = width - linkSize - 20.0F;
                float linkY = yPos + (70.0F - linkSize) / 2.0F;

                if (mx >= linkX && mx <= linkX + linkSize && scrolledMy >= linkY && scrolledMy <= linkY + linkSize) {
                    FileUtils.openLink(addon.getUrl());
                    break;
                }
            }
        }
    }
}
