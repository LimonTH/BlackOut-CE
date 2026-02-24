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
import bodevelopment.client.blackout.module.modules.visual.misc.Freecam;
import bodevelopment.client.blackout.module.modules.visual.misc.SwingModifier;
import bodevelopment.client.blackout.util.RotationUtils;
import bodevelopment.client.blackout.util.SettingUtils;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity {
    @Unique
    private static boolean sent = false;
    @Unique
    private static boolean wasMove = false;
    @Unique
    private static boolean wasRotation = false;
    @Shadow
    public Input input;
    @Shadow
    private float lastYaw;
    @Shadow
    private float lastPitch;

    @Shadow
    protected abstract boolean canSprint();

    @Inject(method = "swingHand(Lnet/minecraft/util/Hand;)V", at = @At("HEAD"))
    private void swingHand(Hand hand, CallbackInfo ci) {
        SwingModifier.getInstance().startSwing(hand);
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void sendPacketsHead(CallbackInfo ci) {
        sent = false;
        wasMove = false;
        wasRotation = false;
    }

    @Redirect(
            method = "sendMovementPackets",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V")
    )
    private void onSendPacket(ClientPlayNetworkHandler instance, Packet<?> packet) {
        sent = true;
        wasMove = packet instanceof PlayerMoveC2SPacket moveC2SPacket && moveC2SPacket.changesPosition();
        wasRotation = packet instanceof PlayerMoveC2SPacket moveC2SPacketx && moveC2SPacketx.changesLook();
        instance.sendPacket(packet);
    }

    @Inject(method = "sendMovementPackets", at = @At("TAIL"))
    private void sendPacketsTail(CallbackInfo ci) {
        if (!sent
                && Managers.ROTATION.rotated()
                && (Managers.ROTATION.rotatingYaw != RotationManager.RotatePhase.Inactive || Managers.ROTATION.rotatingPitch != RotationManager.RotatePhase.Inactive)) {
            BlackOut.mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(Managers.ROTATION.nextYaw, Managers.ROTATION.nextPitch, Managers.PACKET.isOnGround()));
            wasRotation = true;
            sent = true;
        }

        TickShift tickShift = TickShift.getInstance();
        if (tickShift.enabled && tickShift.canCharge(sent, wasMove)) {
            tickShift.unSent = Math.min(tickShift.packets.get(), tickShift.unSent + tickShift.chargeSpeed.get());
        }

        BlackOut.EVENT_BUS.post(MoveEvent.PostSend.get());
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
    private float getYaw(ClientPlayerEntity instance) {
        return instance == BlackOut.mc.player ? Managers.ROTATION.getNextYaw() : instance.getYaw();
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
    private float getPitch(ClientPlayerEntity instance) {
        return instance == BlackOut.mc.player ? Managers.ROTATION.getNextPitch() : instance.getPitch();
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerEntity;lastYaw:F", opcode = 180))
    private float prevYaw(ClientPlayerEntity instance) {
        return instance == BlackOut.mc.player ? Managers.ROTATION.prevYaw : this.lastYaw;
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerEntity;lastPitch:F", opcode = 180))
    private float prevPitch(ClientPlayerEntity instance) {
        return instance == BlackOut.mc.player ? Managers.ROTATION.prevPitch : this.lastPitch;
    }

    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean usingItem(ClientPlayerEntity instance) {
        return instance == BlackOut.mc.player ? NoSlow.shouldSlow() : instance.isUsingItem();
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isOnGround()Z"))
    private boolean isOnGround(ClientPlayerEntity instance) {
        if (instance == BlackOut.mc.player) {
            AntiHunger antiHunger = AntiHunger.getInstance();
            if (antiHunger.enabled && antiHunger.moving.get()) {
                return false;
            }
        }
        return instance.isOnGround();
    }

    @Redirect(method = "sendSprintingPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isSprinting()Z"))
    private boolean sprinting(ClientPlayerEntity instance) {
        if (instance == BlackOut.mc.player) {
            AntiHunger antiHunger = AntiHunger.getInstance();
            if (antiHunger.enabled && antiHunger.sprint.get()) {
                return false;
            }
        }

        return instance.isSprinting();
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void pushOutOfBlocks(double x, double z, CallbackInfo ci) {
        if ((Object) this == BlackOut.mc.player) {
            Velocity velocity = Velocity.getInstance();
            if (velocity.enabled && velocity.blockPush.get()) {
                ci.cancel();
            }
        }
    }

    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isSprinting()Z"))
    private boolean forwardMovement(ClientPlayerEntity value) {
        if ((Object) this != BlackOut.mc.player) {
            return value.isSprinting();
        } else {
            Sprint sprint = Sprint.getInstance();
            boolean hasInput;
            if (!SettingUtils.grimMovement() && SettingUtils.strictSprint()) {
                hasInput = Managers.ROTATION.move && Math.abs(RotationUtils.yawAngle(Managers.ROTATION.moveYaw, Managers.ROTATION.nextYaw)) <= 45.0;
            } else {
                hasInput = this.input.hasForwardMovement() || sprint.enabled && sprint.shouldSprint();
            }

            boolean cantSprint = !hasInput || !this.canSprint();
            if (value.isSwimming()) {
                if (!value.isOnGround() && !this.input.sneaking && cantSprint || !value.isTouchingWater()) {
                    value.setSprinting(false);
                }
            } else if (cantSprint || value.horizontalCollision && !value.collidedSoftly || value.isTouchingWater() && !value.isSubmergedInWater()) {
                value.setSprinting(false);
            }

            return false;
        }
    }

    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/Input;tick(ZF)V"))
    private void tickInput(Input instance, boolean slowDown, float slowDownFactor) {
        if ((Object) this != BlackOut.mc.player) {
            instance.tick(slowDown, slowDownFactor);
        } else {
            Freecam freecam = Freecam.getInstance();
            if (freecam.enabled) {
                freecam.resetInput((KeyboardInput) instance);
            } else {
                instance.tick(slowDown, slowDownFactor);
            }
        }
    }

    @Inject(method = "tickNausea", at = @At("HEAD"), cancellable = true)
    private void onUpdateNausea(CallbackInfo ci) {
        if (Portals.getInstance().enabled) {
            ci.cancel();
        }
    }
}
