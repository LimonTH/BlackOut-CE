package bodevelopment.client.blackout.module.modules.visual.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;

public class NoBobbing extends Module {
    private static NoBobbing INSTANCE;

    public NoBobbing() {
        super("No Bobbing", "Prevents the camera from bobbing while moving.", SubCategory.MISC_VISUAL, true);
        INSTANCE = this;
    }

    public static NoBobbing getInstance() {
        return INSTANCE;
    }
}