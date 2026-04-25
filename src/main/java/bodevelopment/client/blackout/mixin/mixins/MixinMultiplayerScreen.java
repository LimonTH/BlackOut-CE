package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.util.BlocklistUtil;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class MixinMultiplayerScreen {

    @Shadow protected abstract void join(ServerData serverData);

    @Inject(method = "join(Lnet/minecraft/client/multiplayer/ServerData;)V", at = @At("HEAD"), cancellable = true)
    private void onJoin(ServerData serverData, CallbackInfo ci) {
        if (BlocklistUtil.isBlocked(serverData.ip)) {
            ci.cancel();

            BlackOut.mc.setScreen(new ConfirmScreen(
                    (confirmed) -> {
                        if (confirmed) {
                            this.join(serverData);
                        } else {
                            BlackOut.mc.setScreen((JoinMultiplayerScreen) (Object) this);
                        }
                    },
                    Component.literal("Blocked Server Warning").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    Component.literal("The server ")
                            .append(Component.literal(serverData.ip).withStyle(ChatFormatting.GOLD))
                            .append(" is on the Mojang Blocklist.\n\n")
                            .append("This usually means it violated the EULA (monetization rules).\n")
                            .append("Do you want to connect anyway?"),
                    Component.literal("Connect Anyway"),
                    Component.literal("Cancel")
            ));
        }
    }
}