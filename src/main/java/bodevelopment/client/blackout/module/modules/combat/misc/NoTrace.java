package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;

public class NoTrace extends Module {
    private static NoTrace INSTANCE;

    public NoTrace() {
        super("No Trace", "Allows you to click blocks behind entities.", SubCategory.MISC_COMBAT, false);
        INSTANCE = this;
    }

    public static NoTrace getInstance() {
        return INSTANCE;
    }
}
