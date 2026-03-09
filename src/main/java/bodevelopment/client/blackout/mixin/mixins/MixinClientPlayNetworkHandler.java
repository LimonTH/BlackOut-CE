package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.events.GameJoinEvent;
import bodevelopment.client.blackout.mixin.accessors.AccessorPlayerMoveC2SPacket;
import bodevelopment.client.blackout.module.modules.misc.NoRotate;
import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import bodevelopment.client.blackout.module.modules.visual.world.Ambience;
import bodevelopment.client.blackout.util.CompatUtils;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
    @Unique
    private static float lastServerYaw;
    @Unique
    private static float lastServerPitch;

    @Inject(method = "onGameJoin", at = @At("TAIL"))
    private void onJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        BlackOut.EVENT_BUS.post(GameJoinEvent.get(packet));
    }

    @Redirect(
            method = "setPosition",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setYaw(F)V")
    )
    private static void rubberbandYaw(Entity entity, float yaw) {
        if (!(entity instanceof PlayerEntity player)) {
            entity.setYaw(yaw);
            return;
        }

        NoRotate noRotate = NoRotate.getInstance();

        if (noRotate.enabled && noRotate.mode.get() == NoRotate.NoRotateMode.Rel) {
            noRotate.relYaw = yaw - player.getYaw();
        } else if (!noRotate.enabled) {
            player.setYaw(yaw);
        }
        lastServerYaw = yaw;
    }

    @Redirect(
            method = "setPosition",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setPitch(F)V")
    )
    private static void rubberbandPitch(Entity entity, float pitch) {
        if (!(entity instanceof PlayerEntity player)) {
            entity.setPitch(pitch);
            return;
        }

        NoRotate noRotate = NoRotate.getInstance();

        if (noRotate.enabled && noRotate.mode.get() == NoRotate.NoRotateMode.Rel) {
            noRotate.relPitch = pitch - player.getPitch();
        } else if (!noRotate.enabled) {
            player.setPitch(pitch);
        }
        lastServerPitch = pitch;
    }

    @Redirect(
            method = "onPlayerPositionLook",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;send(Lnet/minecraft/network/packet/Packet;)V", ordinal = 1)
    )
    private void sendFull(ClientConnection instance, Packet<?> packet) {
        if (CompatUtils.isBaritonePathing()) {
            instance.send(packet);
            return;
        }

        if (packet instanceof PlayerMoveC2SPacket.Full moveC2SPacket) {
            NoRotate noRotate = NoRotate.getInstance();

            if (noRotate.enabled) {
                switch (noRotate.mode.get()) {
                    case Cancel:
                        return;
                    case Set:
                    case Spoof:
                    case Rel:
                        ((AccessorPlayerMoveC2SPacket) moveC2SPacket).setYaw(lastServerYaw);
                        ((AccessorPlayerMoveC2SPacket) moveC2SPacket).setPitch(lastServerPitch);

                        if (noRotate.mode.get() == NoRotate.NoRotateMode.Rel) {
                            if (BlackOut.mc.player != null) {
                                BlackOut.mc.player.setYaw(BlackOut.mc.player.getYaw() + noRotate.relYaw);
                                BlackOut.mc.player.setPitch(BlackOut.mc.player.getPitch() + noRotate.relPitch);
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
            method = "onWorldTimeUpdate",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;setTime(JJZ)V")
    )
    private void redirectSetTime(ClientWorld world, long time, long timeOfDay, boolean shouldTickTimeOfDay) {
        Ambience ambience = Ambience.getInstance();

        long finalTimeOfDay = (ambience.enabled && ambience.modifyTime.get())
                ? ambience.time.get().longValue()
                : timeOfDay;

        world.setTime(time, finalTimeOfDay, shouldTickTimeOfDay);
    }

    @Redirect(
            method = "onEntityStatus",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;showFloatingItem(Lnet/minecraft/item/ItemStack;)V")
    )
    private void showTotemAnimation(GameRenderer instance, ItemStack floatingItem) {
        NoRender noRender = NoRender.getInstance();
        if (!noRender.enabled || !noRender.totem.get()) {
            instance.showFloatingItem(floatingItem);
        }
    }
}
