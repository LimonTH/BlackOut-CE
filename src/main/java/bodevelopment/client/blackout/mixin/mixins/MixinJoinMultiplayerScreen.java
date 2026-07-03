/*
 * Blackout Client (CE) - A cutting-edge, feature-rich cheat client for Minecraft.
 * A modernized continuation of the original Blackout project by OLEPOSSU & KassuK.
 * Copyright (C) 2026  LimonTH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.Internal;
import bodevelopment.client.blackout.util.BlocklistUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
@Internal
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
