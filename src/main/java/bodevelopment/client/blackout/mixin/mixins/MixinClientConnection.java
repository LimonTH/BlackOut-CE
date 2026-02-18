package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.misc.Pause;
import bodevelopment.client.blackout.module.modules.movement.Blink;
import bodevelopment.client.blackout.randomstuff.Pair;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ClientConnection.class)
public abstract class MixinClientConnection {
    @Shadow
    @Final
    private static Logger LOGGER;
    @Shadow
    @Final
    private NetworkSide side;
    @Shadow
    private Channel channel;
    @Shadow
    private int packetsSentCounter;
    @Unique
    private volatile Packet<?> currentPacket = null;
    @Unique
    private volatile boolean cancelled = false;

    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static void preReceivePacket(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        BlackOut.EVENT_BUS.post(PacketEvent.Receive.Pre.get(packet));
        if (BlackOut.EVENT_BUS.post(PacketEvent.Receive.Post.get(packet)).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "handlePacket", at = @At("TAIL"))
    private static void postReceivePacket(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        BlackOut.EVENT_BUS.post(PacketEvent.Received.get(packet));
    }

    @Shadow
    protected abstract void sendInternal(Packet<?> packet, @Nullable PacketCallbacks callbacks, boolean flush);

    @Shadow
    protected abstract void channelRead0(ChannelHandlerContext context, Packet<?> packet);

    @Inject(method = "exceptionCaught", at = @At("HEAD"))
    private void onException(ChannelHandlerContext context, Throwable ex, CallbackInfo ci) {
        LOGGER.warn("Crashed on packet event ", ex);
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V", at = @At("HEAD"), cancellable = true)
    private void preSendPacket(Packet<?> packet, PacketCallbacks callbacks, CallbackInfo ci) {
        this.cancelled = BlackOut.EVENT_BUS.post(PacketEvent.Send.get(packet)).isCancelled();
        if (this.cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "sendImmediately", at = @At("HEAD"))
    public void sendHead(Packet<?> packet, PacketCallbacks callbacks, boolean flush, CallbackInfo ci) {
        this.currentPacket = packet;
    }

    @Inject(method = "sendImmediately", at = @At("HEAD"), cancellable = true)
    private void sendPing(Packet<?> packet, PacketCallbacks callbacks, boolean flush, CallbackInfo ci) {
        if (this.channel == null || !this.channel.isOpen()) return;
        this.packetsSentCounter++;

        boolean shouldDelay = Managers.PING.shouldDelay(packet);

        if (this.channel.eventLoop().inEventLoop()) {
            if (shouldDelay) {
                Managers.PING.addSend(() -> this.sendInternal(packet, callbacks, flush));
                ci.cancel();
            }
        } else {
            if (shouldDelay) {
                Managers.PING.addSend(() -> this.channel.eventLoop().execute(() ->
                        this.sendInternal(packet, callbacks, flush)
                ));
                ci.cancel();
            }
        }
    }

    @Redirect(
            method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;isOpen()Z")
    )
    private boolean isOpenSend(ClientConnection instance) {
        Blink blink = Blink.getInstance();
        return (!blink.enabled || !blink.onSend()) && instance.isOpen();
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void preReceive(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        Pause pause = Pause.getInstance();
        List<Pair<ChannelHandlerContext, Packet<?>>> packets = pause.packets;
        if (pause.enabled) {
            packets.add(new Pair<>(channelHandlerContext, packet));
            ci.cancel();
        } else if (!pause.emptying && !packets.isEmpty()) {
            pause.emptying = true;
            packets.forEach(pair -> this.channelRead0(pair.getLeft(), pair.getRight()));
            packets.clear();
            pause.emptying = false;
        }
    }

    @Redirect(method = "flush", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;isOpen()Z"))
    private boolean isOpenFlush(ClientConnection instance) {
        Blink blink = Blink.getInstance();
        return (!blink.enabled || !blink.shouldDelay()) && instance.isOpen();
    }

    @Redirect(method = "handleQueuedTasks", at = @At(value = "FIELD", target = "Lnet/minecraft/network/ClientConnection;channel:Lio/netty/channel/Channel;", opcode = Opcodes.GETFIELD))
    private Channel isChannelOpen(ClientConnection instance) {
        Blink blink = Blink.getInstance();
        return blink.enabled && blink.shouldDelay() ? null : this.channel;
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V", at = @At("TAIL"))
    private void postSendPacket(Packet<?> packet, PacketCallbacks callbacks, CallbackInfo ci) {
        if (!this.cancelled) {
            BlackOut.EVENT_BUS.post(PacketEvent.Sent.get(packet));
        }

        this.cancelled = false;
    }
}
