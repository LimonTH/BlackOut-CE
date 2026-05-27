package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.util.BlocklistUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public class MixinJoinMultiplayerScreen {

    @Inject(method = "join(Lnet/minecraft/client/multiplayer/ServerData;)V", at = @At("HEAD"), cancellable = true)
    private void onJoin(ServerData serverData, CallbackInfo ci) {
        if (BlocklistUtil.isBlocked(serverData.ip)) {
            ci.cancel();

            MutableComponent message = Component.literal("")
                    .append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("BlackOut").withStyle(ChatFormatting.RED))
                    .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("Blocklist detected!").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("\n\n"))
                    .append(Component.literal(serverData.ip).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
                    .append(Component.literal("\n\nThis server is on the Mojang Blocklist.").withStyle(ChatFormatting.RED))
                    .append(Component.literal("\nPossible EULA violation.\n").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC))
                    .append(Component.literal("\nDo you want to connect anyway?").withStyle(ChatFormatting.WHITE));

            BlackOut.mc.setScreen(new ConfirmScreen(
                    (confirmed) -> {
                        if (confirmed) {
                            ConnectScreen.startConnecting(
                                    (JoinMultiplayerScreen) (Object) this,
                                    BlackOut.mc,
                                    ServerAddress.parseString(serverData.ip),
                                    serverData,
                                    false,
                                    null
                            );
                        } else {
                            BlackOut.mc.setScreen((JoinMultiplayerScreen) (Object) this);
                        }
                    },
                    Component.literal("It Is Blocked Server").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    message,
                    Component.literal("Connect").withStyle(ChatFormatting.WHITE),
                    Component.literal("Cancel").withStyle(ChatFormatting.WHITE)
            ));
        }
    }
}
