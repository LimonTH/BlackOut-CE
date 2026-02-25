package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.util.Hand;

public class SwingModifier extends Module {
    private static SwingModifier INSTANCE;

    private final SettingGroup sgMainHand = this.addGroup("Main Hand");
    private final SettingGroup sgOffHand = this.addGroup("Off Hand");

    private final Setting<Double> mainSpeed = this.sgMainHand.doubleSetting("Swing Velocity", 1.0, 0.0, 10.0, 0.05, "The temporal speed multiplier for the main-hand animation cycle.");
    private final Setting<Double> mainStart = this.sgMainHand.doubleSetting("Initial Frame", 0.0, 0.0, 10.0, 0.05, "The normalized animation progress at which the main-hand swing begins.");
    private final Setting<Double> mainEnd = this.sgMainHand.doubleSetting("Terminal Frame", 1.0, 0.0, 10.0, 0.05, "The normalized animation progress at which the main-hand swing concludes.");
    private final Setting<Double> mainStartY = this.sgMainHand.doubleSetting("Initial Y-Offset", 0.0, -10.0, 10.0, 0.05, "The starting vertical position of the hand during the swing animation.");
    private final Setting<Double> mainEndY = this.sgMainHand.doubleSetting("Terminal Y-Offset", 0.0, -10.0, 10.0, 0.05, "The ending vertical position of the hand during the swing animation.");
    private final Setting<Boolean> mainReset = this.sgMainHand.booleanSetting("Sequence Reset", false, "Immediately restarts the animation if another swing action is triggered before completion.");

    private final Setting<Double> offSpeed = this.sgOffHand.doubleSetting("Swing Velocity", 1.0, 0.0, 10.0, 0.05, "The temporal speed multiplier for the off-hand animation cycle.");
    private final Setting<Double> offStart = this.sgOffHand.doubleSetting("Initial Frame", 0.0, 0.0, 10.0, 0.05, "The normalized animation progress at which the off-hand swing begins.");
    private final Setting<Double> offEnd = this.sgOffHand.doubleSetting("Terminal Frame", 1.0, 0.0, 10.0, 0.05, "The normalized animation progress at which the off-hand swing concludes.");
    private final Setting<Double> offStartY = this.sgOffHand.doubleSetting("Initial Y-Offset", 0.0, -10.0, 10.0, 0.05, "The starting vertical position of the hand during the swing animation.");
    private final Setting<Double> offEndY = this.sgOffHand.doubleSetting("Terminal Y-Offset", 0.0, -10.0, 10.0, 0.05, "The ending vertical position of the hand during the swing animation.");
    private final Setting<Boolean> offReset = this.sgOffHand.booleanSetting("Sequence Reset", false, "Immediately restarts the animation if another swing action is triggered before completion.");

    private static boolean mainSwinging = false;
    private float mainProgress = 0.0F;
    private boolean offSwinging = false;
    private float offProgress = 0.0F;

    public SwingModifier() {
        super("Swing Modifier", "Provides granular control over the hand-swing animation interpolate, allowing for custom speeds, vertical offsets, and animation keyframes.", SubCategory.MISC_VISUAL, true);
        INSTANCE = this;
    }

    public static SwingModifier getInstance() {
        return INSTANCE;
    }

    public void startSwing(Hand hand) {
        if (hand == Hand.MAIN_HAND) {
            if (this.mainReset.get() || !mainSwinging) {
                this.mainProgress = 0.0F;
                mainSwinging = true;
            }
        } else if (this.offReset.get() || !this.offSwinging) {
            this.offProgress = 0.0F;
            this.offSwinging = true;
        }
    }

    @Event
    public void onRender(RenderEvent.World.Pre event) {
        if (mainSwinging) {
            if (this.mainProgress >= 1.0F) {
                mainSwinging = false;
                this.mainProgress = 0.0F;
            } else {
                this.mainProgress = (float) (this.mainProgress + event.frameTime * this.mainSpeed.get());
            }
        }

        if (this.offSwinging) {
            if (this.offProgress >= 1.0F) {
                this.offSwinging = false;
                this.offProgress = 0.0F;
            } else {
                this.offProgress = (float) (this.offProgress + event.frameTime * this.offSpeed.get());
            }
        }
    }

    public float getSwing(Hand hand) {
        return hand == Hand.MAIN_HAND
                ? (float) (this.mainStart.get() + (this.mainEnd.get() - this.mainStart.get()) * this.mainProgress)
                : (float) (this.offStart.get() + (this.offEnd.get() - this.offStart.get()) * this.offProgress);
    }

    public float getY(Hand hand) {
        return hand == Hand.MAIN_HAND
                ? (float) (this.mainStartY.get() + (this.mainEndY.get() - this.mainStartY.get()) * this.mainProgress) / -10.0F
                : (float) (this.offStartY.get() + (this.offEndY.get() - this.offStartY.get()) * this.offProgress) / -10.0F;
    }
}
