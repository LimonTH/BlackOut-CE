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
    private final SettingGroup sgFog = this.addGroup("Fog");

    public final Setting<Boolean> modifyWeather = this.sgGeneral.booleanSetting("Weather Control", true, "Enables manipulation of atmospheric precipitation and storm intensity.");
    public final Setting<Double> raining = this.sgGeneral.doubleSetting("Precipitation Gradient", 0.0, 0.0, 10.0, 0.25, "The intensity of the rain effect. Values above 1.0 may cause visual artifacts.", this.modifyWeather::get);
    public final Setting<Double> thunder = this.sgGeneral.doubleSetting("Storm Gradient", 0.0, 0.0, 10.0, 0.25, "The intensity of the thunder and sky darkening effects.", this.modifyWeather::get);
    public final Setting<Boolean> modifyTime = this.sgGeneral.booleanSetting("Temporal Control", true, "Overrides the server-side world time with a static client-side value.");
    public final Setting<Integer> time = this.sgGeneral.intSetting("World Time", 2000, 0, 24000, 50, "The fixed time of day in ticks (0 = Dawn, 6000 = Midday, 18000 = Midnight).", this.modifyTime::get);

    public final Setting<Boolean> modifyFog = this.sgFog.booleanSetting("Fog Manipulation", true, "Enables custom rendering of the atmospheric fog layer.");
    public final Setting<Boolean> removeFog = this.sgFog.booleanSetting("Suppress Fog", true, "Completely disables the fog shader for maximum visibility.", this.modifyFog::get);
    private final Setting<FogShape> shape = this.sgFog.enumSetting("Fog Geometry", FogShape.SPHERE, "The mathematical projection used to calculate fog density (Sphere or Cylinder).", () -> this.modifyFog.get() && !this.removeFog.get());
    private final Setting<Double> distance = this.sgFog.doubleSetting("Fog Offset", 25.0, 0.0, 100.0, 1.0, "The distance from the camera at which the fog begins to obstruct vision.", () -> this.modifyFog.get() && !this.removeFog.get());
    private final Setting<Double> fading = this.sgFog.doubleSetting("Fog Falloff", 25.0, 0.0, 250.0, 1.0, "The distance over which the fog transitions from transparent to fully opaque.", () -> this.modifyFog.get() && !this.removeFog.get());
    public final Setting<BlackOutColor> color = this.sgFog.colorSetting("Fog Color", new BlackOutColor(255, 0, 0, 255), "The color applied to the atmospheric fog layer.", () -> this.modifyFog.get() && !this.removeFog.get());
    public final Setting<Boolean> thickFog = this.sgFog.booleanSetting("High Density", true, "Forces fog rendering even in conditions where it would normally be minimal.", this.modifyFog::get);

    public Ambience() {
        super("Ambience", "Customizes global environmental aesthetics including weather gradients, world time, and atmospheric fog parameters.", SubCategory.WORLD, true);
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
