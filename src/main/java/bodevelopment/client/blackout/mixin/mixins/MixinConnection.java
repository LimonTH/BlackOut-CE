package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.Internal;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.misc.Pause;
import bodevelopment.client.blackout.module.modules.movement.Blink;
import bodevelopment.client.blackout.randomstuff.Pair;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Connection.class)
@Internal
public abstract class MixinConnection {
    @Shadow
    @Final
    private static Logger LOGGER;
    @Shadow
    @Final
    private PacketFlow receiving;
    @Shadow
    private Channel channel;
    @Unique
    private volatile Packet<?> currentPacket = null;
    @Unique
    private final ThreadLocal<Boolean> cancelled = ThreadLocal.withInitial(() -> false);

    @Inject(method = "genericsFtw", at = @At("HEAD"), cancellable = true)
    private static void preReceivePacket(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        if (BlackOut.EVENT_BUS.post(PacketEvent.Receive.Pre.get(packet)).isCancelled()) {
            ci.cancel();
            return;
        }

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
        // ClosedChannelException and its subclass StacklessClosedChannelException are
        // normal during disconnect/shutdown — the channel is already closed and pending
        // writes fail. Do not spam the log with these.
        if (ex instanceof java.nio.channels.ClosedChannelException) return;
        LOGGER.error("Connection protocol error — client state may be desynchronized", ex);
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V", at = @At("HEAD"), cancellable = true)
    private void preSendPacket(Packet<?> packet, PacketSendListener callbacks, CallbackInfo ci) {
        boolean isCancelled = BlackOut.EVENT_BUS.post(PacketEvent.Send.get(packet)).isCancelled();
        this.cancelled.set(isCancelled);
        if (isCancelled) {
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

        if (Managers.PING.shouldDelay(packet)) {
            Managers.PING.addSend(() -> this.channel.eventLoop().execute(() ->
                    this.doSendPacket(packet, callbacks, flush)
            ));
            ci.cancel();
        }
    }

    @WrapOperation(
            method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;isConnected()Z")
    )
    private boolean wrapIsConnectedSend(Connection instance, Operation<Boolean> original) {
        Blink blink = Blink.getInstance();
        if (blink.enabled && blink.onSend()) return false;
        return original.call(instance);
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

    @WrapOperation(method = "flushChannel", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;isConnected()Z"))
    private boolean wrapIsConnectedFlush(Connection instance, Operation<Boolean> original) {
        Blink blink = Blink.getInstance();
        if (blink.enabled && blink.shouldDelay()) return false;
        return original.call(instance);
    }

    @WrapOperation(method = "flushQueue", at = @At(value = "FIELD", target = "Lnet/minecraft/network/Connection;channel:Lio/netty/channel/Channel;", opcode = Opcodes.GETFIELD))
    private Channel wrapChannelAccess(Connection instance, Operation<Channel> original) {
        Blink blink = Blink.getInstance();
        if (blink.enabled && blink.shouldDelay()) return null;
        return original.call(instance);
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V", at = @At("TAIL"))
    private void postSendPacket(Packet<?> packet, PacketSendListener callbacks, CallbackInfo ci) {
        if (!this.cancelled.get()) {
            BlackOut.EVENT_BUS.post(PacketEvent.Sent.get(packet));
        }

        this.cancelled.remove();
    }
}
