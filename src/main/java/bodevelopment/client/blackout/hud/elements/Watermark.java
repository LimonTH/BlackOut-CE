package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BackgroundMultiSetting;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class Watermark extends HudElement {
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Mode> mode = this.sgGeneral.enumSetting("Mode", Mode.Clean, "What style to use", () -> true);
    private final Setting<String> extraText = this.sgGeneral
            .stringSetting("Extra Text", "", "Added text to the client name", () -> this.mode.get() == Mode.Exhibition && BlackOut.TYPE.isDevBuild());
    public final Setting<SigmaMode> sigmaMode = this.sgGeneral
            .enumSetting("Sigma Mode", SigmaMode.SigmaJello, "What Sigma style to use", () -> this.mode.get() == Mode.Sigma);
    private final TextColorMultiSetting textColor = TextColorMultiSetting.of(
            this.sgGeneral,
            () -> this.mode.get() == Mode.Clean
                    || this.mode.get() == Mode.Simple
                    || this.mode.get() == Mode.Exhibition
                    || this.mode.get() == Mode.KassuK,
            "Text"
    );
    private final Setting<BlackOutColor> secondaryColor = this.sgGeneral
            .colorSetting(
                    "Secondary Color",
                    new BlackOutColor(255, 255, 255, 255),
                    ".",
                    () -> this.mode.get() == Mode.Simple || this.mode.get() == Mode.Exhibition
            );
    private final Setting<Boolean> blur = this.sgGeneral
            .booleanSetting("Blur", true, ".", () -> this.mode.get() == Mode.Clean || this.mode.get() == Mode.KassuK);
    private final Setting<Boolean> bg = this.sgGeneral
            .booleanSetting("Background", true, ".", () -> this.mode.get() == Mode.Clean || this.mode.get() == Mode.KassuK);
    private final Setting<Boolean> bold = this.sgGeneral.booleanSetting("Bold Font", true, ".", () -> this.mode.get() == Mode.Simple);
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(
            this.sgGeneral,
            () -> this.mode.get() != Mode.Exhibition
                    && this.mode.get() != Mode.Virtue
                    && this.mode.get() != Mode.Remix
                    && this.mode.get() != Mode.Sigma
                    && this.mode.get() != Mode.Simple
                    && this.mode.get() != Mode.GameSense,
            null
    );

    public Watermark() {
        super("Watermark", "Renders the client watermark");
        this.setSize(10.0F, 10.0F);
    }

    @Override
    public void render() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            String formattedTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String formattedTime2 = new SimpleDateFormat("hh:mm a").format(new Date());
            this.stack.push();
            label79:
            switch (this.mode.get()) {
                case Simple: {
                    float width = BlackOut.FONT.getWidth(BlackOut.NAME) * 2.0F + BlackOut.FONT.getWidth(BlackOut.VERSION);
                    this.setSize(width, BlackOut.FONT.getHeight() * 2.0F);
                    this.stack.translate(0.0F, BlackOut.FONT.getHeight() + 2.0F, 0.0F);
                    this.textColor.render(this.stack, BlackOut.NAME, 2.0F, 0.0F, 0.0F, false, true, this.bold.get());
                    float x = this.bold.get() ? BlackOut.BOLD_FONT.getWidth(BlackOut.NAME) * 2.0F : BlackOut.FONT.getWidth(BlackOut.NAME) * 2.0F;
                    float y = this.bold.get() ? BlackOut.BOLD_FONT.getHeight() + 4.0F : BlackOut.FONT.getHeight() + 4.0F;
                    BlackOut.FONT.text(this.stack, BlackOut.VERSION, 1.0F, x, -y, this.secondaryColor.get().getColor(), false, false);
                    break;
                }
                case Virtue: {
                    String text = "Virtue 6";
                    float width = BlackOut.FONT.getWidth(text);
                    this.setSize(width + 16.0F, BlackOut.FONT.getHeight() * 3.0F + 6.0F);
                    RenderUtils.quad(this.stack, 0.0F, 0.0F, width + 16.0F, BlackOut.FONT.getHeight() * 3.0F + 6.0F, new Color(125, 125, 125, 100).getRGB());
                    BlackOut.FONT.text(this.stack, text, 1.0F, 8.0F, 2.0F, new Color(212, 212, 255, 255), false, false);
                    BlackOut.FONT
                            .text(this.stack, formattedTime2, 1.0F, 8.0F + width / 2.0F, 3.0F + BlackOut.FONT.getHeight(), new Color(230, 230, 230, 255), true, false);
                    BlackOut.FONT
                            .text(
                                    this.stack,
                                    "Fps " + BlackOut.mc.getCurrentFps(),
                                    1.0F,
                                    8.0F + width / 2.0F,
                                    4.0F + BlackOut.FONT.getHeight() * 2.0F,
                                    new Color(230, 230, 230, 255),
                                    true,
                                    false
                            );
                    RenderUtils.quad(this.stack, 0.0F, 0.0F, width + 16.0F, 1.0F, Color.BLACK.getRGB());
                    RenderUtils.quad(this.stack, 0.0F, BlackOut.FONT.getHeight() * 3.0F + 6.0F, width + 16.0F, 1.0F, Color.BLACK.getRGB());
                    RenderUtils.quad(this.stack, 0.0F, 0.0F, 1.0F, BlackOut.FONT.getHeight() * 3.0F + 6.0F, Color.BLACK.getRGB());
                    RenderUtils.quad(this.stack, width + 16.0F, 0.0F, 1.0F, BlackOut.FONT.getHeight() * 3.0F + 7.0F, Color.BLACK.getRGB());
                    break;
                }
                case Clean: {
                    String ip = !BlackOut.mc.isIntegratedServerRunning() && BlackOut.mc.getNetworkHandler() != null && BlackOut.mc.getNetworkHandler().getServerInfo() != null
                            ? BlackOut.mc.getNetworkHandler().getServerInfo().address
                            : "Singleplayer";
                    String text = "Blackout | "
                            + BlackOut.TYPE
                            + " | "
                            + BlackOut.mc.player.getName().getString()
                            + " | "
                            + ip
                            + " | "
                            + BlackOut.mc.getCurrentFps()
                            + " fps";
                    float width = BlackOut.FONT.getWidth(text) + 4.0F;
                    this.setSize(width, BlackOut.FONT.getHeight());
                    if (this.blur.get()) {
                        RenderUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(0.0F, 0.0F, width, BlackOut.FONT.getHeight() + 2.0F, 3.0F, 10));
                        Renderer.onHUDBlur();
                    }

                    if (this.bg.get()) {
                        this.background.render(this.stack, 0.0F, 0.0F, width, BlackOut.FONT.getHeight() + 2.0F, 3.0F, 3.0F);
                    }

                    this.textColor.render(this.stack, text, 1.0F, 2.0F, BlackOut.FONT.getHeight() / 2.0F + 1.0F, false, true);
                    break;
                }
                case GameSense: {
                    String ipx = !BlackOut.mc.isIntegratedServerRunning() && BlackOut.mc.getNetworkHandler() != null && BlackOut.mc.getNetworkHandler().getServerInfo() != null
                            ? BlackOut.mc.getNetworkHandler().getServerInfo().address
                            : "Singleplayer";
                    String text = "| "
                            + BlackOut.mc.player.getName().getString()
                            + " | "
                            + BlackOut.mc.getCurrentFps()
                            + " fps | "
                            + ipx
                            + " | "
                            + formattedTime;
                    float width = BlackOut.FONT.getWidth(BlackOut.NAME + text);
                    this.setSize(width + 10.0F, BlackOut.FONT.getHeight() + 8.0F);
                    RenderUtils.drawSkeetBox(this.stack, 0.0F, 0.0F, width + 10.0F, BlackOut.FONT.getHeight() + 8.0F, true);
                    BlackOut.FONT.text(this.stack, "Black", 1.0F, 4.0F, 5.0F, new Color(255, 255, 255, 255), false, false);
                    BlackOut.FONT.text(this.stack, "out", 1.0F, 4.0F + BlackOut.FONT.getWidth("Black"), 5.0F, new Color(50, 125, 50, 255), false, false);
                    BlackOut.FONT.text(this.stack, text, 1.0F, BlackOut.FONT.getWidth("Blackout ") + 4.0F, 5.0F, new Color(255, 255, 255, 255), false, false);
                    break;
                }
                case Sigma:
                    this.setSize(BlackOut.FONT.getWidth("Sigma") * 4.0F, BlackOut.FONT.getHeight() * 5.0F);
                    switch (this.sigmaMode.get()) {
                        case SigmaJello:
                            BlackOut.FONT.text(this.stack, "Sigma", 4.0F, 0.0F, 0.0F, new Color(255, 255, 255, 150), false, false);
                            BlackOut.FONT.text(this.stack, "Jello", 1.2F, 0.0F, 31.0F, new Color(255, 255, 255, 150), false, false);
                            break label79;
                        case SugmaYellow:
                            BlackOut.FONT.text(this.stack, "Sugma", 4.0F, 0.0F, 0.0F, new Color(255, 255, 0, 150), false, false);
                            BlackOut.FONT.text(this.stack, "Yellow", 1.2F, 0.0F, 32.0F, new Color(255, 255, 0, 150), false, false);
                            break label79;
                        default:
                            break label79;
                    }
                case Remix:
                    this.setSize(BlackOut.FONT.getWidth("Remix v1.6.6"), BlackOut.FONT.getHeight());
                    BlackOut.FONT.text(this.stack, "R", 1.0F, 1.0F, 2.0F, new Color(38, 183, 110, 255), false, false);
                    BlackOut.FONT.text(this.stack, "emix v1.6.6", 1.0F, 1.0F + BlackOut.FONT.getWidth("R"), 2.0F, Color.GRAY, false, false);
                    break;
                case Exhibition:
                    String extra = this.extraText.get() != null && !this.extraText.get().isEmpty() ? " - " + this.extraText.get() : "";
                    this.setSize(BlackOut.FONT.getWidth("Exhibition" + extra), BlackOut.FONT.getHeight());
                    this.textColor.render(this.stack, "E", 1.0F, 0.0F, 0.0F, false, false);
                    BlackOut.FONT.text(this.stack, "xhibition" + extra, 1.0F, BlackOut.FONT.getWidth("E"), 0.0F, this.secondaryColor.get().getColor(), false, false);
                    break;
                case KassuK: {
                    float width = BlackOut.FONT.getWidth(BlackOut.NAME) + 8.0F;
                    float height = BlackOut.FONT.getHeight() - 2.0F;
                    this.setSize(width, BlackOut.FONT.getHeight());
                    this.stack.translate(2.0F, 0.0F, 0.0F);
                    Color color = this.getWave(1, this.textColor.getTextColor().getColor(), this.textColor.getWaveColor().getColor());
                    Color color2 = this.getWave(2, this.textColor.getTextColor().getColor(), this.textColor.getWaveColor().getColor());
                    if (this.blur.get()) {
                        RenderUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(-3.0F, 1.0F, width, height, 3.0F, 10));
                        Renderer.onHUDBlur();
                    }

                    if (this.bg.get()) {
                        this.background.render(this.stack, -3.0F, 1.0F, width, height, 3.0F, 3.0F);
                    }

                    RenderUtils.rounded(this.stack, -2.0F, 1.0F, 0.1F, height, 0.5F, 0.5F, color.getRGB(), color.getRGB());
                    RenderUtils.rounded(this.stack, width - 4.1F, 1.0F, 0.1F, height, 0.5F, 0.5F, color2.getRGB(), color2.getRGB());
                    this.textColor.render(this.stack, BlackOut.NAME, 1.0F, 1.0F, BlackOut.FONT.getHeight() / 2.0F + 0.5F, false, true);
                }
            }

            this.stack.pop();
        }
    }

    private Color getWave(int i, Color color, Color color2) {
        return ColorUtils.getWave(color, color2, 1.0, 2.0, i);
    }

    public enum Mode {
        Simple,
        GameSense,
        Virtue,
        Clean,
        Sigma,
        Remix,
        KassuK,
        Exhibition
    }

    public enum SigmaMode {
        SigmaJello,
        SugmaYellow
    }
}
