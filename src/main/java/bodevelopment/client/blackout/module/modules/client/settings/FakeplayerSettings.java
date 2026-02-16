package bodevelopment.client.blackout.module.modules.client.settings;

import bodevelopment.client.blackout.module.SettingsModule;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class FakeplayerSettings extends SettingsModule {
    private static FakeplayerSettings INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<String> fakePlayerName = this.sgGeneral.s("Fake Player Name", "default", "Fake Player display name.");
    public final Setting<Double> damageMultiplier = this.sgGeneral.d("Damage Multiplier", 1.0, 0.0, 5.0, 0.05, ".");
    public final Setting<Boolean> unlimitedTotems = this.sgGeneral.b("Unlimited Totems", true, ".");
    public final Setting<Integer> totems = this.sgGeneral.i("Totems", 10, 0, 20, 1, ".", () -> !this.unlimitedTotems.get());
    public final Setting<Integer> swapDelay = this.sgGeneral.i("Swap Delay", 0, 0, 20, 1, ".", () -> this.unlimitedTotems.get() || this.totems.get() > 0);
    public final Setting<Boolean> eating = this.sgGeneral.b("Eating", true, ".");
    public final Setting<Integer> eatTime = this.sgGeneral.i("Eat Time", 10, 0, 20, 1, ".", this.eating::get);

    public FakeplayerSettings() {
        super("Fake Player", false, false);
        INSTANCE = this;
    }

    public static FakeplayerSettings getInstance() {
        return INSTANCE;
    }
}
