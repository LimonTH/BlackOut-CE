package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BackgroundMultiSetting;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.render.RenderUtils;

import java.util.Comparator;
import java.util.List;

public class Keybinds extends HudElement {
    public final SettingGroup sgGeneral = this.addGroup("General");

    private final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgGeneral, "Label Color");
    private final Setting<BlackOutColor> bindColor = this.sgGeneral.colorSetting("Hotkey Color", new BlackOutColor(128, 128, 128, 50), "The color used for the key designation text.");
    private final Setting<Boolean> bg = this.sgGeneral.booleanSetting("Backdrop", true, "Renders a background panel behind the active keybind list.");
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgGeneral, this.bg::get, null);
    private final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Gaussian Diffusion", true, "Applies a blur effect to the background for increased visual separation.");
    private final Setting<Boolean> rounded = this.sgGeneral.booleanSetting("Bezel Rounding", true, "Smooths the corners of the background and blur layers.", () -> this.bg.get() || this.blur.get());

    private int i = 0;
    private boolean checked = false;
    private float width = 0.0F;
    private float length = 0.0F;

    public Keybinds() {
        super("Keybinds", "Displays a real-time list of currently active modules that have an assigned hotkey.");
        this.setSize(10.0F, 10.0F);
    }

    @Override
    public void render() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            Comparator<Module> comparator = Comparator.comparingDouble(m -> BlackOut.FONT.getWidth(m.getDisplayName() + m.bind.get().getName()));
            List<Module> modules = Managers.MODULES
                    .getModules()
                    .stream()
                    .filter(module -> module.enabled && module.bind.get().value != null)
                    .sorted(comparator.reversed())
                    .toList();
            this.i = 0;
            this.stack.push();
            this.checked = false;
            modules.forEach(
                    module -> {
                        String text = module.getDisplayName();
                        String bind = " [" + module.bind.get().getName() + "]";
                        if (!this.checked) {
                            this.width = BlackOut.FONT.getWidth(text + bind);
                            this.length = ((BlackOut.FONT.getHeight() + 3.0F) * this.i + BlackOut.FONT.getHeight() + 3.0F) * modules.size();
                            this.checked = true;
                            if (this.blur.get()) {
                                RenderUtils.drawLoadedBlur(
                                        "hudblur",
                                        this.stack,
                                        renderer -> renderer.rounded(
                                                0.0F, (BlackOut.FONT.getHeight() + 3.0F) * this.i, this.width + 8.0F, this.length + 3.0F, this.rounded.get() ? 3.0F : 0.0F, 10
                                        )
                                );
                                Renderer.onHUDBlur();
                            }

                            if (this.bg.get()) {
                                this.background.render(this.stack, 0.0F, 0.0F, this.width + 8.0F, this.length + 3.0F, this.rounded.get() ? 3.0F : 0.0F, 3.0F);
                            }

                            this.setSize(this.width + 8.0F, this.length + 3.0F);
                        }

                        this.textColor.render(this.stack, text, 1.0F, 4.0F, (BlackOut.FONT.getHeight() + 3.0F) * this.i + 3.0F, false, false);
                        BlackOut.FONT
                                .text(
                                        this.stack,
                                        bind,
                                        1.0F,
                                        4.0F + BlackOut.FONT.getWidth(text),
                                        (BlackOut.FONT.getHeight() + 3.0F) * this.i + 3.0F,
                                        this.bindColor.get().getColor(),
                                        false,
                                        false
                                );
                        this.i++;
                    }
            );
            this.stack.pop();
        }
    }
}
