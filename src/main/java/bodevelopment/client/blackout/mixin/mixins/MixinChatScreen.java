package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.util.ChatUtils;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChatScreen.class)
public class MixinChatScreen {
    @Redirect(
            method = "sendMessage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendChatMessage(Ljava/lang/String;)V")
    )
    private void onMessage(ClientPlayNetworkHandler instance, String content) {
        if (content.startsWith(Managers.COMMANDS.prefix)) {
            String rur = Managers.COMMANDS.onCommand(content.substring(1).split(" "));
            if (rur == null) {
                ChatUtils.addMessage("Unrecognized command!");
            } else {
                ChatUtils.addMessage(Notifications.getInstance().getClientPrefix() + " " + rur);
            }
        } else {
            instance.sendChatMessage(content);
        }
    }
}
