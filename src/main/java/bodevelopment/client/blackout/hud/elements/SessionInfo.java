package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BackgroundMultiSetting;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import java.awt.*;

public class SessionInfo extends HudElement {
    private final SettingGroup sgGeneral = this.addGroup("General");

    public final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgGeneral, "Label Color");
    private final Setting<Style> style = this.sgGeneral.enumSetting("Visual Theme", Style.Blackout, "The aesthetic layout of the session tracker (Modern 'Blackout' vs. legacy 'Exhibition').");
    private final Setting<Boolean> bar = this.sgGeneral.booleanSetting("Header Separator", true, "Renders a horizontal line beneath the title for structural definition.", () -> this.style.get() == Style.Blackout);
    private final Setting<BlackOutColor> barColor = this.sgGeneral.colorSetting("Separator Color", new BlackOutColor(255, 255, 255, 255), "The color palette for the header separator line.", () -> this.style.get() == Style.Blackout && this.bar.get());
    private final Setting<Boolean> bg = this.sgGeneral.booleanSetting("Enable Backdrop", true, "Renders a background panel behind the session statistics.", () -> this.style.get() == Style.Blackout);
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgGeneral, this.bg::get, null);
    private final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Gaussian Diffusion", true, "Applies a real-time blur effect to the background for improved contrast.", () -> this.style.get() == Style.Blackout);
    private final Setting<Mode> mode = this.sgGeneral.enumSetting("Elimination Tracking", Mode.Chat, "The logic used to detect player kills (Parsing chat logs or listening to game events).");

    private static final long startTime = System.currentTimeMillis();
    private int kills = 0;
    private int deaths = 0;
    private float height = 0.0F;
    private float width = 0.0F;
    private String ip = "";
    private boolean isDead = false;
    public SessionInfo() {
        super("Session Info", "Visualizes real-time metrics for the current play session, including uptime, server address, and combat statistics.");
        this.setSize(10.0F, 10.0F);
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
    }

    @Override
    public void render() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            String timeString = OLEPOSSUtils.getTimeString(System.currentTimeMillis() - startTime);
            this.ip = !BlackOut.mc.isIntegratedServerRunning() && BlackOut.mc.getNetworkHandler() != null && BlackOut.mc.getNetworkHandler().getServerInfo() != null
                    ? BlackOut.mc.getNetworkHandler().getServerInfo().address
                    : "Singleplayer";
            this.stack.push();
            if (BlackOut.mc.player.isDead() && !this.isDead) {
                this.deaths++;
                this.isDead = true;
            } else if (!BlackOut.mc.player.isDead()) {
                this.isDead = false;
            }

            float num = 0.0F;
            switch (this.style.get()) {
                case Blackout:
                    num = this.bar.get() ? 15.0F : BlackOut.FONT.getHeight() * 1.5F;
                    this.height = num + BlackOut.FONT.getHeight() * 4.0F;
                    this.width = BlackOut.FONT.getWidth("Session Info") * 1.5F + 14.0F;
                    this.setSize(this.width, this.height);
                    if (this.blur.get()) {
                        RenderUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(0.0F, 0.0F, this.width, this.height, 3.0F, 10));
                        Renderer.onHUDBlur();
                    }

                    if (this.bg.get()) {
                        this.background.render(this.stack, 0.0F, 0.0F, this.width, this.height, 3.0F, 3.0F);
                    }

                    this.textColor.render(this.stack, "Session Info", 1.5F, this.width / 2.0F, 0.0F, true, false);
                    if (this.bar.get()) {
                        RenderUtils.rounded(
                                this.stack,
                                2.0F,
                                BlackOut.FONT.getHeight() * 1.5F,
                                this.width - 4.0F,
                                0.1F,
                                0.5F,
                                0.0F,
                                this.barColor.get().getRGB(),
                                Color.WHITE.getRGB()
                        );
                    }

                    this.textColor.render(this.stack, this.ip, 1.0F, 5.0F, num, false, false);
                    this.textColor.render(this.stack, "Kills: " + this.kills, 1.0F, 5.0F, num + BlackOut.FONT.getHeight(), false, false);
                    this.textColor.render(this.stack, "Deaths: " + this.deaths, 1.0F, 5.0F, num + BlackOut.FONT.getHeight() * 2.0F, false, false);
                    this.textColor.render(this.stack, timeString, 1.0F, 5.0F, num + BlackOut.FONT.getHeight() * 3.0F, false, false);
                    break;
                case Exhibition:
                    num = BlackOut.FONT.getHeight();
                    this.width = BlackOut.FONT.getWidth(this.getLongest(timeString)) + 6.0F;
                    this.height = BlackOut.FONT.getHeight() * 3.0F + 6.0F;
                    this.stack.translate(-2.0F, -2.0F, 0.0F);
                    this.setSize(this.width + 6.0F, this.height + 4.0F);
                    RenderUtils.drawSkeetBox(this.stack, 0.0F, 0.0F, this.width + 10.0F, this.height + 8.0F, true);
                    this.textColor.render(this.stack, this.ip, 1.0F, 4.0F, 4.0F, false, false);
                    this.textColor.render(this.stack, "Kills: " + this.kills, 1.0F, 4.0F, 4.0F + num, false, false);
                    this.textColor.render(this.stack, "Deaths: " + this.deaths, 1.0F, 4.0F, 4.0F + num * 2.0F, false, false);
                    this.textColor.render(this.stack, timeString, 1.0F, 4.0F, 4.0F + num * 3.0F, false, false);
            }

            this.stack.pop();
        }
    }

    @Event
    public void onReceive(PacketEvent.Receive.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (this.mode.get() == Mode.Chat) {
                if (event.packet instanceof GameMessageS2CPacket packet) {
                    String unformattedText = packet.content().getString();
                    String name = BlackOut.mc.player.getName().getString();
                    String[] look = new String[]{
                            "wurde von " + name,
                            name + " killed ",
                            "killed by " + name,
                            "slain by " + name,
                            "You received a reward for killing ",
                            "while escaping " + name,
                            "You have won",
                            "You have killed"
                    };
                    if (unformattedText == null) {
                        return;
                    }

                    for (String s : look) {
                        if (unformattedText.contains(s)) {
                            this.kills++;
                        }
                    }
                }
            }
        }
    }

    private String getLongest(String time) {
        String[] texts = new String[]{"Session Info", this.ip, "Kills: " + this.kills, "Deaths: " + this.deaths, time};
        String longestString = "";
        int maxLength = 0;

        for (String text : texts) {
            int currentLength = text.length();
            if (currentLength > maxLength) {
                maxLength = currentLength;
                longestString = text;
            }
        }

        return longestString;
    }

    public enum Mode {
        Chat,
        Event
    }

    public enum Style {
        Blackout,
        Exhibition
    }
}
