package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;

public class NoJumpDelay extends Module {
    private static NoJumpDelay INSTANCE;

    public NoJumpDelay() {
        super("No Jump Delay", "Eliminates the internal exhaustion cooldown between consecutive jumps, allowing for immediate lift-off upon landing.", SubCategory.MOVEMENT, false);
        INSTANCE = this;
    }

    public static NoJumpDelay getInstance() {
        return INSTANCE;
    }
}
