package bodevelopment.client.blackout.module.modules.client.settings;

import bodevelopment.client.blackout.module.SettingsModule;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;

public class ServerSettings extends SettingsModule {
    private static ServerSettings INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Boolean> cc = this.sgGeneral.b("CC Hitboxes", false, "Newly placed crystals require 1 block tall space without entity hitboxes.");
    public final Setting<Boolean> oldCrystals = this.sgGeneral.b("1.12.2 Crystals", false, "Requires 2 block tall space to place crystals.");
    public final Setting<Boolean> grimMovement = this.sgGeneral.b("Move Fix", false, "Uses inputs to bypass grimAC's simulation checks.");
    public final Setting<Boolean> strictSprint = this.sgGeneral
            .b("Strict Sprint", false, "Only sprints if you are walking in the same direction as you are looking.", () -> !this.grimMovement.get());
    public final Setting<Boolean> grimPackets = this.sgGeneral.b("Grim Packets", false, "Sends interaction packets before movement packets.");
    public final Setting<Boolean> grimUsing = this.sgGeneral.b("Grim Using", false, "Sends a rotation packet before interact packet.");

    public ServerSettings() {
        super("Server", false, true);
        INSTANCE = this;
    }

    public static ServerSettings getInstance() {
        return INSTANCE;
    }
}
