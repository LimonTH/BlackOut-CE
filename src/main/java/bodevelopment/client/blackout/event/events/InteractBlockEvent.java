package bodevelopment.client.blackout.event.events;

import bodevelopment.client.blackout.event.Cancellable;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

public class InteractBlockEvent extends Cancellable {
    private static final InteractBlockEvent INSTANCE = new InteractBlockEvent();
    public BlockHitResult hitResult = null;
    public Hand hand = null;

    public static InteractBlockEvent get(BlockHitResult hitResult, Hand hand) {
        INSTANCE.hitResult = hitResult;
        INSTANCE.hand = hand;
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }
}
