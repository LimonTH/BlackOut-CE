package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.events.GameJoinEvent;
import bodevelopment.client.blackout.mixin.accessors.AccessorPlayerMoveC2SPacket;
import bodevelopment.client.blackout.module.modules.misc.NoRotate;
import bodevelopment.client.blackout.module.modules.visual.misc.NoRender;
import bodevelopment.client.blackout.module.modules.visual.world.Ambience;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.world.ClientWorld;
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
    private float lastServerYaw;
    @Unique
    private float lastServerPitch;

    @Inject(method = "onGameJoin", at = @At("TAIL"))
    private void onJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        BlackOut.EVENT_BUS.post(GameJoinEvent.get(packet));
    }

    @Redirect(method = "onPlayerPositionLook", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setYaw(F)V"))
    private void rubberbandYaw(PlayerEntity instance, float v) {
        NoRotate noRotate = NoRotate.getInstance();

        if (noRotate.enabled && noRotate.mode.get() == NoRotate.NoRotateMode.Rel) {
            noRotate.relYaw = v - instance.getYaw();
        } else if (!noRotate.enabled) {
            instance.setYaw(v);
        }

        this.lastServerYaw = v;
    }

    @Redirect(method = "onPlayerPositionLook", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setPitch(F)V"))
    private void rubberbandPitch(PlayerEntity instance, float v) {
        NoRotate noRotate = NoRotate.getInstance();

        if (noRotate.enabled && noRotate.mode.get() == NoRotate.NoRotateMode.Rel) {
            noRotate.relPitch = v - instance.getPitch();
        } else if (!noRotate.enabled) {
            instance.setPitch(v);
        }

        this.lastServerPitch = v;
    }

    @Redirect(
            method = "onPlayerPositionLook",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;send(Lnet/minecraft/network/packet/Packet;)V", ordinal = 1)
    )
    private void sendFull(ClientConnection instance, Packet<?> packet) {
        PlayerMoveC2SPacket.Full moveC2SPacket = (PlayerMoveC2SPacket.Full) packet;
        NoRotate noRotate = NoRotate.getInstance();
        if (!noRotate.enabled) {
            instance.send(moveC2SPacket);
        } else {
            switch (noRotate.mode.get()) {
                case Cancel:
                    return;
                case Set:
                case Spoof:
                    ((AccessorPlayerMoveC2SPacket) moveC2SPacket).setYaw(this.lastServerYaw);
                    ((AccessorPlayerMoveC2SPacket) moveC2SPacket).setPitch(this.lastServerPitch);
                    break;
                case Rel:
                    ((AccessorPlayerMoveC2SPacket) moveC2SPacket).setYaw(this.lastServerYaw);
                    ((AccessorPlayerMoveC2SPacket) moveC2SPacket).setPitch(this.lastServerPitch);

                    BlackOut.mc.player.setYaw(BlackOut.mc.player.getYaw() + noRotate.relYaw);
                    BlackOut.mc.player.setPitch(BlackOut.mc.player.getPitch() + noRotate.relPitch);

                    noRotate.relYaw = 0;
                    noRotate.relPitch = 0;
                    break;
            }
        }
        instance.send(moveC2SPacket);
    }

    @Redirect(method = "onWorldTimeUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;setTimeOfDay(J)V"))
    private void setWorldTime(ClientWorld instance, long timeOfDay) {
        Ambience ambience = Ambience.getInstance();
        if (ambience.enabled && ambience.modifyTime.get()) {
            instance.setTimeOfDay(ambience.time.get());
        } else {
            instance.setTimeOfDay(timeOfDay);
        }
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
