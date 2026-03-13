package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.events.GameJoinEvent;
import bodevelopment.client.blackout.mixin.accessors.AccessorPlayerMoveC2SPacket;
import bodevelopment.client.blackout.module.modules.misc.NoRotate;
import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import bodevelopment.client.blackout.module.modules.visual.world.Ambience;
import bodevelopment.client.blackout.util.CompatUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPlayNetworkHandler {
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
                        ((AccessorPlayerMoveC2SPacket) moveC2SPacket).setYaw(lastServerYaw);
                        ((AccessorPlayerMoveC2SPacket) moveC2SPacket).setPitch(lastServerPitch);

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
}
