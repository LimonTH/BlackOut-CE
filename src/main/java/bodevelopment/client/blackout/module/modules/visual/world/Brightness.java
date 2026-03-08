package bodevelopment.client.blackout.module.modules.visual.world;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class Brightness extends Module {
    private static Brightness INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Mode> mode = this.sgGeneral.enumSetting("Illumination Mode", Mode.Gamma, "The technical approach used to enhance visibility.");
    public final Setting<Integer> luminanceLevel = this.sgGeneral.intSetting("Luminance Level", 8, 0, 15, 1, "Brightness level for blocks in Luminance mode.", () -> this.mode.get() == Mode.Luminance);

    public static int luminanceValue = 0;

    public Brightness() {
        super("Brightness", "Provides artificial illumination via Gamma override, Status Effects, or Lightmap Luminance.", SubCategory.WORLD, true);
        INSTANCE = this;
    }

    public static Brightness getInstance() {
        return INSTANCE;
    }

    @Override
    public void onDisable() {
        if (BlackOut.mc.player != null) {
            if (BlackOut.mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                BlackOut.mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
        }
    }

    @Override
    public String getInfo() {
        return this.mode.get().name();
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (BlackOut.mc.player == null || BlackOut.mc.world == null) return;

        luminanceValue = (this.luminanceLevel.get() * 16) << 16;
        if (this.mode.get() == Mode.Effect) {
            BlackOut.mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, StatusEffectInstance.INFINITE, StatusEffectInstance.MAX_AMPLIFIER, false, false));
        } else {
            if (BlackOut.mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                BlackOut.mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
        }
    }

    public enum Mode {
        Effect,
        Gamma,
        Luminance
    }
}