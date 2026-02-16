package bodevelopment.client.blackout.module.modules.visual.world;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.FogShape;

public class Ambience extends Module {
    private static Ambience INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Boolean> modifyWeather = this.sgGeneral.b("Modify Weather", true, "Changes rain and thunder values.");
    public final Setting<Double> raining = this.sgGeneral
            .d("Rain", 0.0, 0.0, 10.0, 0.25, "Rain gradient. 1 = raining, 0 = not raining.", this.modifyWeather::get);
    public final Setting<Double> thunder = this.sgGeneral
            .d("Thunder", 0.0, 0.0, 10.0, 0.25, "Thunder gradient. 1 = thundering, 0 = not thundering.", this.modifyWeather::get);
    public final Setting<Boolean> modifyTime = this.sgGeneral.b("Modify Time", true, "Changes world time.");
    public final Setting<Integer> time = this.sgGeneral.i("Time", 2000, 0, 24000, 50, ".", this.modifyTime::get);
    private final SettingGroup sgFog = this.addGroup("Fog");
    public final Setting<Boolean> modifyFog = this.sgFog.b("Modify Fog", true, "Changes fog.");
    public final Setting<Boolean> removeFog = this.sgFog.b("Remove Fog", true, "Removes fog.", this.modifyFog::get);
    private final Setting<FogShape> shape = this.sgFog
            .e("Fog Shape", FogShape.SPHERE, "Fog shape.", () -> this.modifyFog.get() && !this.removeFog.get());
    private final Setting<Double> distance = this.sgFog
            .d("Fog Distance", 25.0, 0.0, 100.0, 1.0, "How far away should the fog start rendering.", () -> this.modifyFog.get() && !this.removeFog.get());
    private final Setting<Double> fading = this.sgFog
            .d("Fog Fading", 25.0, 0.0, 250.0, 1.0, "How smoothly should the fog fade.", () -> this.modifyFog.get() && !this.removeFog.get());
    public final Setting<BlackOutColor> color = this.sgFog
            .c("Fog Color", new BlackOutColor(255, 0, 0, 255), "Color of the fog.", () -> this.modifyFog.get() && !this.removeFog.get());
    public final Setting<Boolean> thickFog = this.sgFog.b("Thick Fog", true, "Makes the fog extremely thick.", this.modifyFog::get);

    public Ambience() {
        super("Ambience", ".", SubCategory.WORLD, true);
        INSTANCE = this;
    }

    public static Ambience getInstance() {
        return INSTANCE;
    }

    public boolean modifyFog(boolean terrain) {
        if (!terrain && !this.thickFog.get()) {
            return false;
        } else if (!this.modifyFog.get()) {
            return false;
        } else if (this.removeFog.get()) {
            RenderSystem.setShaderFogColor(0.0F, 0.0F, 0.0F, 0.0F);
            return true;
        } else {
            RenderSystem.setShaderFogColor(
                    this.color.get().red / 255.0F, this.color.get().green / 255.0F, this.color.get().blue / 255.0F, this.color.get().alpha / 255.0F
            );
            RenderSystem.setShaderFogStart(this.distance.get().floatValue());
            RenderSystem.setShaderFogEnd(this.distance.get().floatValue() + this.fading.get().floatValue());
            RenderSystem.setShaderFogShape(this.shape.get());
            return true;
        }
    }
}
