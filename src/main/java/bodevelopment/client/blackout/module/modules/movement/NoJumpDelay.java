package bodevelopment.client.blackout.module.modules.movement;

import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;

public class NoJumpDelay extends Module {
    private static NoJumpDelay INSTANCE;

    public NoJumpDelay() {
        super("No Jump Delay", "Removes wait time between jumps.", SubCategory.MOVEMENT, false);
        INSTANCE = this;
    }

    public static NoJumpDelay getInstance() {
        return INSTANCE;
    }
}
