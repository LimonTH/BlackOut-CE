package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.hud.HudElement;
import bodevelopment.client.blackout.module.modules.client.ThemeSettings;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.rendering.renderer.Renderer;
import bodevelopment.client.blackout.util.render.RenderUtils;

import java.awt.*;

public class ThemeTest extends HudElement {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<Boolean> useBlur = this.sgGeneral.booleanSetting("Blur", true, "Uses a blur effect", () -> true);
    private final Setting<Boolean> shadow = this.sgGeneral.booleanSetting("Shadow", true, ".", () -> true);
    private int i = 0;
    private float x = 0.0F;
    private float y = 0.0F;

    public ThemeTest() {
        super("Theme Test", ".");
        this.setSize(10.0F, 10.0F);
    }

    @Override
    public void render() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.stack.push();
            this.setSize(20.0F, 20.0F);
            ThemeSettings themeSettings = ThemeSettings.getInstance();
            this.i = 0;
            this.x = 0.0F;
            this.y = 0.0F;
            themeSettings.getThemes()
                    .forEach(
                            theme -> {
                                if (this.useBlur.get()) {
                                    RenderUtils.drawLoadedBlur("hudblur", this.stack, renderer -> renderer.rounded(this.x, this.y, 100.0F, 30.0F, 2.0F, 10));
                                    Renderer.onHUDBlur();
                                }

                                RenderUtils.tenaRounded(
                                        this.stack,
                                        this.x,
                                        this.y,
                                        100.0F,
                                        30.0F,
                                        2.0F,
                                        this.shadow.get() ? 2.0F : 0.0F,
                                        theme.mainWithAlpha(175),
                                        theme.secondaryWithAlpha(175),
                                        1.5F
                                );
                                BlackOut.BOLD_FONT.text(this.stack, theme.getName(), 1.0F, this.x + 50.0F, this.y + 15.0F, Color.WHITE.getRGB(), true, true);
                                this.i++;
                                this.x += 108.0F;
                                if (this.i % 5 == 0) {
                                    this.y += 38.0F;
                                    this.x = 0.0F;
                                }
                            }
                    );
            this.stack.pop();
        }
    }
}
