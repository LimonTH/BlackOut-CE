package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.misc.Pause;
import bodevelopment.client.blackout.module.modules.movement.Blink;
import bodevelopment.client.blackout.randomstuff.Pair;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
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
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

@Mixin(Connection.class)
public abstract class MixinClientConnection {
    @Shadow
    @Final
    private static Logger LOGGER;
    @Shadow
    @Final
    private PacketFlow receiving;
    @Shadow
    private Channel channel;
    @Shadow
    private int sentPackets;
    @Unique
    private volatile Packet<?> currentPacket = null;
    @Unique
    private volatile boolean cancelled = false;

    @Inject(method = "genericsFtw", at = @At("HEAD"), cancellable = true)
    private static void preReceivePacket(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        BlackOut.EVENT_BUS.post(PacketEvent.Receive.Pre.get(packet));
        if (BlackOut.EVENT_BUS.post(PacketEvent.Receive.Post.get(packet)).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "genericsFtw", at = @At("TAIL"))
    private static void postReceivePacket(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        BlackOut.EVENT_BUS.post(PacketEvent.Received.get(packet));
    }

    @Shadow
    protected abstract void doSendPacket(Packet<?> packet, @Nullable PacketSendListener callbacks, boolean flush);

    @Shadow
    protected abstract void channelRead0(ChannelHandlerContext context, Packet<?> packet);

    @Inject(method = "exceptionCaught", at = @At("HEAD"))
    private void onException(ChannelHandlerContext context, Throwable ex, CallbackInfo ci) {
        LOGGER.warn("Crashed on packet event ", ex);
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V", at = @At("HEAD"), cancellable = true)
    private void preSendPacket(Packet<?> packet, PacketSendListener callbacks, CallbackInfo ci) {
        this.cancelled = BlackOut.EVENT_BUS.post(PacketEvent.Send.get(packet)).isCancelled();
        if (this.cancelled) {
            ci.cancel();
        }
    }

    @Inject(method = "sendPacket", at = @At("HEAD"))
    public void sendHead(Packet<?> packet, PacketSendListener callbacks, boolean flush, CallbackInfo ci) {
        this.currentPacket = packet;
    }

    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void sendPing(Packet<?> packet, PacketSendListener callbacks, boolean flush, CallbackInfo ci) {
        if (this.channel == null || !this.channel.isOpen()) return;
        this.sentPackets++;

        boolean shouldDelay = Managers.PING.shouldDelay(packet);

        if (this.channel.eventLoop().inEventLoop()) {
            if (shouldDelay) {
                Managers.PING.addSend(() -> this.doSendPacket(packet, callbacks, flush));
                ci.cancel();
            }
        } else {
            if (shouldDelay) {
                Managers.PING.addSend(() -> this.channel.eventLoop().execute(() ->
                        this.doSendPacket(packet, callbacks, flush)
                ));
                ci.cancel();
            }
        }
    }

    @Redirect(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;isConnected()Z")
    )
    private boolean isOpenSend(Connection instance) {
        Blink blink = Blink.getInstance();
        return (!blink.enabled || !blink.onSend()) && instance.isConnected();
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void preReceive(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        Pause pause = Pause.getInstance();
        List<Pair<ChannelHandlerContext, Packet<?>>> packets = pause.packets;
        if (pause.enabled) {
            packets.add(new Pair<>(channelHandlerContext, packet));
            ci.cancel();
        } else if (!pause.emptying && !packets.isEmpty()) {
            pause.emptying = true;
            packets.forEach(pair -> this.channelRead0(pair.getA(), pair.getB()));
            packets.clear();
            pause.emptying = false;
        }
    }

    @Redirect(method = "flushChannel", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;isConnected()Z"))
    private boolean isOpenFlush(Connection instance) {
        Blink blink = Blink.getInstance();
        return (!blink.enabled || !blink.shouldDelay()) && instance.isConnected();
    }

    @Redirect(method = "flushQueue", at = @At(value = "FIELD", target = "Lnet/minecraft/network/Connection;channel:Lio/netty/channel/Channel;", opcode = Opcodes.GETFIELD))
    private Channel isChannelOpen(Connection instance) {
        Blink blink = Blink.getInstance();
        return blink.enabled && blink.shouldDelay() ? null : this.channel;
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V", at = @At("TAIL"))
    private void postSendPacket(Packet<?> packet, PacketSendListener callbacks, CallbackInfo ci) {
        if (!this.cancelled) {
            BlackOut.EVENT_BUS.post(PacketEvent.Sent.get(packet));
        }

        this.cancelled = false;
    }
}
