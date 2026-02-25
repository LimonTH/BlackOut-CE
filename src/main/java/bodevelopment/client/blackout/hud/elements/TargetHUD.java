package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.manager.managers.StatsManager;
import bodevelopment.client.blackout.module.modules.combat.offensive.Aura;
import bodevelopment.client.blackout.module.modules.combat.offensive.AutoCrystal;
import bodevelopment.client.blackout.module.modules.combat.offensive.BedAura;
import bodevelopment.client.blackout.module.modules.combat.offensive.PistonCrystal;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BackgroundMultiSetting;
import bodevelopment.client.blackout.module.setting.multisettings.RoundedColorMultiSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.rendering.renderer.TextureRenderer;
import bodevelopment.client.blackout.util.RotationUtils;
import bodevelopment.client.blackout.util.render.AnimUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.awt.*;

public class TargetHUD extends HudElement {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgColor = this.addGroup("Color");

    public final Setting<Mode> mode = this.sgGeneral.enumSetting("Visual Interface", Mode.Blackout, "The aesthetic layout and theme used for target visualization.");
    public final Setting<ArmorCount> countMode = this.sgGeneral.enumSetting("Durability Logic", ArmorCount.Average, "Determines whether to display the average armor integrity or highlight the weakest piece.", () -> this.mode.get() == Mode.BlackoutNew);
    private final Setting<Boolean> hp = this.sgGeneral.booleanSetting("Value Readout", false, "Renders numeric health values alongside the health bar.", () -> this.mode.get() == Mode.Blackout);
    private final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Gaussian Diffusion", true, "Applies a real-time blur effect to the background panel for depth.", () -> this.mode.get() != Mode.Exhibition);
    private final Setting<Boolean> shadow = this.sgGeneral.booleanSetting("Structural Shadow", true, "Adds a subtle shadow around the element to simulate elevation.", () -> this.mode.get() != Mode.Exhibition);
    private final Setting<TargetMode> targetMode = this.sgGeneral.enumSetting("Acquisition Logic", TargetMode.ModuleTarget, "Determines how the element selects which player to track.");
    private final Setting<Double> targetRange = this.sgGeneral.doubleSetting("Scanning Radius", 20.0, 0.0, 200.0, 2.0, "The maximum distance to search for a target when using Closest mode.", () -> this.targetMode.get() == TargetMode.Closest);
    private final Setting<RenderType> renderType = this.sgGeneral.enumSetting("Projection Type", RenderType.Hud, "Determines if the HUD is pinned to the screen or projected into the 3D world near the player.");
    private final Setting<Double> renderHeight = this.sgGeneral.doubleSetting("Vertical Offset", 0.75, 0.0, 1.0, 0.05, "The height relative to the target's hitbox where the HUD is rendered in 3D mode.", () -> this.renderType.get() == RenderType.Player);
    private final Setting<Double> dist = this.sgGeneral.doubleSetting("Depth Offset", 0.25, 0.0, 1.0, 0.05, "The distance from the target's center to project the HUD in 3D space.", () -> this.renderType.get() == RenderType.Player);

    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgColor, () -> this.mode.get() != Mode.ExhibitionNew, null);
    private final Setting<BlackOutColor> secondaryColor = this.sgColor.colorSetting("Accent Palette", new BlackOutColor(220, 60, 90, 255), "A secondary color used for specific thematic elements.", () -> this.mode.get() == Mode.Arsenic);
    private final RoundedColorMultiSetting armorBar = RoundedColorMultiSetting.of(this.sgColor, () -> this.mode.get() == Mode.BlackoutNew, "Armor Bar");
    private final Setting<BlackOutColor> textColor = this.sgColor.colorSetting("Text Palette", new BlackOutColor(255, 255, 255, 255), "The primary color for names and data readouts.");
    private final RoundedColorMultiSetting healthBar = RoundedColorMultiSetting.of(this.sgColor, "Health Bar");

    private float delta = 0.0F;
    private float progress = 0.0F;
    private float armorProgress = 0.0F;
    private AbstractClientPlayerEntity target = null;
    private AbstractClientPlayerEntity renderTarget = null;
    private Vec3d renderPos = Vec3d.ZERO;
    private Identifier renderSkin;

    public TargetHUD() {
        super("Target HUD", "Displays an advanced status overlay for the current combat target, featuring health, armor, and player head renders.");
        this.setSize(10.0F, 10.0F);
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
    }

    @Override
    public void render() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.updateTarget();
            if (this.target != null) {
                this.setRendering(this.target);
            }

            if (this.renderTarget == null) {
                this.setRendering(BlackOut.mc.player);
            }

            this.setSize(this.getRenderWidth(), this.getRenderHeight());
            if (this.renderType.get() == RenderType.Hud) {
                this.renderTargetHUD(false);
            }
        }
    }

    @Override
    public void onRemove() {
        BlackOut.EVENT_BUS.unsubscribe(this);
    }

    @Event
    public void onRender(RenderEvent.Hud.Post event) {
        if (BlackOut.mc.world != null && BlackOut.mc.player != null) {
            if (this.renderType.get() != RenderType.Hud && this.renderTarget != null) {
                this.stack.push();
                RenderUtils.unGuiScale(this.stack);
                double yaw = Math.toRadians(RotationUtils.getYaw(this.renderPos, BlackOut.mc.gameRenderer.getCamera().getPos(), 0.0));
                Vec2f f = RenderUtils.getCoords(
                        this.renderPos.x + Math.cos(yaw) * this.dist.get(),
                        this.renderPos.y + this.renderTarget.getBoundingBox().getLengthY() * this.renderHeight.get(),
                        this.renderPos.z + Math.sin(yaw) * this.dist.get(),
                        true
                );
                if (f == null) {
                    this.stack.pop();
                } else {
                    this.stack.translate(f.x, f.y, 0.0F);
                    this.stack.scale(this.getScale() * 2.0F, this.getScale() * 2.0F, 0.0F);
                    this.renderTargetHUD(true);
                    this.stack.pop();
                }
            }
        }
    }

    private void setRendering(AbstractClientPlayerEntity player) {
        this.renderTarget = player;
        this.renderPos = new Vec3d(
                MathHelper.lerp(BlackOut.mc.getRenderTickCounter().getTickDelta(true), this.renderTarget.prevX, this.renderTarget.getX()),
                MathHelper.lerp(BlackOut.mc.getRenderTickCounter().getTickDelta(true), this.renderTarget.prevY, this.renderTarget.getY()),
                MathHelper.lerp(BlackOut.mc.getRenderTickCounter().getTickDelta(true), this.renderTarget.prevZ, this.renderTarget.getZ())
        );
    }

    private void renderTargetHUD(boolean center) {
        if (this.target != null) {
            this.delta = Math.min(this.delta + this.frameTime, 1.0F);
        } else {
            this.delta = Math.max(this.delta - this.frameTime, 0.0F);
        }

        float health = this.renderTarget.getHealth();
        float nameScale = this.renderTarget.getName().getString().length() >= 12 ? 0.7F : 0.9F;
        float renderHealth = this.renderTarget.getHealth() + this.renderTarget.getAbsorptionAmount();
        float renderScale = (float) AnimUtils.easeOutQuart(this.delta);
        // TODO: color integration
        // float colorHealth = Math.min((this.renderTarget.getHealth() + this.renderTarget.getAbsorptionAmount()) / this.renderTarget.getMaxHealth(), 1.0F);
        float targetProgress = Math.min(health / 20.0F, 1.0F);
        float progressDelta = this.frameTime + this.frameTime * Math.abs(targetProgress - this.progress);
        if (targetProgress > this.progress) {
            this.progress = Math.min(this.progress + progressDelta, targetProgress);
        } else {
            this.progress = Math.max(this.progress - progressDelta, targetProgress);
        }

        float armorTargetProgress = Math.min(this.getDurability(this.renderTarget), 1.0F);
        float armorProgressDelta = this.frameTime + this.frameTime * Math.abs(armorTargetProgress - this.armorProgress);
        if (armorTargetProgress > this.armorProgress) {
            this.armorProgress = Math.min(this.armorProgress + armorProgressDelta, armorTargetProgress);
        } else {
            this.armorProgress = Math.max(this.armorProgress - armorProgressDelta, armorTargetProgress);
        }

        Color yes = new Color(0, 0, 0, 85);
        this.stack.push();
        this.stack.translate(this.getRenderWidth() / 2.0F, center ? 0.0F : this.getRenderHeight() / 2.0F, 0.0F);
        this.stack.scale(renderScale, renderScale, 1.0F);
        float prevAlpha = Renderer.getAlpha();
        Renderer.setAlpha(renderScale);
        this.stack.push();
        this.stack.translate(this.getRenderWidth() / -2.0F, this.getRenderHeight() / -2.0F, 0.0F);

        float width;
        float x;
        String healthPercent;

        switch (this.mode.get()) {
            case Blackout:
                if (this.blur.get()) {
                    RenderUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(0.0F, 0.0F, 105.0F, 20.0F, 3.0F, 10));
                    Renderer.onHUDBlur();
                }

                this.background.render(this.stack, 0.0F, 0.0F, 105.0F, 20.0F, 3.0F, 3.0F);
                BlackOut.FONT.text(this.stack, this.renderTarget.getName().getString(), nameScale, 27.0F, 1.0F, this.textColor.get().getColor(), false, false);
                this.healthBar.render(this.stack, 27.0F, 15.0F, 70.0F * this.progress, 1.0F, 2.0F, 3.0F);
                width = BlackOut.FONT.getWidth("HP: " + Math.round(renderHealth)) * 0.6F;
                x = 27.0F + 70.0F * this.progress - width;
                float textX = Math.max(x, 27.0F);
                if (this.hp.get()) {
                    BlackOut.FONT.text(this.stack, "HP: " + Math.round(renderHealth), 0.6F, textX, 8.0F, this.textColor.get().getColor(), false, false);
                }

                this.drawFace(this.stack, 1.1F, -1.0F, -1.0F);
                break;
            case ExhibitionNew:
                RenderUtils.rounded(
                        this.stack,
                        0.0F,
                        0.0F,
                        105.0F,
                        28.0F,
                        3.0F,
                        this.shadow.get() ? 5.0F : 0.0F,
                        new Color(25, 25, 25, 255).getRGB(),
                        new Color(0, 0, 0, 100).getRGB()
                );
                BlackOut.FONT
                        .text(
                                this.stack,
                                this.renderTarget.getName().getString() + " HP: " + Math.round(renderHealth),
                                0.7F,
                                27.0F,
                                1.0F,
                                this.textColor.get().getColor(),
                                false,
                                false
                        );
                RenderUtils.rounded(this.stack, 28.0F, 11.0F, 72.0F * this.progress, 1.0F, 2.0F, 0.0F, new Color(255, 202, 24, 255).getRGB(), 0);
                RenderUtils.rounded(this.stack, 28.0F, 11.0F, 72.0F * this.progress, 1.0F, 1.0F, 0.0F, new Color(255, 242, 0, 255).getRGB(), 0);
                RenderUtils.rounded(this.stack, 6.0F, 8.0F, 12.0F, 12.0F, 5.0F, 0.0F, new Color(45, 45, 45, 255).getRGB(), 0);
                this.drawArmor(this.stack, this.renderTarget, 32.0F, 19.0F);
                this.drawFace(this.stack, 1.1F, 1.0F, 3.0F);
                break;

            case BlackoutInfo:
                String ping = "0";
                PlayerListEntry entry = BlackOut.mc.getNetworkHandler().getPlayerListEntry(this.renderTarget.getUuid());
                if (entry != null) {
                    ping = String.valueOf(entry.getLatency());
                }

                boolean naked = !this.getArmor(this.renderTarget);
                StatsManager.TrackerData trackerData = Managers.STATS.getStats(this.renderTarget);
                int popAmount = trackerData == null ? 0 : trackerData.pops;
                String info = "HP: " + Math.round(renderHealth) + " Ping: " + ping + "ms Pops: " + popAmount;
                if (this.blur.get()) {
                    RenderUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(0.0F, 0.0F, 120.0F, 50.0F, 3.0F, 10));
                    Renderer.onHUDBlur();
                }

                this.background.render(this.stack, 0.0F, 0.0F, 120.0F, 50.0F, 3.0F, 3.0F);
                BlackOut.FONT.text(this.stack, this.renderTarget.getName().getString(), 1.0F, 60.0F, 5.0F, this.textColor.get().getColor(), true, true);
                RenderUtils.quad(this.stack, 5.0F, 3.0F + BlackOut.FONT.getHeight(), 110.0F, 1.0F, new Color(0, 0, 0, 100).getRGB());
                this.healthBar.render(this.stack, 5.0F, 3.0F + BlackOut.FONT.getHeight(), 110.0F * this.progress, 1.0F, 0.0F, 0.0F);
                BlackOut.FONT.text(this.stack, info, 0.8F, 60.0F, 12.0F + BlackOut.FONT.getHeight(), this.textColor.get().getColor(), true, true);
                RenderUtils.rounded(
                        this.stack, 20.0F, 24.0F + BlackOut.FONT.getHeight(), 80.0F, 10.0F, 3.0F, 3.0F, new Color(0, 0, 0, 80).getRGB(), new Color(0, 0, 0, 40).getRGB()
                );
                if (naked) {
                    BlackOut.FONT.text(this.stack, "Naked!", 0.8F, 60.0F, 30.0F + BlackOut.FONT.getHeight(), this.textColor.get().getColor(), true, true);
                } else {
                    this.drawArmor(this.stack, this.renderTarget, 20.0F, 24.0F + BlackOut.FONT.getHeight());
                }
                break;

            case Old:
                if (this.blur.get()) {
                    RenderUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(0.0F, 0.0F, 108.0F, 24.0F, 0.0F, 10));
                    Renderer.onHUDBlur();
                }

                this.background.render(this.stack, 0.0F, 0.0F, 108.0F, 24.0F, 0.0F, 3.0F);
                this.healthBar.render(this.stack, 25.0F, 14.0F, 4.0F * health, 8.0F, 0.0F, 0.0F);
                BlackOut.FONT.text(this.stack, this.renderTarget.getName().getString(), 1.0F, 25.0F, 4.0F, Color.WHITE, false, false);
                BlackOut.FONT.text(this.stack, "HP: " + Math.round(renderHealth), 0.8F, 65.0F, 18.0F, Color.WHITE, true, true);
                this.drawFace(this.stack, 1.0F, 2.0F, 2.0F);
                break;

            case Tenacity:
                if (this.blur.get()) {
                    RenderUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(0.0F, 0.0F, 115.0F, 26.0F, 6.0F, 10));
                    Renderer.onHUDBlur();
                }

                this.background.render(this.stack, 0.0F, 0.0F, 115.0F, 26.0F, 6.0F, 3.0F);
                BlackOut.FONT.text(this.stack, this.renderTarget.getName().getString(), 1.0F, 70.0F, 0.0F, this.textColor.get().getColor(), true, false);
                RenderUtils.rounded(this.stack, 32.0F, 25.0F, 80.0F * this.progress, 0.2F, 1.0F, 0.0F, yes.getRGB(), yes.getRGB());
                this.healthBar.render(this.stack, 32.0F, 25.0F, 80.0F * this.progress, 0.2F, 1.0F, 0.0F);
                healthPercent = Math.round(this.renderTarget.getHealth() / this.renderTarget.getMaxHealth() * 100.0F) + "%";
                BlackOut.FONT
                        .text(
                                this.stack,
                                healthPercent,
                                0.8F,
                                32.0F + 80.0F * this.progress - BlackOut.FONT.getWidth(healthPercent) * 0.8F,
                                17.0F,
                                this.textColor.get().getColor(),
                                false,
                                false
                        );
                this.drawFace(this.stack, 1.2F, 0.0F, 1.0F);
                break;

            case Tenacity2:
                if (this.blur.get()) {
                    RenderUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(0.0F, 0.0F, 100.0F, 26.0F, 6.0F, 10));
                    Renderer.onHUDBlur();
                }

                this.background.render(this.stack, 0.0F, 0.0F, 100.0F, 26.0F, 6.0F, 3.0F);
                BlackOut.FONT
                        .text(
                                this.stack,
                                this.renderTarget.getName().getString(),
                                1.0F,
                                65.0F,
                                32.0F - BlackOut.FONT.getHeight() * 4.0F,
                                this.textColor.get().getColor(),
                                true,
                                false
                        );
                RenderUtils.rounded(this.stack, 31.0F, 13.2F, 68.0F, 0.4F, 1.2F, 0.0F, yes.getRGB(), yes.getRGB());
                this.healthBar.render(this.stack, 31.0F, 13.2F, 68.0F * this.progress, 0.4F, 1.2F, 0.0F);
                healthPercent = Math.round(this.renderTarget.getHealth() / this.renderTarget.getMaxHealth() * 100.0F) + "%";
                String dist = Math.round(this.renderTarget.distanceTo(BlackOut.mc.player)) + "m";
                BlackOut.FONT
                        .text(this.stack, healthPercent + " " + dist, 0.8F, 65.0F, BlackOut.FONT.getHeight() * 2.5F, this.textColor.get().getColor(), true, false);
                this.drawFace(this.stack, 1.2F, 0.0F, 1.0F);
                break;

            case BlackoutNew:
                if (this.blur.get()) {
                    RenderUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(0.0F, 0.0F, 100.0F, 20.0F, 3.0F, 10));
                    Renderer.onHUDBlur();
                }

                this.background.render(this.stack, 0.0F, 0.0F, 100.0F, 20.0F, 3.0F, 3.0F);
                BlackOut.FONT.text(this.stack, this.renderTarget.getName().getString(), 0.75F, 27.0F, 1.0F, this.textColor.get().getColor(), false, false);
                RenderUtils.rounded(this.stack, 27.0F, 11.0F, 70.0F, 0.1F, 1.0F, 0.0F, new Color(0, 0, 0, 100).getRGB(), new Color(0, 0, 0, 100).getRGB());
                this.healthBar.render(this.stack, 27.0F, 11.0F, 70.0F * this.progress, 0.1F, 1.0F, 1.0F);
                RenderUtils.rounded(this.stack, 27.0F, 18.0F, 70.0F, 0.1F, 1.0F, 0.0F, new Color(0, 0, 0, 100).getRGB(), new Color(0, 0, 0, 100).getRGB());
                this.armorBar.render(this.stack, 27.0F, 18.0F, 70.0F * this.getDurability(this.renderTarget), 0.1F, 1.0F, 1.0F);
                String txt = "HP: " + Math.round(renderHealth);
                x = 99.0F - BlackOut.FONT.getWidth(txt) * 0.75F;
                BlackOut.FONT.text(this.stack, txt, 0.75F, x, 1.0F, new Color(150, 150, 150, 255), false, false);
                this.drawFace(this.stack, 1.1F, -1.0F, -1.0F);
                break;

            case Arsenic:
                String name = "Name: " + this.renderTarget.getName().getString();
                String hp = "HP: " + Math.round(renderHealth);
                width = Math.max(BlackOut.FONT.getWidth(name), BlackOut.FONT.getWidth(hp));
                float height = BlackOut.FONT.getHeight() * 2.0F;
                if (this.blur.get()) {
                    RenderUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(0.0F, 0.0F, width, height, 0.0F, 10));
                    Renderer.onHUDBlur();
                }

                this.background.render(this.stack, 0.0F, 0.0F, width, height, 0.0F, 3.0F);
                this.healthBar.render(this.stack, 0.0F, -1.0F, width * this.progress, 1.0F, 0.0F, 0.0F);
                BlackOut.FONT.text(this.stack, "Name: ", 1.0F, 0.0F, 1.0F, this.secondaryColor.get().getColor(), false, false);
                BlackOut.FONT.text(this.stack, "HP: ", 1.0F, 0.0F, BlackOut.FONT.getHeight() + 1.0F, this.secondaryColor.get().getColor(), false, false);
                BlackOut.FONT
                        .text(
                                this.stack,
                                this.renderTarget.getName().getString(),
                                1.0F,
                                BlackOut.FONT.getWidth("Name: "),
                                1.0F,
                                this.textColor.get().getColor(),
                                false,
                                false
                        );
                BlackOut.FONT
                        .text(
                                this.stack,
                                String.valueOf(Math.round(renderHealth)),
                                1.0F,
                                BlackOut.FONT.getWidth("HP: "),
                                BlackOut.FONT.getHeight() + 1.0F,
                                this.textColor.get().getColor(),
                                false,
                                false
                        );
                break;

            case Exhibition:
                RenderUtils.quad(this.stack, 0.0F, 0.0F, 114.0F, 32.0F, new Color(0, 0, 0, 150).getRGB());
                RenderUtils.quad(this.stack, 1.0F, 1.0F, 30.0F, 30.0F, new Color(200, 200, 200, 255).getRGB());
                this.drawFace(this.stack, 1.4F, 2.0F, 2.0F);
                BlackOut.FONT.text(this.stack, this.renderTarget.getName().getString(), 1.0F, 34.0F, 2.0F, this.textColor.get().getRGB(), false, false);
                RenderUtils.quad(this.stack, 34.0F, 3.0F + BlackOut.FONT.getHeight(), 3.7F * health, 3.0F, new Color(220, 220, 0, 255).getRGB());
                RenderUtils.quad(this.stack, 34.0F, 2.5F + BlackOut.FONT.getHeight(), 74.0F, 0.5F, Color.BLACK.getRGB());
                RenderUtils.quad(this.stack, 34.0F, 6.0F + BlackOut.FONT.getHeight(), 74.0F, 0.5F, Color.BLACK.getRGB());

                for (int i = 0; i < 11; i++) {
                    RenderUtils.quad(this.stack, (float) (33.5 + 7.4 * i), 2.5F + BlackOut.FONT.getHeight(), 0.5F, 4.0F, Color.BLACK.getRGB());
                }

                BlackOut.FONT
                        .text(
                                this.stack,
                                "HP: " + Math.round(renderHealth) + ": Dist: " + Math.round(BlackOut.mc.player.distanceTo(this.renderTarget)),
                                0.6F,
                                34.0F,
                                16.0F,
                                this.textColor.get().getRGB(),
                                false,
                                false
                        );
                BlackOut.FONT
                        .text(
                                this.stack,
                                "Yaw: "
                                        + Math.round(this.renderTarget.getYaw())
                                        + " Pitch: "
                                        + Math.round(this.renderTarget.getPitch())
                                        + " BodyYaw: "
                                        + Math.round(this.renderTarget.bodyYaw),
                                0.6F,
                                34.0F,
                                16.0F + BlackOut.FONT.getHeight() * 0.6F,
                                this.textColor.get().getRGB(),
                                false,
                                false
                        );
                BlackOut.FONT
                        .text(
                                this.stack,
                                "TOG: 0 HURT: " + this.renderTarget.hurtTime + " TE: " + this.renderTarget.age,
                                0.6F,
                                34.0F,
                                16.0F + BlackOut.FONT.getHeight() * 1.2F,
                                this.textColor.get().getRGB(),
                                false,
                                false
                        );
        }

        Renderer.setAlpha(prevAlpha);
        this.stack.pop();
    }

    private float getRenderWidth() {
        return switch (this.mode.get()) {
            case Blackout, ExhibitionNew -> 105.0F;
            case BlackoutInfo -> 120.0F;
            case Old -> 108.0F;
            case Tenacity -> 115.0F;
            case Tenacity2, BlackoutNew -> 100.0F;
            case Arsenic -> 50.0F;
            case Exhibition -> 114.0F;
        };
    }

    private float getRenderHeight() {
        return switch (this.mode.get()) {
            case Blackout, BlackoutNew -> 20.0F;
            case ExhibitionNew -> 28.0F;
            case BlackoutInfo -> 50.0F;
            case Old -> 24.0F;
            case Tenacity, Tenacity2 -> 26.0F;
            case Arsenic -> 18.0F;
            case Exhibition -> 32.0F;
        };
    }

    private void updateTarget() {
        this.target = null;
        switch (this.targetMode.get()) {
            case ModuleTarget:
                this.moduleTarget();
                break;
            case Closest:
                this.closestTarget();
        }

        if (this.target != null) {
            this.renderSkin = this.target.getSkinTextures().texture();
        }
    }

    private void moduleTarget() {
        if (!AutoCrystal.getInstance().enabled || (this.target = AutoCrystal.getInstance().targetedPlayer) == null) {
            if (!Aura.getInstance().enabled || (this.target = Aura.targetedPlayer) == null) {
                if (!BedAura.getInstance().enabled || (this.target = BedAura.targetedPlayer) == null) {
                    if (PistonCrystal.getInstance().enabled) {
                        this.target = PistonCrystal.targetedPlayer;
                    }
                }
            }
        }
    }

    private void closestTarget() {
        double distance = Double.MAX_VALUE;

        for (AbstractClientPlayerEntity player : BlackOut.mc.world.getPlayers()) {
            if (player != BlackOut.mc.player && !Managers.FRIENDS.isFriend(player) && !(player.distanceTo(BlackOut.mc.player) > this.targetRange.get())) {
                double d = BlackOut.mc.player.distanceTo(player);
                if (d < distance) {
                    this.target = player;
                    distance = d;
                }
            }
        }
    }

    private void drawFace(MatrixStack stack, float scale, float x, float y) {
        float size = scale * 20.0F;
        if (this.renderSkin != null) {
            if (this.mode.get() == Mode.Old || this.mode.get() == Mode.Exhibition) {
                TextureRenderer.renderQuad(
                        stack, x, y, size, size, 0.125F, 0.125F, 0.25F, 0.25F, BlackOut.mc.getTextureManager().getTexture(this.renderSkin).getGlId()
                );
            }

            if (this.mode.get() != Mode.Tenacity && this.mode.get() != Mode.Tenacity2) {
                TextureRenderer.renderFitRounded(
                        stack, x, y, size, size, 0.125F, 0.125F, 0.25F, 0.25F, 5.0F, 40, BlackOut.mc.getTextureManager().getTexture(this.renderSkin).getGlId()
                );
            } else {
                TextureRenderer.renderFitRounded(
                        stack, x, y, size, size, 0.125F, 0.125F, 0.25F, 0.25F, 12.0F, 40, BlackOut.mc.getTextureManager().getTexture(this.renderSkin).getGlId()
                );
            }
        }
    }

    private void drawArmor(MatrixStack stack, PlayerEntity player, float x, float y) {
        switch (this.mode.get()) {
            case ExhibitionNew:
                for (int ix = 0; ix < 4; ix++) {
                    ItemStack itemStack = player.getInventory().armor.get(3 - ix);
                    RenderUtils.rounded(stack, x + ix * 18, y, 8.0F, 8.0F, 2.0F, 0.0F, new Color(45, 45, 45, 255).getRGB(), 0);
                    RenderUtils.rounded(stack, x + ix * 18 + 1.0F, y + 1.0F, 6.0F, 6.0F, 2.0F, 0.0F, new Color(25, 25, 25, 255).getRGB(), 0);
                    if (!itemStack.isEmpty()) {
                        RenderUtils.renderItem(stack, itemStack, x - 4.0F + ix * 18, y - 4.0F, 10.0F, 500.0F, true);
                    }
                }
                break;
            case BlackoutInfo:
                for (int i = 0; i < 4; i++) {
                    ItemStack itemStack = player.getInventory().armor.get(3 - i);
                    if (!itemStack.isEmpty()) {
                        RenderUtils.renderItem(stack, itemStack, x + i * 20, y - 3.0F, 16.0F, 500.0F, true);
                    }
                }
        }
    }

    private boolean getArmor(PlayerEntity entity) {
        for (int i = 0; i < 4; i++) {
            if (!entity.getInventory().getArmorStack(i).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private float getDurability(PlayerEntity entity) {
        float durability = 0.0F;
        int armors = 0;
        float lowest = 1.0F;

        for (int i = 0; i < 4; i++) {
            if (entity.getInventory().getArmorStack(i).isEmpty()) {
                lowest = 0.0F;
            } else {
                ItemStack itemStack = entity.getInventory().getArmorStack(i);
                armors++;
                switch (this.countMode.get()) {
                    case Average:
                        durability += Math.round((float) ((itemStack.getMaxDamage() - itemStack.getDamage()) * 100) / itemStack.getMaxDamage());
                        return durability / 100.0F / armors;
                    case Lowest:
                        durability = Math.round((float) ((itemStack.getMaxDamage() - itemStack.getDamage()) * 100) / itemStack.getMaxDamage()) / 100.0F;
                        if (durability < lowest) {
                            lowest = durability;
                        }
                }
            }
        }

        return lowest;
    }

    public enum ArmorCount {
        Average,
        Lowest
    }

    // TODO: ColorMode нигде не используется
    public enum ColorMode {
        Dynamic,
        Rainbow,
        Custom,
        Wave
    }

    public enum Mode {
        Old,
        Blackout,
        BlackoutInfo,
        ExhibitionNew,
        Tenacity,
        Tenacity2,
        BlackoutNew,
        Arsenic,
        Exhibition
    }

    public enum RenderType {
        Hud,
        Player
    }

    public enum TargetMode {
        ModuleTarget,
        Closest
    }
}
