package bodevelopment.client.blackout.module.modules.combat.misc;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;

public class NoInterpolation extends Module {
    private static NoInterpolation INSTANCE;

    public NoInterpolation() {
        super("No Interpolation", "Disables smooth movement for other players to show their exact server-side position.", SubCategory.MISC_COMBAT, true);
        INSTANCE = this;
    }

    public static NoInterpolation getInstance() {
        return INSTANCE;
    }
}
