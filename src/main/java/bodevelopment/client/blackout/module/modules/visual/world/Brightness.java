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
    public final Setting<Mode> mode = this.sgGeneral.enumSetting("Mode", Mode.Gamma, ".", () -> true);

    public Brightness() {
        super("Brightness", "Makes the world bright", SubCategory.WORLD, true);
        INSTANCE = this;
    }

    public static Brightness getInstance() {
        return INSTANCE;
    }

    @Override
    public void onDisable() {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (this.mode.get() == Mode.Effect && BlackOut.mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
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
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (this.mode.get() == Mode.Effect) {
                BlackOut.mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 69, 0));
            }
        }
    }

    public enum Mode {
        Effect,
        Gamma
    }
}
