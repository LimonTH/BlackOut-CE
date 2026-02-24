package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.util.math.MatrixStack;

public class Crosshair extends Module {
    private static Crosshair INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Boolean> remove = this.sgGeneral.booleanSetting("Remove", false, "Stops the crosshair from rendering");
    private final Setting<Integer> dist = this.sgGeneral.intSetting("Distance", 5, 0, 25, 1, ".", () -> !this.remove.get());
    private final Setting<Integer> width = this.sgGeneral.intSetting("Width", 1, 1, 5, 1, ".", () -> !this.remove.get());
    private final Setting<Integer> length = this.sgGeneral.intSetting("Length", 10, 0, 50, 1, ".", () -> !this.remove.get());
    private final Setting<BlackOutColor> color = this.sgGeneral.colorSetting("Color", new BlackOutColor(255, 255, 255, 225), ".", () -> !this.remove.get());
    public final Setting<Boolean> t = this.sgGeneral.booleanSetting("T shape", false, ".");
    private final MatrixStack stack = new MatrixStack();

    public Crosshair() {
        super("Crosshair", "Modifies the games crosshair", SubCategory.MISC_VISUAL, true);
        INSTANCE = this;
    }

    public static Crosshair getInstance() {
        return INSTANCE;
    }

    @Event
    public void onRender(RenderEvent.Hud.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (!this.remove.get()) {
                this.stack.push();
                RenderUtils.unGuiScale(this.stack);
                this.stack.translate(BlackOut.mc.getWindow().getWidth() / 2.0F - 1.0F, BlackOut.mc.getWindow().getHeight() / 2.0F - 1.0F, 0.0F);
                int d = this.dist.get();
                int w = this.width.get();
                int l = this.length.get();
                if (!this.t.get()) {
                    RenderUtils.rounded(this.stack, -w / 2.0F, -d - l, w, l, 0.0F, 0.0F, this.color.get().getRGB(), this.color.get().getRGB());
                }

                RenderUtils.rounded(this.stack, d, -w / 2.0F, l, w, 0.0F, 0.0F, this.color.get().getRGB(), this.color.get().getRGB());
                RenderUtils.rounded(this.stack, -w / 2.0F, d, w, l, 0.0F, 0.0F, this.color.get().getRGB(), this.color.get().getRGB());
                RenderUtils.rounded(this.stack, -d - l, -w / 2.0F, l, w, 0.0F, 0.0F, this.color.get().getRGB(), this.color.get().getRGB());
                this.stack.pop();
            }
        }
    }
}
