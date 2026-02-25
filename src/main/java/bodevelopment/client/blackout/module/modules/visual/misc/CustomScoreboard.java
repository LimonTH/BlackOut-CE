package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;
import java.util.Collection;

public class CustomScoreboard extends Module {
    private static CustomScoreboard INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");
    private final SettingGroup sgColor = this.addGroup("Color");

    public final Setting<Boolean> remove = this.sgGeneral.booleanSetting("Disable Rendering", false, "Completely prevents the scoreboard from appearing on the HUD.");
    public final Setting<Boolean> useFont = this.sgGeneral.booleanSetting("Custom Font Engine", true, "Switches from the vanilla font renderer to the client's high-definition font system.", () -> !this.remove.get());
    public final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Gaussian Blur", true, "Applies a background blur effect behind the scoreboard for enhanced contrast.", () -> !this.remove.get());
    public final Setting<Boolean> background = this.sgGeneral.booleanSetting("Plate Background", true, "Renders a solid or semi-transparent background panel behind the scoreboard data.", () -> !this.remove.get());
    public final Setting<Boolean> shadow = this.sgGeneral.booleanSetting("Outer Shadow", true, "Applies a soft drop shadow to the background plate.", () -> !this.remove.get() && this.background.get());
    private final Setting<Double> scale = this.sgGeneral.doubleSetting("Visual Scale", 1.0, 0.0, 10.0, 0.05, "Adjusts the overall size of the scoreboard element.");
    private final Setting<Integer> addedY = this.sgGeneral.intSetting("Vertical Offset", 0, 0, 500, 10, "Adjusts the vertical placement of the scoreboard on the HUD.");

    public final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgColor, () -> this.useFont.get() && !this.remove.get(), "Label Palette");
    public final Setting<BlackOutColor> bgColor = this.sgColor.colorSetting("Plate Color", new BlackOutColor(0, 0, 0, 50), "The base color of the scoreboard background.", () -> this.background.get() && !this.remove.get());
    public final Setting<BlackOutColor> shadowColor = this.sgColor.colorSetting("Shadow Tint", new BlackOutColor(0, 0, 0, 100), "The color applied to the background's drop shadow.", () -> this.background.get() && this.shadow.get() && !this.remove.get());


    private final MatrixStack stack = new MatrixStack();
    public String objectiveName;
    public Color objectiveColor;
    public Collection<String> texts;
    private float y = 0.0F;
    public CustomScoreboard() {
        super("Scoreboard", "Provides extensive customization for the in-game scoreboard, including font overrides, background effects, and scaling.", SubCategory.MISC_VISUAL, true);
        INSTANCE = this;
    }

    public static CustomScoreboard getInstance() {
        return INSTANCE;
    }

    @Event
    public void onRender(RenderEvent.Hud.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (this.objectiveName != null && this.texts != null && !this.remove.get()) {
                float width = Math.max(this.getLongest(this.texts) * 2.0F + 20.0F, BlackOut.FONT.getWidth(this.objectiveName) * 2.0F + 40.0F);
                float length = (this.texts.size() + 2) * BlackOut.FONT.getHeight() * 2.0F + 6.0F;
                this.stack.push();
                RenderUtils.unGuiScale(this.stack);
                this.stack
                        .translate(
                                BlackOut.mc.getWindow().getWidth() - (width + 8.0F) * this.scale.get(),
                                BlackOut.mc.getWindow().getHeight() / 2.0F + this.addedY.get(),
                                0.0
                        );
                this.stack.scale(this.scale.get().floatValue(), this.scale.get().floatValue(), 0.0F);
                if (this.blur.get()) {
                    RenderUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(0.0F, 0.0F, width, length + 4.0F, 6.0F, 10));
                    Renderer.onHUDBlur();
                }

                if (this.background.get()) {
                    RenderUtils.rounded(this.stack, 0.0F, 0.0F, width, length + 4.0F, 6.0F, 6.0F, this.bgColor.get().getRGB(), this.shadowColor.get().getRGB());
                }

                BlackOut.FONT.text(this.stack, this.objectiveName, 2.0F, width / 2.0F, 0.0F, this.objectiveColor, true, false);
                this.y = BlackOut.FONT.getHeight() * 2.0F + 4.0F;
                this.texts.forEach(text -> {
                    this.textColor.render(this.stack, text, 2.0F, 0.0F, this.y, false, false);
                    this.y = this.y + (BlackOut.FONT.getHeight() * 2.0F + 2.0F);
                });
                this.stack.pop();
            }
        }
    }

    private float getLongest(Collection<String> text) {
        String longestString = "";

        for (String str : text) {
            if (str.length() > longestString.length()) {
                longestString = str;
            }
        }

        return BlackOut.FONT.getWidth(longestString);
    }
}
