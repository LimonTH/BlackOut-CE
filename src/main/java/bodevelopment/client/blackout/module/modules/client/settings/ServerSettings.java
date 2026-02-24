package bodevelopment.client.blackout.module.modules.client.settings;

import bodevelopment.client.blackout.module.SettingsModule;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class ServerSettings extends SettingsModule {
    private static ServerSettings INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Boolean> cc = this.sgGeneral.b("CC Hitboxes", false,
            "Calculates crystal placement by ensuring there is a 1-block tall space free of any entity hitboxes, preventing placement failures on modern servers.");
    public final Setting<Boolean> oldCrystals = this.sgGeneral.b("1.12.2 Crystals", false,
            "Enforces the legacy 1.12.2 placement rule which requires a 2-block high air gap to place End Crystals.");
    public final Setting<Boolean> grimMovement = this.sgGeneral.b("Move Fix", false,
            "Synchronizes your movement packets with actual player inputs to satisfy GrimAC's strict motion simulation and prediction checks.");
    public final Setting<Boolean> strictSprint = this.sgGeneral.b("Strict Sprint", false,
            "Only allows the sprint state to be active when your movement vector closely matches your look direction to bypass directional sprint checks.",
            () -> !this.grimMovement.get());
    public final Setting<Boolean> grimPackets = this.sgGeneral.b("Grim Packets", false,
            "Modifies packet order to send interaction packets before movement updates, helping with action-sequence validation on GrimAC.");
    public final Setting<Boolean> grimUsing = this.sgGeneral.b("Grim Using", false,
            "Injects a dedicated rotation packet immediately before any interaction (like using an item) to ensure the server sees you looking at the target on that specific frame.");
    public ServerSettings() {
        super("Server", false, true);
        INSTANCE = this;
    }

    public static ServerSettings getInstance() {
        return INSTANCE;
    }
}
