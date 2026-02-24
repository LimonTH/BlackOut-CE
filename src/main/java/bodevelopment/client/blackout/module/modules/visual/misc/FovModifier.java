package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.keys.KeyBind;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.render.AnimUtils;
import net.minecraft.util.math.MathHelper;

public class FovModifier extends Module {
    private static FovModifier INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Double> fov = this.sgGeneral.doubleSetting("FOV", 100.0, 10.0, 170.0, 5.0, ".");
    public final Setting<KeyBind> zoomKey = this.sgGeneral.keySetting("Zoom Key", ".");
    public final Setting<Double> zoom = this.sgGeneral.doubleSetting("Zoom FOV", 30.0, 5.0, 100.0, 1.0, ".", () -> this.zoomKey.get().value != null);
    public final Setting<Double> zoomTime = this.sgGeneral.doubleSetting("Zoom Time", 0.3, 0.0, 5.0, 0.05, ".", () -> this.zoomKey.get().value != null);
    private double progress = 0.0;

    public FovModifier() {
        super("FOV Modifier", "Modifies FOV.", SubCategory.MISC_VISUAL, true);
        INSTANCE = this;
    }

    public static FovModifier getInstance() {
        return INSTANCE;
    }

    @Event
    public void onRender(RenderEvent.World.Pre event) {
        this.progress = MathHelper.clamp(
                this.progress + (this.zoomKey.get().isPressed() ? event.frameTime / this.zoomTime.get() : -event.frameTime / this.zoomTime.get()), 0.0, 1.0
        );
    }

    public double getFOV() {
        return MathHelper.lerp(AnimUtils.easeInOutCubic(this.progress), this.fov.get(), this.zoom.get());
    }
}
