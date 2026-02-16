package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;

public class MultiTask extends Module {
    private static MultiTask INSTANCE;

    public MultiTask() {
        super("Multi Task", "Allows you to mine while eating.", SubCategory.MISC_COMBAT, false);
        INSTANCE = this;
    }

    public static MultiTask getInstance() {
        return INSTANCE;
    }
}
