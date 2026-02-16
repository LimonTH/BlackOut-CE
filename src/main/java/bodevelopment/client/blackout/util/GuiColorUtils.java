package bodevelopment.client.blackout.util;

import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.util.render.AnimUtils;
import net.minecraft.util.math.MathHelper;

import java.awt.*;

public class GuiColorUtils {
    public static Color bg1 = getColor(25);
    public static Color bg2 = getColor(30);
    public static Color bg3 = getColor(35);
    public static Color bindBG = getColor(26);
    public static Color bindText = getColor(255);
    public static Color enabled = getColor(255);
    public static Color disabled = getColor(150);
    public static Color disabledSettingCategory = getColor(125);
    public static Color disabledSettingText = getColor(100);
    public static Color enabledSettingCategory = getColor(175);
    public static Color enabledSettingText = getColor(150);
    public static Color blackout = getColor(255);
    public static Color category = getColor(255);
    public static Color parentCategory = getColor(150);
    public static Color enabledDisabledBindBg = getColor(75);
    public static Color disabledDisabledBindBg = getColor(50);
    public static Color enabledEnabledBindBg = getColor(150);
    public static Color disabledEnabledBindBg = getColor(120);
    public static Color enabledDisabledBindDot = getColor(100);
    public static Color disabledDisabledBindDot = getColor(75);
    public static Color enabledEnabledBindDot = getColor(255);
    public static Color disabledEnabledBindDot = getColor(150);
    public static long toggleTime;
    public static boolean isEnabled;

    public static void set(Module module) {
        toggleTime = module.toggleTime;
        isEnabled = module.enabled;
    }

    public static void set(HudElement element) {
        toggleTime = element.toggleTime;
        isEnabled = element.enabled;
    }

    private static Color getColor(int d) {
        return new Color(d, d, d);
    }

    private static Color getRed(int r) {
        return new Color(100, 100, r, 255);
    }

    public static Color getSettingText(float y) {
        return ColorUtils.lerpColor(
                getColorProgress((long) (toggleTime + y * 0.25)),
                isEnabled ? disabledSettingText : enabledSettingText,
                isEnabled ? enabledSettingText : disabledSettingText
        );
    }

    public static Color getSettingCategory(float y) {
        return ColorUtils.lerpColor(
                getColorProgress((long) (toggleTime + y * 0.25)),
                isEnabled ? disabledSettingCategory : enabledSettingCategory,
                isEnabled ? enabledSettingCategory : disabledSettingCategory
        );
    }

    public static Color getEnabledBindBG(float y) {
        return ColorUtils.lerpColor(
                getColorProgress((long) (toggleTime + y * 0.25)),
                isEnabled ? disabledEnabledBindBg : enabledEnabledBindBg,
                isEnabled ? enabledEnabledBindBg : disabledEnabledBindBg
        );
    }

    public static Color getDisabledBindBG(float y) {
        return ColorUtils.lerpColor(
                getColorProgress((long) (toggleTime + y * 0.25)),
                isEnabled ? disabledDisabledBindBg : enabledDisabledBindBg,
                isEnabled ? enabledDisabledBindBg : disabledDisabledBindBg
        );
    }

    public static Color getEnabledBindDot(float y) {
        return ColorUtils.lerpColor(
                getColorProgress((long) (toggleTime + y * 0.25)),
                isEnabled ? disabledEnabledBindDot : enabledEnabledBindDot,
                isEnabled ? enabledEnabledBindDot : disabledEnabledBindDot
        );
    }

    public static Color getDisabledBindDot(float y) {
        return ColorUtils.lerpColor(
                getColorProgress((long) (toggleTime + y * 0.25)),
                isEnabled ? disabledDisabledBindDot : enabledDisabledBindDot,
                isEnabled ? enabledDisabledBindDot : disabledDisabledBindDot
        );
    }

    private static float getColorProgress(long time) {
        float toggleProgress = (float) MathHelper.clamp(System.currentTimeMillis() - time, 0L, 500L) / 500.0F;
        return (float) AnimUtils.easeInOutCubic(toggleProgress);
    }
}
