package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;

public class AntiPose extends Module {
    private static AntiPose INSTANCE;

    public AntiPose() {
        super("Anti Pose", "Doesn't force you in swim or crouch pose when in too small space. For 1.12.2.", SubCategory.MISC, false);
        INSTANCE = this;
    }

    public static AntiPose getInstance() {
        return INSTANCE;
    }
}
