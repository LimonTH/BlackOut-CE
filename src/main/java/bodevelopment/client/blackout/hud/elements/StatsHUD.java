package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.Stats;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.manager.managers.StatsManager;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BackgroundMultiSetting;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.util.function.Predicate;

public class StatsHUD extends HudElement {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgData = this.addGroup("Data");

    private final Setting<TargetMode> targetMode = this.sgGeneral.enumSetting("Target Focus", TargetMode.Enemy, "The criteria used to select which player's statistics are displayed.");
    private final Setting<Boolean> bg = this.sgGeneral.booleanSetting("Enable Backdrop", true, "Renders a background panel behind the player metadata.");
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgGeneral, this.bg::get, null);
    private final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Gaussian Diffusion", true, "Applies a real-time blur effect to the background for enhanced UI contrast.");
    private final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgGeneral, "Label Color");

    private final Setting<Boolean> hole = this.sgData.booleanSetting("Hole Chronology", true, "Displays the total duration the target has occupied a hole.");
    private final Setting<Boolean> phased = this.sgData.booleanSetting("Phase Tracking", true, "Displays the total duration the target has been in a phased or clipped state.");
    private final Setting<Boolean> pops = this.sgData.booleanSetting("Totem Registry", true, "Tracks the total number of Totems of Undying consumed by the target.");
    private final Setting<Boolean> eaten = this.sgData.booleanSetting("Consumption Tracker", true, "Displays the amount of food or Golden Apples consumed.");
    private final Setting<Boolean> bottles = this.sgData.booleanSetting("Expended Bottles", true, "Tracks the number of Experience Bottles thrown by the target.");
    private final Setting<Boolean> moved = this.sgData.booleanSetting("Spatical Displacement", true, "Tracks the total distance in blocks the target has traveled.");
    private final Setting<Boolean> damage = this.sgData.booleanSetting("Accrued Damage", true, "Displays the total amount of health lost during the current tracking session.");

    public StatsHUD() {
        super("Stats", "Displays comprehensive real-time combat and movement metadata for the selected target.");
        this.setSize(50.0F, 50.0F);
        BlackOut.EVENT_BUS.subscribe(this, () -> false);
    }

    @Override
    public void render() {
        AbstractClientPlayerEntity target = this.getTarget();
        if (target != null) {
            StatsManager.TrackerData data = Managers.STATS.getStats(target);
            if (data != null) {
                int statCount = this.statCount();
                this.stack.push();
                this.setSize(
                        Math.max(50.0F, BlackOut.FONT.getWidth(target.getGameProfile().getName()) * 1.5F + 20.0F),
                        BlackOut.FONT.getHeight() * 1.5F + statCount * BlackOut.FONT.getHeight() + 10.0F
                );
                if (this.blur.get()) {
                    RenderUtils.drawLoadedBlur(
                            "hudblur",
                            this.stack,
                            renderer -> renderer.rounded(0.0F, 0.0F, this.getWidth() / this.getScale(), this.getHeight() / this.getScale(), 3.0F, 10)
                    );
                    Renderer.onHUDBlur();
                }

                if (this.bg.get()) {
                    this.background.render(this.stack, 0.0F, 0.0F, this.getWidth() / this.getScale(), this.getHeight() / this.getScale(), 3.0F, 3.0F);
                }

                this.textColor.render(this.stack, target.getGameProfile().getName(), 1.5F, this.getWidth() / 2.0F / this.getScale(), 0.0F, true, false);
                this.stack.translate(0.0, BlackOut.FONT.getHeight() * 1.5 + 10.0, 0.0);

                for (Stats stat : Stats.values()) {
                    if (this.shouldRender(stat)) {
                        this.textColor.render(this.stack, this.getStat(stat, data), 1.0F, 0.0F, 0.0F, false, true);
                        this.stack.translate(0.0F, BlackOut.FONT.getHeight(), 0.0F);
                    }
                }

                this.stack.pop();
            }
        }
    }

    private String getStat(Stats stat, StatsManager.TrackerData data) {
        return switch (stat) {
            case Hole -> "In Hole: " + OLEPOSSUtils.getTimeString(data.inHoleFor * 50L);
            case Phased -> "Phased: " + OLEPOSSUtils.getTimeString(data.phasedFor * 50L);
            case Pops -> "Pops: " + data.pops;
            case Eaten -> "Eaten: " + data.eaten;
            case Bottles -> "Bottles: " + data.bottles;
            case Moved -> "Moved: " + data.blocksMoved;
            case Damage -> String.format("Damage: %.1f", data.damage);
        };
    }

    private boolean shouldRender(Stats stat) {
        return (switch (stat) {
            case Hole -> this.hole;
            case Phased -> this.phased;
            case Pops -> this.pops;
            case Eaten -> this.eaten;
            case Bottles -> this.bottles;
            case Moved -> this.moved;
            case Damage -> this.damage;
        }).get();
    }

    private int statCount() {
        int stats = 0;

        for (Stats stat : Stats.values()) {
            if (this.shouldRender(stat)) {
                stats++;
            }
        }

        return stats;
    }

    private AbstractClientPlayerEntity getTarget() {
        return switch (this.targetMode.get()) {
            case Enemy -> this.getClosest(player -> player != BlackOut.mc.player && !Managers.FRIENDS.isFriend(player));
            case Friend -> this.getClosest(Managers.FRIENDS::isFriend);
            case Own -> BlackOut.mc.player;
        };
    }

    private AbstractClientPlayerEntity getClosest(Predicate<AbstractClientPlayerEntity> predicate) {
        double closestDist = 0.0;
        AbstractClientPlayerEntity closest = null;

        for (AbstractClientPlayerEntity player : BlackOut.mc.world.getPlayers()) {
            double d = BlackOut.mc.player.distanceTo(player);
            if (predicate.test(player) && (closest == null || !(d > closestDist))) {
                closest = player;
                closestDist = d;
            }
        }

        return closest;
    }

    public enum TargetMode {
        Enemy,
        Friend,
        Own
    }
}
