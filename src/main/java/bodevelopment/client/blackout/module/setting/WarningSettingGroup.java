package bodevelopment.client.blackout.module.setting;

public class WarningSettingGroup extends SettingGroup {
    public final String warning;

    public WarningSettingGroup(String name, String warning) {
        super(name);
        this.warning = warning;
    }
}
