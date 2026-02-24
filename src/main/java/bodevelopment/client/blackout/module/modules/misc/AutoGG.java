package bodevelopment.client.blackout.module.modules.misc;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.util.ChatUtils;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

public class AutoGG extends Module {
    public AutoGG() {
        super("Auto GG", "Automatically sends a congratulatory message in chat upon the conclusion of a match or after a death.", SubCategory.MISC, true);
    }

    @Event
    public void onReceive(PacketEvent.Receive.Pre event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            if (event.packet instanceof GameMessageS2CPacket packet) {
                String unformattedText = packet.content().getString();
                String[] look = new String[]{
                        "You won! Want to play again? Click here! ", "You lost! Want to play again? Click here! ", "You died! Want to play again? Click here! "
                };
                if (unformattedText == null) {
                    return;
                }

                for (String s : look) {
                    if (unformattedText.contains(s)) {
                        ChatUtils.sendMessage("gg");
                    }
                }
            }
        }
    }
}
