package bodevelopment.client.blackout.module.modules.client.settings;

import bodevelopment.client.blackout.module.SettingsModule;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class FakeplayerSettings extends SettingsModule {
    private static FakeplayerSettings INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<String> fakePlayerName = this.sgGeneral.stringSetting("Fake Player Name", "default",
            "The name that will be displayed above the fake player's head and in combat logs.");

    public final Setting<Double> damageMultiplier = this.sgGeneral.doubleSetting("Damage Multiplier", 1.0, 0.0, 5.0, 0.05,
            "Scales the damage received by the fake player. Useful for simulating targets with different armor or resistance levels.");

    public final Setting<Boolean> unlimitedTotems = this.sgGeneral.booleanSetting("Unlimited Totems", true,
            "If enabled, the fake player will never run out of totems, allowing for infinite combat testing.");

    public final Setting<Integer> totems = this.sgGeneral.intSetting("Totems", 10, 0, 20, 1,
            "The specific amount of totems the fake player has before it finally 'dies'.", () -> !this.unlimitedTotems.get());

    public final Setting<Integer> swapDelay = this.sgGeneral.intSetting("Swap Delay", 0, 0, 20, 1,
            "The delay (in ticks) before the fake player pops a new totem. Set to 0 for instant 'immortal' behavior.", () -> this.unlimitedTotems.get() || this.totems.get() > 0);

    public final Setting<Boolean> eating = this.sgGeneral.booleanSetting("Eating", true,
            "Simulates the fake player eating golden apples or food, making it look more realistic during testing.");

    public final Setting<Integer> eatTime = this.sgGeneral.intSetting("Eat Time", 10, 0, 20, 1,
            "How many ticks it takes for the fake player to finish eating.", this.eating::get);

    public FakeplayerSettings() {
        super("Fake Player", false, false);
        INSTANCE = this;
    }

    public static FakeplayerSettings getInstance() {
        return INSTANCE;
    }
}
