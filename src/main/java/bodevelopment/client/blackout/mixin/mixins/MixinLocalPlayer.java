package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.events.MoveEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.manager.managers.RotationManager;
import bodevelopment.client.blackout.module.modules.misc.AntiHunger;
import bodevelopment.client.blackout.module.modules.misc.Portals;
import bodevelopment.client.blackout.module.modules.movement.NoSlow;
import bodevelopment.client.blackout.module.modules.movement.Sprint;
import bodevelopment.client.blackout.module.modules.movement.TickShift;
import bodevelopment.client.blackout.module.modules.movement.Velocity;
import bodevelopment.client.blackout.module.modules.visual.misc.FreeCam;
import bodevelopment.client.blackout.module.modules.visual.misc.SwingModifier;
import bodevelopment.client.blackout.util.RotationUtils;
import bodevelopment.client.blackout.util.SettingUtils;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer {
    @Unique
    private static boolean sent = false;
    @Unique
    private static boolean wasMove = false;
    @Unique
    private static boolean wasRotation = false;
    @Shadow
    public ClientInput input;
    @Shadow
    private float yRotLast;
    @Shadow
    private float xRotLast;

    @Shadow
    protected abstract boolean hasEnoughFoodToStartSprinting();

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;)V", at = @At("HEAD"))
    private void swingHand(InteractionHand hand, CallbackInfo ci) {
        SwingModifier.getInstance().startSwing(hand);
    }

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void sendPacketsHead(CallbackInfo ci) {
        sent = false;
        wasMove = false;
        wasRotation = false;
    }

    @Redirect(
            method = "sendPosition",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V")
    )
    private void onSendPacket(ClientPacketListener instance, Packet<?> packet) {
        sent = true;
        wasMove = packet instanceof ServerboundMovePlayerPacket moveC2SPacket && moveC2SPacket.hasPosition();
        wasRotation = packet instanceof ServerboundMovePlayerPacket moveC2SPacketx && moveC2SPacketx.hasRotation();
        instance.send(packet);
    }

    @Inject(method = "sendPosition", at = @At("TAIL"))
    private void sendPacketsTail(CallbackInfo ci) {
        if (!sent
                && Managers.ROTATION.rotated()
                && (Managers.ROTATION.rotatingYaw != RotationManager.RotatePhase.Inactive || Managers.ROTATION.rotatingPitch != RotationManager.RotatePhase.Inactive)) {
            BlackOut.mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(Managers.ROTATION.nextYaw, Managers.ROTATION.nextPitch, Managers.PACKET.isOnGround(), BlackOut.mc.player.horizontalCollision));
            wasRotation = true;
            sent = true;
        }

        TickShift tickShift = TickShift.getInstance();
        if (tickShift.enabled && tickShift.canCharge(sent, wasMove)) {
            tickShift.unSent = Math.min(tickShift.packets.get(), tickShift.unSent + tickShift.chargeSpeed.get());
        }

        BlackOut.EVENT_BUS.post(MoveEvent.PostSend.get());
    }

    @Redirect(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
    private float getYaw(LocalPlayer instance) {
        return instance == BlackOut.mc.player ? Managers.ROTATION.getNextYaw() : instance.getYRot();
    }

    @Redirect(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
    private float getPitch(LocalPlayer instance) {
        return instance == BlackOut.mc.player ? Managers.ROTATION.getNextPitch() : instance.getXRot();
    }

    @Redirect(method = "sendPosition", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;yRotLast:F", opcode = 180))
    private float prevYaw(LocalPlayer instance) {
        return instance == BlackOut.mc.player ? Managers.ROTATION.prevYaw : this.yRotLast;
    }

    @Redirect(method = "sendPosition", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;xRotLast:F", opcode = 180))
    private float prevPitch(LocalPlayer instance) {
        return instance == BlackOut.mc.player ? Managers.ROTATION.prevPitch : this.xRotLast;
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean usingItem(LocalPlayer instance) {
        return instance == BlackOut.mc.player ? NoSlow.shouldSlow() : instance.isUsingItem();
    }

    @Redirect(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z"))
    private boolean isOnGround(LocalPlayer instance) {
        if (instance == BlackOut.mc.player) {
            AntiHunger antiHunger = AntiHunger.getInstance();
            if (antiHunger.enabled && antiHunger.moving.get()) {
                return false;
            }
        }
        return instance.onGround();
    }

    @Redirect(method = "sendIsSprintingIfNeeded", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSprinting()Z"))
    private boolean sprinting(LocalPlayer instance) {
        if (instance == BlackOut.mc.player) {
            AntiHunger antiHunger = AntiHunger.getInstance();
            if (antiHunger.enabled && antiHunger.sprint.get()) {
                return false;
            }
        }

        return instance.isSprinting();
    }

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void pushOutOfBlocks(double x, double z, CallbackInfo ci) {
        if ((Object) this == BlackOut.mc.player) {
            Velocity velocity = Velocity.getInstance();
            if (velocity.enabled && velocity.blockPush.get()) {
                ci.cancel();
            }
        }
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSprinting()Z"))
    private boolean forwardMovement(LocalPlayer value) {
        if ((Object) this != BlackOut.mc.player) {
            return value.isSprinting();
        } else {
            Sprint sprint = Sprint.getInstance();
            boolean hasInput;
            if (!SettingUtils.grimMovement() && SettingUtils.strictSprint()) {
                hasInput = Managers.ROTATION.move && Math.abs(RotationUtils.yawAngle(Managers.ROTATION.moveYaw, Managers.ROTATION.nextYaw)) <= 45.0;
            } else {
                hasInput = this.input.hasForwardImpulse() || sprint.enabled && sprint.shouldSprint();
            }

            boolean cantSprint = !hasInput || !this.hasEnoughFoodToStartSprinting();
            if (value.isSwimming()) {
                if (!value.onGround() && !this.input.keyPresses.shift() && cantSprint || !value.isInWater()) {
                    value.setSprinting(false);
                }
            } else if (cantSprint || value.horizontalCollision && !value.minorHorizontalCollision || value.isInWater() && !value.isUnderWater()) {
                value.setSprinting(false);
            }

            return false;
        }
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;tick(ZF)V"))
    private void tickInput(ClientInput instance, boolean slowDown, float slowDownFactor) {
        if ((Object) this != BlackOut.mc.player) {
            instance.tick(slowDown, slowDownFactor);
        } else {
            FreeCam freecam = FreeCam.getInstance();
            if (freecam.enabled) {
                freecam.resetInput((KeyboardInput) instance);
            } else {
                instance.tick(slowDown, slowDownFactor);
            }
        }
    }

    @Inject(method = "handleConfusionTransitionEffect", at = @At("HEAD"), cancellable = true)
    private void onUpdateNausea(CallbackInfo ci) {
        if (Portals.getInstance().enabled) {
            ci.cancel();
        }
    }
}
