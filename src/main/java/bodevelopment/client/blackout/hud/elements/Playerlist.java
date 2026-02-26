package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.manager.managers.StatsManager;
import bodevelopment.client.blackout.module.modules.combat.misc.AntiBot;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BackgroundMultiSetting;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.rendering.renderer.TextureRenderer;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Playerlist extends HudElement {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<NameMode> nameMode = this.sgGeneral.enumSetting("Label Protocol", NameMode.EntityName, "Determines whether to show formatted display names or raw entity names.");
    private final Setting<Boolean> bg = this.sgGeneral.booleanSetting("Backdrop", true, "Renders a background panel behind the player statistics.");
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgGeneral, this.bg::get, null);
    private final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Gaussian Diffusion", true, "Applies a real-time blur effect to the backdrop for improved data legibility.");
    private final Setting<Boolean> dynamic = this.sgGeneral.booleanSetting("Contextual Coloring", false, "Dynamically colors health, ping, and pop values based on their severity.");
    private final Setting<BlackOutColor> good = this.sgGeneral.colorSetting("Positive Threshold", new BlackOutColor(0, 225, 0, 255), "The color assigned to healthy states (High health, low ping).", this.dynamic::get);
    private final Setting<BlackOutColor> bad = this.sgGeneral.colorSetting("Negative Threshold", new BlackOutColor(150, 0, 0, 255), "The color assigned to critical states (Low health, high ping).", this.dynamic::get);
    private final Setting<Boolean> showPops = this.sgGeneral.booleanSetting("Track Totem Pops", false, "Enables a column to track the number of Totems of Undying used by each player.");
    private final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgGeneral, "Header Color");

    private final List<Entity> players = new ArrayList<>();
    private float currentLongest = 0.0F;
    private float longest = 0.0F;
    private float currentLongestPing = 0.0F;
    private float longestPing = 0.0F;
    private float bgLength = 0.0F;
    private float y = 0.0F;

    public Playerlist() {
        super("Playerlist", "Provides a comprehensive real-time overview of nearby players, including skin icons, health, latency, and totem usage.");
        this.setSize(10.0F, 10.0F);
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
    }

    @Override
    public void render() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.stack.push();
            this.setSize(Math.max(this.bgLength, 10.0F), this.y + 6.0F);
            this.currentLongest = BlackOut.FONT.getWidth("PlayersHealthPing" + (this.showPops.get() ? "pops" : ""));
            this.currentLongestPing = BlackOut.FONT.getWidth("Ping");
            if (this.blur.get()) {
                RenderUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(0.0F, 0.0F, this.bgLength, this.y + 6.0F, 3.0F, 10));
                Renderer.onHUDBlur();
            }

            if (this.bg.get()) {
                this.background.render(this.stack, 0.0F, 0.0F, this.bgLength, this.y + 6.0F, 3.0F, 3.0F);
            }

            this.drawText(this.stack, 0.0F, 0.0F, "Players", "Health", "Ping", "Pops", true, 0.0F, 0.0F, 0.0F);
            this.y = 0.0F;
            this.players.forEach(player -> {
                AbstractClientPlayerEntity current = (AbstractClientPlayerEntity) player;
                String name = this.nameMode.get().getName(current);
                PlayerListEntry entry = BlackOut.mc.getNetworkHandler().getPlayerListEntry(current.getUuid());
                int pingValue = entry == null ? 0 : entry.getLatency();
                float healthValue = Math.round(current.getHealth() + current.getAbsorptionAmount());
                StatsManager.TrackerData trackerData = Managers.STATS.getStats(current);
                int popAmount = trackerData == null ? 0 : trackerData.pops;
                String ping = String.valueOf(pingValue);
                String health = String.valueOf(healthValue);
                String pops = String.valueOf(popAmount);
                if (BlackOut.FONT.getWidth(name + health + ping) > this.currentLongest) {
                    this.currentLongest = BlackOut.FONT.getWidth(name + health + ping);
                }

                if (BlackOut.FONT.getWidth(ping) > this.currentLongestPing) {
                    this.currentLongestPing = BlackOut.FONT.getWidth(ping);
                }

                this.drawFace(this.stack, 10.0F + this.y, current.getSkinTextures().texture());
                this.drawText(this.stack, 8.0F, 10.0F + this.y, name, health, ping, pops, false, healthValue, pingValue, popAmount);
                this.y += 10.0F;
            });
            this.longest = this.currentLongest;
            this.longestPing = this.currentLongestPing;
            this.stack.pop();
        }
    }

    private void drawText(
            MatrixStack stack,
            float x,
            float y,
            String string,
            String string2,
            String string3,
            String string4,
            Boolean first,
            float health,
            float ping,
            float totemPops
    ) {
        float drawX = first ? x + 8.0F : x;
        this.textColor.render(stack, string, 1.0F, x, y, false, false);
        if (this.dynamic.get() && !first) {
            BlackOut.FONT.text(stack, string2, 1.0F, drawX + this.longest, y, this.getHealthColor(health), false, false);
            BlackOut.FONT.text(stack, string3, 1.0F, drawX + this.longest + BlackOut.FONT.getWidth("Health") + 4.0F, y, this.getColor(ping), false, false);
            if (this.showPops.get()) {
                BlackOut.FONT
                        .text(
                                stack,
                                string4,
                                1.0F,
                                drawX + this.longest + BlackOut.FONT.getWidth("Health") + BlackOut.FONT.getWidth("Ping") + 4.0F,
                                y,
                                this.getColor(totemPops),
                                false,
                                false
                        );
            }
        } else {
            this.textColor.render(stack, string2, 1.0F, drawX + this.longest, y, false, false);
            this.textColor.render(stack, string3, 1.0F, drawX + this.longest + BlackOut.FONT.getWidth("Health") + 4.0F, y, false, false);
            if (this.showPops.get()) {
                this.textColor
                        .render(stack, string4, 1.0F, drawX + this.longest + BlackOut.FONT.getWidth("Health") + BlackOut.FONT.getWidth("Ping") + 8.0F, y, false, false);
            }
        }

        float txtWidth = this.showPops.get() ? BlackOut.FONT.getWidth("Health") + BlackOut.FONT.getWidth("Ping") + 6.0F : BlackOut.FONT.getWidth("Health");
        this.bgLength = this.longest + txtWidth + 12.0F + this.longestPing;
    }

    @Event
    public void onTickPost(TickEvent.Post event) {
        if (BlackOut.mc.world != null && BlackOut.mc.player != null) {
            this.players.clear();
            BlackOut.mc.world.entityList.forEach(entity -> {
                if (entity instanceof AbstractClientPlayerEntity && this.shouldRender(entity)) {
                    this.players.add(entity);
                }
            });
            this.players.sort(Comparator.comparing(entity -> this.nameMode.get().getName(entity)));
        }
    }

    public boolean shouldRender(Entity entity) {
        AntiBot antiBot = AntiBot.getInstance();
        return (!antiBot.enabled || antiBot.mode.get() != AntiBot.HandlingMode.Ignore || !(entity instanceof AbstractClientPlayerEntity player) || !antiBot.getBots().contains(player)) && entity != BlackOut.mc.player && entity instanceof AbstractClientPlayerEntity;
    }

    private void drawFace(MatrixStack stack, float y, Identifier renderSkin) {
        float size = 6.0F;
        if (renderSkin != null) {
            TextureRenderer.renderQuad(stack, 0.0F, y, size, size, 0.125F, 0.125F, 0.25F, 0.25F, BlackOut.mc.getTextureManager().getTexture(renderSkin).getGlId());
        }
    }

    private Color getHealthColor(float number) {
        return ColorUtils.lerpColor(Math.min(number, 1.0F), this.bad.get().getColor(), this.good.get().getColor());
    }

    private Color getColor(float number) {
        return ColorUtils.lerpColor(Math.min(number, 1.0F), this.good.get().getColor(), this.bad.get().getColor());
    }

    public enum NameMode {
        Display,
        EntityName;

        private String getName(Entity entity) {
            return switch (this) {
                case Display -> entity.getDisplayName().getString();
                default -> entity.getName().getString();
            };
        }
    }
}
