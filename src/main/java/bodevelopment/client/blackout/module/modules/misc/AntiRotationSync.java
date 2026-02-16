package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;

public class AntiRotationSync extends Module {
    private static AntiRotationSync INSTANCE;

    public AntiRotationSync() {
        super("Anti Rotation Sync", "Doesn't send movement packets when using items.", SubCategory.MISC, true);
        INSTANCE = this;
    }

    public static AntiRotationSync getInstance() {
        return INSTANCE;
    }
}
