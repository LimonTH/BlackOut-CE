package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.OLEPOSSUtils;

public class AntiSpam extends Module {
    private static AntiSpam INSTANCE;
    public final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<Double> similarity = this.sgGeneral.doubleSetting("Similarity", 0.9, 0.0, 1.0, 0.01, ".");

    public AntiSpam() {
        super("Anti Spam", "Stacks similar messages.", SubCategory.MISC, false);
        INSTANCE = this;
    }

    public static AntiSpam getInstance() {
        return INSTANCE;
    }

    public boolean isSimilar(String string1, String string2) {
        return OLEPOSSUtils.similarity(string1, string2) >= this.similarity.get();
    }
}
