package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;

public class AntiPose extends Module {
    private static AntiPose INSTANCE;

    public AntiPose() {
        super("Anti Pose", "Prevents the server from forcing specific player states, such as crouching or swimming, when moving through confined spaces. (for v1.12.2)", SubCategory.MISC, false);
        INSTANCE = this;
    }

    public static AntiPose getInstance() {
        return INSTANCE;
    }
}
