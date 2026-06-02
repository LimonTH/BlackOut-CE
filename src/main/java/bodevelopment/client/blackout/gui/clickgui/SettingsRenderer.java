package bodevelopment.client.blackout.gui.clickgui;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.gui.clickgui.components.ModuleComponent;
import bodevelopment.client.blackout.module.modules.client.GuiSettings;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.settings.EnumSetting;
import bodevelopment.client.blackout.rendering.renderer.TextureRenderer;
import bodevelopment.client.blackout.rendering.texture.BOTextures;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.GuiColorUtils;
import bodevelopment.client.blackout.util.render.Render2DUtils;
import com.mojang.blaze3d.vertex.PoseStack;

import java.awt.Color;
import java.util.List;

/**
 * Shared rendering utilities for setting groups and individual settings.
 * Used by both {@link ModuleComponent} (ClickGUI modules) and
 * {@link bodevelopment.client.blackout.hud.HudEditorSettings} (HUD element settings).
 */
public class SettingsRenderer {
    public static final float RESET_ICON_SIZE = 20.0F;
    public static final float RESET_HIT_SIZE = 28.0F;
    public static final float RESET_HOLD_THRESHOLD = 2.5F;

    public static class ResetState {
        public boolean hovered = false;
        public float holdTime = 0.0F;
    }

    public static boolean hasVisibleSettings(SettingGroup group) {
        for (Setting<?> setting : group.settings) {
            if (setting.isVisible()) return true;
        }
        return false;
    }

    /**
     * Calculates the total height of all setting groups for a module/element.
     */
    public static float getLength(List<SettingGroup> settingGroups) {
        float fs = GuiSettings.getInstance().fontScale.get().floatValue();
        float length = switch (GuiSettings.getInstance().settingGroup.get()) {
            case Line, Shadow, None -> 0.0F;
            case Quad -> 7.0F * fs;
        };

        int visible = (int) settingGroups.stream().filter(g -> g.settings.stream().anyMatch(Setting::isVisible)).count();
        int idx = 0;

        for (SettingGroup group : settingGroups) {
            if (group.settings.stream().noneMatch(Setting::isVisible)) continue;
            idx++;

            length += switch (GuiSettings.getInstance().settingGroup.get()) {
                case Line, None -> 40.0F * fs;
                case Shadow -> 45.0F * fs;
                case Quad -> 50.0F * fs;
            };

            for (Setting<?> setting : group.settings) {
                if (setting.isVisible()) {
                    length += setting.getHeight();
                }
            }

            if (idx < visible) {
                length += 5.0F * fs;
            }
        }
        return length;
    }

    /**
     * Renders the hold-to-confirm reset button icon and returns the updated ResetState.
     */
    public static void renderResetButton(PoseStack stack, float centerX, float centerY,
                                          float mx, float my, float frameTime,
                                          ResetState state, boolean ready) {
        float halfHit = RESET_HIT_SIZE / 2.0F;
        state.hovered = mx > centerX - halfHit
                && mx < centerX + halfHit
                && my > centerY - halfHit
                && my < centerY + halfHit;

        if (state.hovered && !ready) {
            state.holdTime = Math.min(state.holdTime + frameTime, RESET_HOLD_THRESHOLD);
            if (state.holdTime >= RESET_HOLD_THRESHOLD) {
                ClickGui.hoveredDescription = "Click to revert module settings to stock.";
            } else {
                ClickGui.hoveredDescription = "Wait...";
            }
        } else if (!state.hovered) {
            state.holdTime = Math.max(state.holdTime - frameTime * 3.0F, 0.0F);
        }

        float progress = state.holdTime / RESET_HOLD_THRESHOLD;
        boolean isReady = progress >= 1.0F;

        int alpha = isReady ? 255 : (int) (80 + progress * 175);
        int color = ColorUtils.withAlpha(Color.WHITE.getRGB(), alpha);
        TextureRenderer icon = BOTextures.getResetIconRenderer();
        float halfIcon = RESET_ICON_SIZE / 2.0F;

        stack.pushPose();
        stack.translate(centerX, centerY, 0.0F);
        stack.mulPose(com.mojang.math.Axis.ZP.rotation((float) (progress * Math.PI * 6.0)));
        icon.quad(stack, -halfIcon, -halfIcon, RESET_ICON_SIZE, RESET_ICON_SIZE, color);
        stack.popPose();
    }

