package bodevelopment.client.blackout.hud.elements;

import bodevelopment.client.blackout.util.PlayerUtils;
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
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Coordinates extends HudElement {
    public final SettingGroup sgGeneral = this.addGroup("General");
    public final SettingGroup sgColor = this.addGroup("Color");

    private final Setting<Boolean> otherWorld = this.sgGeneral.booleanSetting("Cross-Dimension Tracking", true, "Calculates and displays relative coordinates for the corresponding dimension (Nether/Overworld ratio).");
    private final Setting<Boolean> bg = this.sgGeneral.booleanSetting("Backdrop", true, "Renders a background panel behind the coordinate text.");
    private final BackgroundMultiSetting background = BackgroundMultiSetting.of(this.sgGeneral, this.bg::get, null);
    private final Setting<Boolean> blur = this.sgGeneral.booleanSetting("Gaussian Diffusion", true, "Applies a real-time blur effect to the background for improved legibility.");
    private final Setting<Boolean> rounded = this.sgGeneral.booleanSetting("Bezel Rounding", true, "Smooths the corners of the background and blur layers.", () -> this.bg.get() || this.blur.get());

    private final TextColorMultiSetting textColor = TextColorMultiSetting.of(this.sgColor, "Value Color");
    private final TextColorMultiSetting infoColor = TextColorMultiSetting.of(this.sgColor, "Label Color");

    private final List<Component> components = new ArrayList<>();
    private float offset = 0.0F;
    private int i = 0;
    private boolean drawingOther = false;

    public Coordinates() {
        super("Coordinates", "Displays your current spatial position in the world, with support for real-time dimension scaling.");
        this.setSize(10.0F, 10.0F);
    }

    @Override
    public void render() {
        if (PlayerUtils.isInGame()) {
            String text = this.getString(BlackOut.mc.player.position());
            String otherWorldText = BlackOut.NAME + " Client coordinate calculation system thread 42069 has failed the mathing of the coordinates please hit your pc with a hammer to fix the issue";
            this.stack.pushPose();
            float height;
            float width;
            if (this.drawingOther) {
                ResourceKey<Level> worldKey = BlackOut.mc.level.dimension();

                if (worldKey == Level.OVERWORLD) {
                    otherWorldText = this.getString(BlackOut.mc.player.position().scale(0.125));
                } else if (worldKey == Level.NETHER) {
                    otherWorldText = this.getString(BlackOut.mc.player.position().scale(8.0));
                }

                height = BlackOut.FONT.getHeight() * 2.0F - 2.0F;
                width = Math.max(BlackOut.FONT.getWidth(text), BlackOut.FONT.getWidth(otherWorldText));
            } else {
                height = BlackOut.FONT.getHeight();
                width = BlackOut.FONT.getWidth(text);
            }

            this.setSize(width, BlackOut.FONT.getHeight());
            if (this.blur.get()) {
                RenderUtils.drawLoadedBlur(
                        "hudblur",
                        this.stack,
                        renderer -> renderer.rounded(0.0F, this.drawingOther ? -BlackOut.FONT.getHeight() : 0.0F, width, height, this.rounded.get() ? 3.0F : 0.0F, 10)
                );
                Renderer.onHUDBlur();
            }

            if (this.bg.get()) {
                this.background
                        .render(this.stack, 0.0F, this.drawingOther ? -BlackOut.FONT.getHeight() : 0.0F, width, height, this.rounded.get() ? 3.0F : 0.0F, 3.0F);
            }

            this.drawingOther = false;
            this.updateComponents(BlackOut.mc.player.position());
            this.drawComponents(0.0F);
            if (this.otherWorld.get() && BlackOut.mc.level.dimension() != Level.END) {
                Vec3 pos = null;

                ResourceKey<Level> currentWorld = BlackOut.mc.level.dimension();

                if (currentWorld == Level.OVERWORLD) {
                    pos = BlackOut.mc.player.position().multiply(0.125, 1.0, 0.125);
                } else if (currentWorld == Level.NETHER) {
                    pos = BlackOut.mc.player.position().multiply(8.0, 1.0, 8.0);
                }

                if (pos != null) {
                    this.updateComponents(pos);
                }

                this.drawComponents(-BlackOut.FONT.getHeight() - 2.0F);
                this.drawingOther = true;
            }

            this.stack.popPose();
        }
    }

    private void drawComponents(float y) {
        this.components.forEach(component -> {
            if (this.i % 2 == 0) {
                this.infoColor.render(this.stack, component.text, 1.0F, this.offset, y, false, false);
            } else {
                this.textColor.render(this.stack, component.text, 1.0F, this.offset, y, false, false);
            }

            this.offset = this.offset + component.width;
            this.i++;
        });
    }

    private String getString(Vec3 pos) {
        return String.format("X: %.1f Y: %.1f Z: %.1f", pos.x, pos.y, pos.z);
    }

    private void updateComponents(Vec3 pos) {
        this.i = 1;
        this.offset = 0.0F;
        this.components.clear();
        this.components.add(new Component("X: "));
        this.components.add(new Component(this.format(pos.x) + " "));
        this.components.add(new Component("Y: "));
        this.components.add(new Component(this.format(pos.y) + " "));
        this.components.add(new Component("Z: "));
        this.components.add(new Component(this.format(pos.z)));
    }

    private String format(Double d) {
        return String.format("%.1f", d);
    }
}
