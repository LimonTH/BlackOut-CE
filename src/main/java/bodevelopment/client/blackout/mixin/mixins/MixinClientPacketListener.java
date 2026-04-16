package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.events.GameJoinEvent;
import bodevelopment.client.blackout.event.events.PacketEvent;
import bodevelopment.client.blackout.mixin.accessors.AccessorServerboundMovePlayerPacket;
import bodevelopment.client.blackout.module.modules.misc.NoRotate;
import bodevelopment.client.blackout.module.modules.movement.Velocity;
import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import bodevelopment.client.blackout.module.modules.visual.world.Ambience;
import bodevelopment.client.blackout.util.CompatUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {
    @Unique
    private static float lastServerYaw;
    @Unique
    private static float lastServerPitch;

    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void onJoin(ClientboundLoginPacket packet, CallbackInfo ci) {
        BlackOut.EVENT_BUS.post(GameJoinEvent.get(packet));
    }

    @Redirect(
            method = "setValuesFromPositionPacket",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setYRot(F)V")
    )
    private static void rubberbandYaw(Entity entity, float yaw) {
        if (!(entity instanceof Player player)) {
            entity.setYRot(yaw);
            return;
        }

        NoRotate noRotate = NoRotate.getInstance();

        if (noRotate.enabled && noRotate.mode.get() == NoRotate.NoRotateMode.Rel) {
            noRotate.relYaw = yaw - player.getYRot();
        } else if (!noRotate.enabled) {
            player.setYRot(yaw);
        }
        lastServerYaw = yaw;
    }

    @Redirect(
            method = "setValuesFromPositionPacket",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setXRot(F)V")
    )
    private static void rubberbandPitch(Entity entity, float pitch) {
        if (!(entity instanceof Player player)) {
            entity.setXRot(pitch);
            return;
        }

        NoRotate noRotate = NoRotate.getInstance();

        if (noRotate.enabled && noRotate.mode.get() == NoRotate.NoRotateMode.Rel) {
            noRotate.relPitch = pitch - player.getXRot();
        } else if (!noRotate.enabled) {
            player.setXRot(pitch);
        }
        lastServerPitch = pitch;
    }

    @Redirect(
            method = "handleMovePlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;)V", ordinal = 1)
    )
    private void sendFull(Connection instance, Packet<?> packet) {
        if (CompatUtils.isBaritonePathing()) {
            instance.send(packet);
            return;
        }

        if (packet instanceof ServerboundMovePlayerPacket.PosRot moveC2SPacket) {
            NoRotate noRotate = NoRotate.getInstance();

            if (noRotate.enabled) {
                switch (noRotate.mode.get()) {
                    case Cancel:
                        return;
                    case Set:
                    case Spoof:
                    case Rel:
                        ((AccessorServerboundMovePlayerPacket) moveC2SPacket).setYaw(lastServerYaw);
                        ((AccessorServerboundMovePlayerPacket) moveC2SPacket).setPitch(lastServerPitch);

                        if (noRotate.mode.get() == NoRotate.NoRotateMode.Rel) {
                            if (BlackOut.mc.player != null) {
                                BlackOut.mc.player.setYRot(BlackOut.mc.player.getYRot() + noRotate.relYaw);
                                BlackOut.mc.player.setXRot(BlackOut.mc.player.getXRot() + noRotate.relPitch);
                            }
                            noRotate.relYaw = 0;
                            noRotate.relPitch = 0;
                        }
                        break;
                }
            }
            instance.send(moveC2SPacket);
        } else {
            instance.send(packet);
        }
    }

    @Redirect(
            method = "handleSetTime",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;setTimeFromServer(JJZ)V")
    )
    private void redirectSetTime(ClientLevel world, long time, long timeOfDay, boolean shouldTickTimeOfDay) {
        Ambience ambience = Ambience.getInstance();

        long finalTimeOfDay = (ambience.enabled && ambience.modifyTime.get())
                ? ambience.time.get().longValue()
                : timeOfDay;

        world.setTimeFromServer(time, finalTimeOfDay, shouldTickTimeOfDay);
    }

    @Redirect(
            method = "handleEntityEvent",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;displayItemActivation(Lnet/minecraft/world/item/ItemStack;)V")
    )
    private void showTotemAnimation(GameRenderer instance, ItemStack floatingItem) {
        NoRender noRender = NoRender.getInstance();
        if (!noRender.enabled || !noRender.totem.get()) {
            instance.displayItemActivation(floatingItem);
        }
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "handleBundlePacket", at = @At("HEAD"), cancellable = true)
    private void dispatchBundleSubPackets(ClientboundBundlePacket bundle, CallbackInfo ci) {
        for (Packet<?> subPacket : bundle.subPackets()) {
            if (BlackOut.EVENT_BUS.post(PacketEvent.Receive.Pre.get(subPacket)).isCancelled()) continue;
            if (BlackOut.EVENT_BUS.post(PacketEvent.Receive.Post.get(subPacket)).isCancelled()) continue;
            ((Packet<ClientGamePacketListener>) subPacket).handle((ClientGamePacketListener) (Object) this);
            BlackOut.EVENT_BUS.post(PacketEvent.Received.get(subPacket));
        }
        ci.cancel();
    }

    @WrapOperation(
            method = "handleExplosion",
            at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V")
    )
    private void wrapExplosionKnockback(Optional<Vec3> optional, Consumer<Vec3> consumer, Operation<Void> original) {
        Velocity velocity = Velocity.getInstance();
        if (velocity == null || !velocity.enabled || !velocity.explosions.get()) {
            original.call(optional, consumer);
            return;
        }

        switch (velocity.mode.get()) {
            case Grim:
                if (velocity.chance.get() >= ThreadLocalRandom.current().nextDouble()) {
                    velocity.sendGrimPackets();
                } else {
                    original.call(optional, consumer);
                }
                break;
            case Delayed:
                original.call(optional, consumer);
                break;

            case Simple: {
                double rand = ThreadLocalRandom.current().nextDouble();
                boolean hB = velocity.hChance.get() >= rand;
                boolean vB = velocity.vChance.get() >= rand;
                optional.ifPresent(knockback -> {
                    LocalPlayer player = BlackOut.mc.player;
                    if (player == null) return;
                    player.push(
                            hB ? knockback.x * velocity.horizontal.get() : knockback.x,
                            vB ? knockback.y * velocity.vertical.get() : knockback.y,
                            hB ? knockback.z * velocity.horizontal.get() : knockback.z
                    );
                });
                break;
            }

            case Matrix_AAC: {
                double h = Math.max(0.05, velocity.horizontal.get());
                double v = velocity.vertical.get();
                optional.ifPresent(knockback -> {
                    LocalPlayer player = BlackOut.mc.player;
                    if (player != null) player.push(knockback.x * h, knockback.y * v, knockback.z * h);
                });
                break;
            }

            case Vulcan:
                optional.ifPresent(knockback -> {
                    LocalPlayer player = BlackOut.mc.player;
                    if (player != null) player.push(0.0, knockback.y * 0.2, 0.0);
                });
                break;
        }
    }
}