    /**
     * Renders a single setting group header and all its visible settings.
     *
     * @return the height consumed by this group
     */
    public static float renderSettingGroup(PoseStack stack, SettingGroup group, boolean last,
                                            float x, float y, float width,
                                            float frameTime, double mx, double my) {
        float fs = GuiSettings.getInstance().fontScale.get().floatValue();
        float groupScale = fs * 2.0F;

        float categoryLength = 42.0F * fs;
        for (Setting<?> setting : group.settings) {
            if (setting.isVisible()) {
                categoryLength += setting.getHeight();
            }
        }

        Color catColor = GuiColorUtils.getSettingCategory(y + 30.0F * fs);

        switch (GuiSettings.getInstance().settingGroup.get()) {
            case Line:
                Render2DUtils.fadeLine(stack, x, y + 30.0F * fs, x + width, y + 30.0F * fs,
                        catColor.getRGB());
                BlackOut.FONT.text(stack, group.name, groupScale,
                        x + width / 2.0F, y + 20.0F * fs,
                        catColor, true, true);
                break;
            case Shadow:
                float bottomY = y + categoryLength - 10.0F * fs;
                if (!last && bottomY < ClickGui.height + 50.0F) {
                    Render2DUtils.fade(stack, x - 5, y + categoryLength - 10.0F * fs,
                            width + 10.0F, 20.0F, ColorUtils.SHADOW80I, Render2DUtils.FadeSide.TOP);
                }
                Render2DUtils.fade(stack, x - 5, y + 30.0F * fs,
                        width + 10.0F, 20.0F, ColorUtils.SHADOW80I, Render2DUtils.FadeSide.BOTTOM);
                BlackOut.FONT.text(stack, group.name, groupScale,
                        x + width / 2.0F, y + 15.0F * fs,
                        catColor, true, true);
                break;
            case Quad:
                Render2DUtils.rounded(stack, x + 7, y + 12.0F * fs,
                        width - 14.0F, categoryLength,
                        2.0F, 7.0F,
                        GuiColorUtils.bg2.getRGB(), ColorUtils.SHADOW80I);
                BlackOut.FONT.text(stack, group.name, groupScale,
                        x + width / 2.0F, y + 31.0F * fs,
                        catColor, true, true);
                break;
            case None:
                BlackOut.FONT.text(stack, group.name, groupScale,
                        x + width / 2.0F, y + 20.0F * fs,
                        catColor, true, true);
                break;
        }

        float currentY = y;
        currentY += computeGroupHeaderHeight();
        for (Setting<?> setting : group.settings) {
            if (!setting.isVisible()) continue;

            float renderY = currentY;
            if (mx > x && mx < x + width
                    && my > renderY - 5.5F && my < renderY + setting.getHeight() - 5.5F) {
                if (setting.description != null && !setting.description.isEmpty()) {
                    ClickGui.hoveredDescription = setting.description;
                }
            }

            boolean shouldRender = true;
            currentY += setting.onRender(stack, frameTime, width, x, renderY, mx, my, shouldRender);
        }

        return currentY - y;
    }

    private static float computeGroupHeaderHeight() {
        float fs = GuiSettings.getInstance().fontScale.get().floatValue();
        return switch (GuiSettings.getInstance().settingGroup.get()) {
            case Line, None -> 40.0F * fs;
            case Shadow -> 45.0F * fs;
            case Quad -> 50.0F * fs;
        };
    }

    public static void renderEnumDropdowns(List<SettingGroup> groups) {
        for (SettingGroup group : groups) {
            for (Setting<?> s : group.settings) {
                if (s instanceof EnumSetting<?> es && es.isChoosing()) {
                    es.renderDropdown();
                }
            }
        }
    }

    /**
     * Renders the globally-last-opened enum dropdown on top of all others.
     */
    public static void renderLastOpenedDropdown() {
        EnumSetting<?> lastOpened = EnumSetting.getGlobalLastOpened();
        if (lastOpened != null && lastOpened.isChoosing()) {
            lastOpened.renderDropdown();
        }
    }
}
