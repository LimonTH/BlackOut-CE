package bodevelopment.client.blackout.event.events;

import bodevelopment.client.blackout.event.Cancellable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

public class InteractBlockEvent extends Cancellable {
    private static final InteractBlockEvent INSTANCE = new InteractBlockEvent();
    public BlockHitResult hitResult = null;
    public InteractionHand hand = null;

    public static InteractBlockEvent get(BlockHitResult hitResult, InteractionHand hand) {
        INSTANCE.hitResult = hitResult;
        INSTANCE.hand = hand;
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }
}
