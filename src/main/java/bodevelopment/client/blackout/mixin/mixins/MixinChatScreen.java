package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.client.Notifications;
import bodevelopment.client.blackout.util.ChatUtils;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChatScreen.class)
public class MixinChatScreen {
    @Redirect(
            method = "handleChatInput",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendChat(Ljava/lang/String;)V")
    )
    private void onMessage(ClientPacketListener instance, String content) {
        if (content.startsWith(Managers.COMMANDS.prefix)) {
            String rur = Managers.COMMANDS.onCommand(content.substring(1).split(" "));
            if (rur == null) {
                ChatUtils.addMessage("Unrecognized command!");
            } else {
                ChatUtils.addMessage(Notifications.getInstance().getClientPrefix() + " " + rur);
            }
        } else {
            instance.sendChat(content);
        }
    }
}
