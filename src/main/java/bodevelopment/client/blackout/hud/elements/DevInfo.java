package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BackgroundMultiSetting;
import bodevelopment.client.blackout.module.setting.multisettings.TextColorMultiSetting;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.render.RenderUtils;

import java.util.ArrayList;
import java.util.List;

public class DevInfo extends HudElement {
    public final SettingGroup sgGeneral = this.addGroup("General");
    private final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgGeneral, "Text");
    private final Setting<Boolean> bg = this.sgGeneral.booleanSetting("Background", true, "Renders a background");
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgGeneral, this.bg::get, null);
    private final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Blur", true, ".");
    private final Setting<Boolean> rounded = this.sgGeneral.booleanSetting("Rounded", true, "Renders a background", () -> this.bg.get() || this.blur.get());
    private final Setting<Boolean> typeColor = this.sgGeneral.booleanSetting("Use Type Color", false, ".");
    private final List<Component> components = new ArrayList<>();
    private float offset = 0.0F;

    public DevInfo() {
        super("Dev Info", ".");
        this.setSize(10.0F, 10.0F);
    }

    @Override
    public void render() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.components.clear();
            String text = "Public " + BlackOut.TYPE + " Build - " + BlackOut.VERSION;
            this.components.add(new Component("Public "));
            this.components.add(new Component(BlackOut.TYPE.name(), this.typeColor.get() ? BlackOut.TYPECOLOR : null, true));
            this.components.add(new Component(" Build - " + BlackOut.VERSION));
            this.stack.push();
            if (this.blur.get()) {
                RenderUtils.drawLoadedBlur(
                        "hudblur",
                        this.stack,
                        renderer -> renderer.rounded(0.0F, 0.0F, BlackOut.FONT.getWidth(text), BlackOut.FONT.getHeight(), this.rounded.get() ? 3.0F : 0.0F, 10)
                );
                Renderer.onHUDBlur();
            }

            if (this.bg.get()) {
                this.background.render(this.stack, 0.0F, 0.0F, BlackOut.FONT.getWidth(text), BlackOut.FONT.getHeight(), this.rounded.get() ? 3.0F : 0.0F, 3.0F);
            }

            this.setSize(BlackOut.FONT.getWidth(text), BlackOut.FONT.getHeight());
            this.offset = 0.0F;
            this.components.forEach(component -> {
                if (component.color == null) {
                    this.textColor.render(this.stack, component.text, 1.0F, this.offset, 0.0F, false, false, component.bold);
                } else if (component.bold) {
                    BlackOut.BOLD_FONT.text(this.stack, component.text, 1.0F, this.offset, 0.0F, component.color, false, false);
                } else {
                    BlackOut.FONT.text(this.stack, component.text, 1.0F, this.offset, 0.0F, component.color, false, false);
                }

                this.offset = this.offset + component.width;
            });
            this.stack.pop();
        }
    }
}
