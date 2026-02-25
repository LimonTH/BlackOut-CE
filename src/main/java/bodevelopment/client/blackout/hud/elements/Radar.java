package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.combat.misc.AntiBot;
import bodevelopment.client.blackout.module.modules.visual.misc.FreeCam;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BackgroundMultiSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.RotationUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

public class Radar extends HudElement {
    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Integer> range = this.sgGeneral.intSetting("Detection Radius", 32, 0, 128, 1, "The maximum distance in blocks visualized on the radar display.");
    private final Setting<Style> style = this.sgGeneral.enumSetting("Visual Theme", Style.Blackout, "The aesthetic layout of the radar (Modern 'Blackout' or retro 'Exhibition').");
    private final Setting<Boolean> bg = this.sgGeneral.booleanSetting("Enable Backdrop", true, "Renders a background surface for the radar interface.", () -> this.style.get() == Style.Blackout);
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgGeneral, () -> this.bg.get() && this.style.get() == Style.Blackout, "Radar");
    private final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Gaussian Diffusion", true, "Applies a blur effect to the radar backdrop for improved clarity.", () -> this.style.get() == Style.Blackout);
    private final Setting<Boolean> fadeLines = this.sgGeneral.booleanSetting("Gradient Axis", false, "Uses a fading gradient for the radar's X and Y axis lines.");
    private final Setting<BlackOutColor> lineColor = this.sgGeneral.colorSetting("Axis Color", new BlackOutColor(255, 255, 255, 80), "The color palette for the crosshair axis lines.");
    private final Setting<BlackOutColor> enemyColor = this.sgGeneral.colorSetting("Hostile Marker", new BlackOutColor(255, 255, 255, 80), "The color assigned to non-allied player markers.");
    private final Setting<BlackOutColor> friendColor = this.sgGeneral.colorSetting("Ally Marker", new BlackOutColor(100, 100, 255, 180), "The color assigned to players identified in the friend list.");

    public Radar() {
        super("Radar", "Provides a 2D top-down spatial visualization of nearby players relative to your current orientation.");
        this.setSize(40.0F, 40.0F);
    }

    @Override
    public void render() {
        this.stack.push();
        switch (this.style.get()) {
            case Exhibition:
                this.setSize(42.0F, 42.0F);
                RenderUtils.drawSkeetBox(this.stack, -2.0F, -2.0F, 46.0F, 46.0F, true);
                if (this.fadeLines.get()) {
                    RenderUtils.fadeLine(this.stack, 0.0F, 21.0F, 42.0F, 21.0F, this.lineColor.get().getRGB());
                    RenderUtils.fadeLine(this.stack, 21.0F, 0.0F, 21.0F, 42.0F, this.lineColor.get().getRGB());
                } else {
                    RenderUtils.line(this.stack, 1.0F, 21.0F, 41.0F, 21.0F, this.lineColor.get().getRGB());
                    RenderUtils.line(this.stack, 21.0F, 1.0F, 21.0F, 41.0F, this.lineColor.get().getRGB());
                }
                break;
            case Blackout:
                this.setSize(40.0F, 40.0F);
                if (this.blur.get()) {
                    RenderUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(0.0F, 0.0F, 40.0F, 40.0F, 4.0F, 10));
                    Renderer.onHUDBlur();
                }

                if (this.bg.get()) {
                    this.background.render(this.stack, 0.0F, 0.0F, 40.0F, 40.0F, 4.0F, 4.0F);
                }

                if (this.fadeLines.get()) {
                    RenderUtils.fadeLine(this.stack, 0.0F, 20.0F, 40.0F, 20.0F, this.lineColor.get().getRGB());
                    RenderUtils.fadeLine(this.stack, 20.0F, 0.0F, 20.0F, 40.0F, this.lineColor.get().getRGB());
                } else {
                    RenderUtils.line(this.stack, 0.0F, 20.0F, 40.0F, 20.0F, this.lineColor.get().getRGB());
                    RenderUtils.line(this.stack, 20.0F, 0.0F, 20.0F, 40.0F, this.lineColor.get().getRGB());
                }
        }

        this.stack.translate(20.0F, 20.0F, 0.0F);

        for (PlayerEntity player : BlackOut.mc.world.getPlayers()) {
            if (player != BlackOut.mc.player && this.shouldRender(player)) {
                boolean isFriend = Managers.FRIENDS.isFriend(player);
                double dist = player.getPos().subtract(BlackOut.mc.player.getPos()).horizontalLength();
                double yaw = RotationUtils.getYaw(player.getPos());
                yaw = Math.toRadians(MathHelper.wrapDegrees(yaw - BlackOut.mc.player.getYaw() - 90.0));
                float x = (float) (Math.cos(yaw) * dist);
                float z = (float) (Math.sin(yaw) * dist);
                x /= this.range.get();
                if (!(Math.abs(x) >= 1.0F)) {
                    x *= 20.0F;
                    z /= this.range.get();
                    if (!(Math.abs(z) >= 1.0F)) {
                        z *= 20.0F;
                        this.renderEnemy(this.stack, x, z, isFriend);
                    }
                }
            }
        }

        this.stack.pop();
    }

    public void renderEnemy(MatrixStack stack, float x, float y, boolean friend) {
        if (this.style.get() == Style.Exhibition) {
            RenderUtils.quad(stack, x - 1.0F, y - 1.0F, 3.0F, 3.0F, Color.BLACK.getRGB());
            RenderUtils.quad(stack, x, y, 1.0F, 1.0F, friend ? Color.YELLOW.getRGB() : Color.RED.getRGB());
        } else {
            RenderUtils.rounded(
                    stack, x, y, 0.0F, 0.0F, 1.0F, 0.0F, friend ? this.friendColor.get().getRGB() : this.enemyColor.get().getRGB(), ColorUtils.SHADOW100I
            );
        }
    }

    public boolean shouldRender(Entity entity) {
        AntiBot antiBot = AntiBot.getInstance();
        if (antiBot.enabled && antiBot.mode.get() == AntiBot.HandlingMode.Ignore && entity instanceof AbstractClientPlayerEntity player && antiBot.getBots().contains(player)) {
            return false;
        } else {
            return entity != BlackOut.mc.player || FreeCam.getInstance().enabled;
        }
    }

    public enum Style {
        Blackout,
        Exhibition
    }
}
