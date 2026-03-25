package bodevelopment.client.blackout.module.modules.legit;

import bodevelopment.client.blackout.util.PlayerUtils;
import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;

public class JumpReset extends Module {

    public JumpReset() {
        super("Jump Reset", "Reduces incoming horizontal knockback by automatically jumping the moment damage is received.", SubCategory.LEGIT, true);
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (PlayerUtils.isInGame() && BlackOut.mc.player.hurtTime > 1 && BlackOut.mc.player.onGround()) {
            BlackOut.mc.player.jumpFromGround();
        }
    }
}
