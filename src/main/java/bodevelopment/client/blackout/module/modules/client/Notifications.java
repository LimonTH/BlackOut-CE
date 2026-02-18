package bodevelopment.client.blackout.module.modules.client;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.managers.NotificationManager;
import bodevelopment.client.blackout.module.SettingsModule;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.rendering.renderer.TextureRenderer;
import bodevelopment.client.blackout.rendering.texture.BOTextures;
import bodevelopment.client.blackout.util.render.AnimUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

public class Notifications extends SettingsModule {
    private static Notifications INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Boolean> chatNotifications = this.sgGeneral.b("Chat Notifications", false, "Do we send notifications in chat.");
    public final Setting<Boolean> hudNotifications = this.sgGeneral.b("Hud Notifications", true, "Do we render notifications on the hud.");
    public final Setting<Boolean> sound = this.sgGeneral.b("Play Sound", true, ".");
    private final SettingGroup sgRender = this.addGroup("Render");
    private final Setting<Style> style = this.sgRender.e("Style", Style.Classic, "What style to use", () -> true);
    private final Setting<Double> rounding = this.sgRender
            .d("Rounding", 0.5, 0.0, 1.0, 0.01, ".", () -> this.style.get() == Style.Classic || this.style.get() == Style.New);
    private final Setting<Boolean> bold = this.sgRender
            .b("Bold", true, ".", () -> this.style.get() == Style.Slim || this.style.get() == Style.NewSlim);
    private final Setting<Integer> bgAlpha = this.sgRender
            .i("Background Alpha", 175, 0, 255, 1, ".", () -> this.style.get() == Style.New || this.style.get() == Style.NewSlim);
    private final Setting<BlackOutColor> bgColor = this.sgRender
            .c(
                    "Background Color",
                    new BlackOutColor(0, 0, 0, 50),
                    ".",
                    () -> this.style.get() != Style.New || this.style.get() != Style.NewSlim
            );
    private final Setting<BlackOutColor> shadowColor = this.sgRender
            .c(
                    "Shadow Color",
                    new BlackOutColor(0, 0, 0, 100),
                    ".",
                    () -> this.style.get() != Style.New || this.style.get() != Style.NewSlim
            );
    private final Setting<Boolean> blur = this.sgRender.b("Blur", true, ".");
    private final Setting<Boolean> shadow = this.sgRender.b("Shadow", true, ".");
    private final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgGeneral, "Text");
    private final Setting<Formatting> nameColor = this.sgRender.e("Name Color", Formatting.RED, ".");
    private final Setting<Formatting> bracketColor = this.sgRender.e("Bracket Color", Formatting.DARK_GRAY, ".");
    private final Setting<Formatting> txtColor = this.sgRender.e("Chat Text Color", Formatting.WHITE, ".");

    public Notifications() {
        super("Notifications", true, true);
        INSTANCE = this;
    }

    public static Notifications getInstance() {
        return INSTANCE;
    }

    public String getClientPrefix() {
        return this.getPrefix(BlackOut.NAME);
    }

    public String getPrefix(String prefixName) {
        return String.format("%s[%s%s%s]%s", this.bracketColor.get(), this.nameColor.get(), prefixName, this.bracketColor.get(), this.txtColor.get());
    }

    public float render(MatrixStack stack, NotificationManager.Notification n, float offset) {
        double delta = this.getAnimationProgress(n.startTime, n.time);
        float y = BlackOut.mc.getWindow().getHeight() - offset;
        double bar = 1.0 - Math.min((double) (System.currentTimeMillis() - n.startTime), 1000.0) / 1000.0;
        float returnHeight = 0.0F;

        TextureRenderer t = switch (n.type) {
            case Enable -> BOTextures.getEnableIconRenderer();
            case Disable -> BOTextures.getDisableIconRenderer();
            case Info -> BOTextures.getInfoIconRender();
            default -> BOTextures.getAlertIconRenderer();
        };
        int alpha = this.style.get() == Style.Old ? 255 : 125;

        Color c = switch (n.type) {
            case Enable -> this.style.get() != Style.New && this.style.get() != Style.NewSlim
                    ? new Color(42, 121, 42, alpha)
                    : new Color(0, 175, 0, this.bgAlpha.get());
            case Disable -> this.style.get() != Style.New && this.style.get() != Style.NewSlim
                    ? new Color(255, 0, 0, alpha)
                    : new Color(185, 0, 0, this.bgAlpha.get());
            case Info -> this.style.get() != Style.New && this.style.get() != Style.NewSlim
                    ? new Color(135, 135, 255, alpha)
                    : new Color(80, 80, 80, this.bgAlpha.get());
            default -> new Color(
                    255, 200, 20, this.style.get() != Style.New && this.style.get() != Style.NewSlim ? alpha : this.bgAlpha.get()
            );
        };
        float fHeight = this.bold.get() ? BlackOut.BOLD_FONT.getHeight() : BlackOut.FONT.getHeight();
        stack.push();
        float width;
        float x;
        int r;
        float roundedWidth;
        float roundedHeight;
        float tWidth;
        float tHeight;
        float textWidth;
        switch (this.style.get()) {
            case Classic:
                returnHeight = 40.0F;
                width = Math.max(150.0F, BlackOut.FONT.getWidth(n.text) * 2.0F + 50.0F);
                x = (float) (BlackOut.mc.getWindow().getWidth() - (width + 20.0F) * delta);
                r = this.getRounding();

                stack.translate(x + r - 5.0F, y + r - 5.0F, 0.0F);
                roundedWidth = width - r * 2 + 10.0F;
                roundedHeight = 40.0F - r * 2 + 10.0F;

                RenderUtils.rounded(
                        stack, 0.0F, 0.0F, roundedWidth, roundedHeight, r, this.shadow.get() ? 3.0F : 0.0F, this.bgColor.get().getRGB(), this.shadowColor.get().getRGB()
                );

                stack.translate(25.0F - r, 22.0F - r, 0.0F);

                RenderUtils.circle(stack, 0.0F, 0.0F, 16.0F, c.getRGB());

                float iScale = 3.0F;
                float iVisualY = 0.5F;

                BlackOut.FONT.text(stack, "i", iScale, 0.0F, iVisualY, Color.WHITE, true, true);
                this.textColor.render(stack, n.text, 2.0F, 25.0F, 0.0F, false, true);
                break;
            case Slim:
                returnHeight = 30.0F;
                roundedHeight = fHeight * 3.0F;
                width = this.getWidth(n.text) * 2.0F + 4.0F;
                x = (float) (BlackOut.mc.getWindow().getWidth() - (width + 20.0F) * delta);
                stack.translate(x, y, 0.0F);
                if (this.blur.get()) {
                    RenderUtils.drawLoadedBlur("hudblur", stack, renderer -> renderer.rounded(0.0F, 0.0F, width, roundedHeight, 6.0F, 10));
                    Renderer.onHUDBlur();
                }

                RenderUtils.rounded(
                        stack, 0.0F, 0.0F, width, roundedHeight, 6.0F, this.shadow.get() ? 6.0F : 0.0F, this.bgColor.get().getRGB(), this.shadowColor.get().getRGB()
                );
                this.textColor.render(stack, n.text, 2.0F, 2.0F, fHeight / 2.0F + 1.0F, false, false, this.bold.get());
                break;
            case NewSlim:
                returnHeight = 30.0F;
                tWidth = t.getWidth() / 4.8F;
                tHeight = t.getHeight() / 4.8F;
                roundedHeight = fHeight * 3.0F;
                width = this.getWidth(n.text) * 2.0F + 6.0F + tHeight;
                x = (float) (BlackOut.mc.getWindow().getWidth() - (width + 20.0F) * delta);
                stack.translate(x, y, 0.0F);
                if (this.blur.get()) {
                    RenderUtils.drawLoadedBlur("hudblur", stack, renderer -> renderer.rounded(0.0F, 0.0F, width, roundedHeight, 6.0F, 10));
                    Renderer.onHUDBlur();
                }

                RenderUtils.rounded(stack, 0.0F, 0.0F, width, roundedHeight, 6.0F, this.shadow.get() ? 6.0F : 0.0F, c.getRGB(), c.getRGB());
                t.quad(stack, -1.0F, -1.0F, tWidth, tHeight);
                this.textColor.render(stack, n.text, 2.0F, 4.0F + tWidth, fHeight / 2.0F + 1.0F, false, false, this.bold.get());
                break;
            case New:
                returnHeight = 40.0F;
                textWidth = Math.max(BlackOut.BOLD_FONT.getWidth(n.bigText) * 2.5F, BlackOut.FONT.getWidth(n.text) * 2.0F);
                width = Math.max(150.0F, textWidth + 50.0F);
                x = (float) (BlackOut.mc.getWindow().getWidth() - (width + 20.0F) * delta);
                r = this.getRounding();
                stack.translate(x + r - 5.0F, y + r - 5.0F, 0.0F);
                roundedWidth = width - r * 2 + 10.0F;
                roundedHeight = 40 - r * 2 + 10;
                if (this.blur.get()) {
                    RenderUtils.drawLoadedBlur("hudblur", stack, renderer -> renderer.rounded(0.0F, 0.0F, roundedWidth, roundedHeight, r, 10));
                    Renderer.onHUDBlur();
                }

                RenderUtils.rounded(stack, 0.0F, 0.0F, roundedWidth, roundedHeight, r, this.shadow.get() ? 5.0F : 0.0F, c.getRGB(), c.getRGB());
                stack.translate(25 - r, 25 - r, 0.0F);
                tWidth = t.getWidth() / 5.0F;
                tHeight = t.getHeight() / 5.0F;
                t.quad(stack, -tWidth / 2.0F, -tHeight / 2.0F, tWidth, tHeight);
                this.textColor.render(stack, n.bigText, 2.5F, 25.0F, -BlackOut.BOLD_FONT.getHeight() * 2.5F + 9.0F, false, true, true);
                this.textColor.render(stack, n.text, 2.0F, 25.0F, BlackOut.FONT.getHeight() * 2.0F - 2.0F, false, true);
                break;
            case Old:
                returnHeight = 65.0F;
                textWidth = Math.max(BlackOut.BOLD_FONT.getWidth(n.bigText) * 2.5F, BlackOut.FONT.getWidth(n.text) * 2.0F);
                width = Math.max(150.0F, textWidth + 50.0F);
                float height = 60.0F;
                x = (float) (BlackOut.mc.getWindow().getWidth() - (width + 20.0F) * delta);
                stack.translate(x - 5.0F, y - 5.0F, 0.0F);
                if (this.blur.get()) {
                    RenderUtils.drawLoadedBlur("hudblur", stack, renderer -> renderer.rounded(0.0F, 0.0F, width, height, 0.0F, 10));
                    Renderer.onHUDBlur();
                }

                RenderUtils.rounded(
                        stack, 0.0F, 0.0F, width, height, 0.0F, this.shadow.get() ? 5.0F : 0.0F, this.bgColor.get().getRGB(), this.shadowColor.get().getRGB()
                );
                RenderUtils.quad(stack, 0.0F, height - 5.0F, width, 5.0F, this.bgColor.get().getRGB());
                RenderUtils.quad(stack, 0.0F, height - 5.0F, (float) (width * bar), 5.0F, c.getRGB());
                this.textColor.render(stack, n.bigText, 2.5F, 5.0F, 5.0F, false, false, true);
                this.textColor.render(stack, n.text, 2.2F, 5.0F, height - 15.0F - BlackOut.FONT.getHeight() * 1.3F, false, false);
                tWidth = t.getWidth() / 7.0F;
                tHeight = t.getHeight() / 7.0F;
                t.quad(stack, width - tWidth - 5.0F, 5.0F, tWidth, tHeight);
        }

        stack.pop();
        return MathHelper.lerp((float) delta, returnHeight, returnHeight + 20.0F);
    }

    private double getAnimationProgress(long start, long time) {
        if (System.currentTimeMillis() - start < time / 2L) {
            double delta = Math.min((double) (System.currentTimeMillis() - start), 500.0) / 500.0;
            return AnimUtils.easeOutQuart(delta);
        } else {
            double delta = Math.min((double) (start + time - System.currentTimeMillis()), 500.0) / 500.0;
            return AnimUtils.easeOutBack(delta);
        }
    }

    private int getRounding() {
        return (int) Math.round(this.rounding.get() * 20.0);
    }

    private float getWidth(String text) {
        return this.bold.get() ? BlackOut.BOLD_FONT.getWidth(text) : BlackOut.FONT.getWidth(text);
    }

    public enum Style {
        Classic,
        Slim,
        New,
        Old,
        NewSlim
    }

    public enum Type {
        Enable,
        Disable,
        Info,
        Alert
    }
}
