package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BackgroundMultiSetting;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

public class Effects extends HudElement {
    public final SettingGroup sgGeneral = this.addGroup("General");
    public final SettingGroup sgColor = this.addGroup("Color");

    public final Setting<Style> style = this.sgGeneral.enumSetting("Visual Theme", Style.Blackout, "The aesthetic layout used to display status effects (Modern 'Blackout' boxes or traditional 'Simple' text).");
    private final Setting<Double> minWidth = this.sgGeneral.doubleSetting("Minimum Dimensions", 0.0, 0.0, 100.0, 1.0, "The minimum width for the effect background boxes.", () -> this.style.get() == Style.Blackout);
    private final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Gaussian Diffusion", true, "Applies a real-time blur effect behind each effect container for maximum legibility.", () -> this.style.get() == Style.Blackout);
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgGeneral, () -> this.style.get() == Style.Blackout, null);
    public final Setting<Side> side = this.sgGeneral.enumSetting("Horizontal Anchor", Side.Left, "Determines whether the effects list aligns to the left or right relative to its origin.");
    public final Setting<Order> order = this.sgGeneral.enumSetting("Sort Hierarchy", Order.Longest, "The criteria used to organize the sequence of active effects.");
    private final Setting<Boolean> rn = this.sgGeneral.booleanSetting("Roman Notation", false, "Converts numerical amplifier levels into Roman numerals (e.g., Strength II).");
    private final Setting<Boolean> up = this.sgGeneral.booleanSetting("Vertical Inversion", false, "Renders the effects list growing upwards from the starting coordinate.");

    public final Setting<ColorMode> colorMode = this.sgColor.enumSetting("Color Assignment", ColorMode.Custom, "The logic for text coloring (Manual selection or matching the native Minecraft effect color).");
    private final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgColor, () -> this.colorMode.get() == ColorMode.Custom, "Primary Label");
    private final Setting<BlackOutColor> infoColor = this.sgColor.colorSetting("Metadata Color", new BlackOutColor(200, 200, 200, 255), "The color applied to duration timers and amplifier levels.");

    private final String[] romanNumerals = new String[]{"I", "II", "III", "IV", "V"};
    private final List<Component> components = new ArrayList<>();
    private float offset;

    public Effects() {
        super("Effects", "Displays a real-time overview of active status effects, including duration timers and amplifier levels with advanced sorting.");
        this.setSize(10.0F, 10.0F);
    }

    @Override
    public void render() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (BlackOut.mc.player.getActiveStatusEffects() != null) {
                this.stack.push();
                float width = BlackOut.FONT.getWidth("idunnoman 4:20");
                this.setSize(width, BlackOut.FONT.getHeight() * 2.0F + 1.0F);
                if (this.side.get() == Side.Right) {
                    this.stack.translate(width, 0.0F, 0.0F);
                }

                Comparator<Entry<RegistryEntry<StatusEffect>, StatusEffectInstance>> comparator = Comparator.comparingDouble(this::getWidth);
                BlackOut.mc
                        .player
                        .getActiveStatusEffects()
                        .entrySet()
                        .stream()
                        .sorted(this.order.get() == Order.Shortest ? comparator : comparator.reversed())
                        .forEach(entry -> this.stack.translate(0.0F, this.render(entry.getKey().value(), entry.getValue()), 0.0F));
                this.stack.pop();
            }
        }
    }

    private float render(StatusEffect effect, StatusEffectInstance effectInstance) {
        int timeS = (int) Math.floor(effectInstance.getDuration() / 20.0);
        String timeString = (int) Math.floor(timeS / 60.0F) + ":" + (timeS % 60 < 10 ? "0" : "") + timeS % 60;
        String levelString = this.levelString(effectInstance);
        String nameString = effect.getName().getString();
        float returnHeight = 0.0F;
        switch (this.style.get()) {
            case Blackout:
                float width = Math.max(this.getWidth(effect, effectInstance), BlackOut.FONT.getWidth(timeString) + 4.0F);
                float height = BlackOut.FONT.getHeight() * 2.0F + 1.0F;
                float rad = 3.0F;
                if (this.blur.get()) {
                    RenderUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(this.side.get().getSide(width), 0.0F, width, height, rad, 10));
                    Renderer.onHUDBlur();
                }

                this.background.render(this.stack, this.side.get().getSide(width), 0.0F, width, height, rad, 3.0F);
                switch (this.colorMode.get()) {
                    case Custom:
                        this.textColor.render(this.stack, nameString, 1.0F, this.side.get().getSide(width) + 2.0F, 5.0F, false, true);
                        break;
                    case Effect:
                        BlackOut.FONT.text(this.stack, nameString, 1.0F, this.side.get().getSide(width) + 2.0F, 5.0F, new Color(effect.getColor()), false, true);
                }

                BlackOut.FONT.text(this.stack, levelString, 1.0F, this.side.get().getSide(width) + width - 5.0F, 5.0F, this.infoColor.get().getColor(), true, true);
                BlackOut.FONT
                        .text(
                                this.stack,
                                timeString,
                                1.0F,
                                this.side.get().getSide(width) + 2.0F,
                                height - BlackOut.FONT.getHeight() / 2.0F,
                                this.infoColor.get().getColor(),
                                false,
                                true
                        );
                returnHeight = height + rad * 2.0F + 3.0F;
                break;
            case Simple:
                this.offset = 0.0F;
                this.components.clear();
                this.components.add(new Component(nameString, false));
                this.components.add(new Component(levelString, true));
                this.components.add(new Component("(" + timeString + ")", true));
                this.components.forEach(component -> {
                    if (component.info) {
                        BlackOut.FONT.text(this.stack, component.text, 1.0F, this.offset, 0.0F, this.infoColor.get().getColor(), false, true);
                    } else {
                        switch (this.colorMode.get()) {
                            case Custom:
                                this.textColor.render(this.stack, component.text, 1.0F, this.offset, 0.0F, false, true);
                                break;
                            case Effect:
                                BlackOut.FONT.text(this.stack, component.text, 1.0F, this.offset, 0.0F, new Color(effect.getColor()), false, true);
                        }
                    }

                    this.offset = this.offset + (component.width + BlackOut.FONT.getWidth(" "));
                });
                returnHeight = BlackOut.FONT.getHeight();
        }

        return this.up.get() ? -returnHeight : returnHeight;
    }

    private float getWidth(Entry<RegistryEntry<StatusEffect>, StatusEffectInstance> entry) {
        return this.getWidth(entry.getKey().value(), entry.getValue());
    }

    private float getWidth(StatusEffect effect, StatusEffectInstance effectInstance) {
        return this.getWidth(effect.getName().getString(), String.valueOf(effectInstance.getAmplifier()));
    }

    private float getWidth(String name, String level) {
        return Math.max(BlackOut.FONT.getWidth(name + level) + 10.0F, this.minWidth.get().floatValue());
    }

    private String levelString(StatusEffectInstance instance) {
        if (instance.getAmplifier() < 0) {
            return "-";
        } else {
            return this.rn.get() && instance.getAmplifier() < this.romanNumerals.length
                    ? this.romanNumerals[instance.getAmplifier()]
                    : String.valueOf(instance.getAmplifier() + 1);
        }
    }

    public enum ColorMode {
        Custom,
        Effect
    }

    public enum Order {
        Shortest,
        Longest
    }

    public enum Side {
        Left,
        Right;

        private float getSide(float width) {
            return switch (this) {
                case Left -> 0.0F;
                case Right -> -width;
            };
        }
    }

    public enum Style {
        Simple,
        Blackout
    }

    private static class Component {
        private final String text;
        private final float width;
        private final boolean info;

        private Component(String text, boolean info) {
            this.text = text;
            this.width = BlackOut.FONT.getWidth(text);
            this.info = info;
        }
    }
}
