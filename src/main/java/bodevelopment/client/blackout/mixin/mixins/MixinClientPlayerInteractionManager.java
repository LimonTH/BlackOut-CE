package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.combat.offensive.AutoMine;
import bodevelopment.client.blackout.module.modules.misc.AntiRotationSync;
import bodevelopment.client.blackout.module.modules.misc.HandMine;
import bodevelopment.client.blackout.randomstuff.FakePlayerEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class MixinClientPlayerInteractionManager {
    @Shadow
    public BlockPos currentBreakingPos;
    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    private float currentBreakingProgress;
    @Shadow
    private ItemStack selectedStack;
    @Shadow
    private boolean breakingBlock;
    @Shadow
    private float blockBreakingSoundCooldown;
    @Unique
    private BlockPos position = null;
    @Unique
    private Direction dir = null;

    @Shadow
    public abstract ActionResult interactBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult);

    @Shadow
    protected abstract void sendSequencedPacket(ClientWorld world, SequencedPacketCreator packetCreator);

    @Shadow
    public abstract boolean breakBlock(BlockPos pos);

    @Shadow
    public abstract int getBlockBreakingProgress();

    @Inject(method = "attackBlock", at = @At("HEAD"))
    private void onAttack(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        this.position = pos;
        this.dir = direction;
    }

    @Redirect(
            method = "attackBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;sendSequencedPacket(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/client/network/SequencedPacketCreator;)V",
                    ordinal = 1
            )
    )
    private void onStart(ClientPlayerInteractionManager instance, ClientWorld world, SequencedPacketCreator packetCreator) {
        AutoMine autoMine = AutoMine.getInstance();
        if (!autoMine.enabled) {
            HandMine handMine = HandMine.getInstance();
            if (!handMine.enabled) {
                this.sendSequencedPacket(world, packetCreator);
            } else {
                BlockState blockState = world.getBlockState(this.position);
                boolean bl = !blockState.isAir();
                boolean canInstant = bl
                        && handMine.getDelta(
                        this.position, blockState.calcBlockBreakingDelta(this.client.player, this.client.player.getEntityWorld(), this.position)
                )
                        >= 1.0F;
                Runnable runnable = () -> this.sendSequencedPacket(world, sequence -> {
                    if (bl && this.currentBreakingProgress == 0.0F) {
                        blockState.onBlockBreakStart(this.client.world, this.position, this.client.player);
                    }

                    if (bl && canInstant) {
                        handMine.onInstant(this.position, () -> this.breakBlock(this.position));
                    } else {
                        this.breakingBlock = true;
                        this.currentBreakingPos = this.position;
                        this.selectedStack = this.client.player.getMainHandStack();
                        this.currentBreakingProgress = 0.0F;
                        this.blockBreakingSoundCooldown = 0.0F;
                        this.client.world.setBlockBreakingInfo(this.client.player.getId(), this.currentBreakingPos, this.getBlockBreakingProgress());
                    }

                    return new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, this.position, this.dir, sequence);
                });
                if (canInstant) {
                    handMine.onInstant(this.position, runnable);
                } else {
                    runnable.run();
                }
            }
        } else {
            BlockState blockState = world.getBlockState(this.position);
            boolean bl = !blockState.isAir();
            if (bl && this.currentBreakingProgress == 0.0F) {
                blockState.onBlockBreakStart(this.client.world, this.position, this.client.player);
            }

            if (bl && blockState.calcBlockBreakingDelta(this.client.player, this.client.player.getEntityWorld(), this.position) >= 1.0F) {
                this.breakBlock(this.position);
            } else {
                this.breakingBlock = true;
                this.currentBreakingPos = this.position;
                this.selectedStack = this.client.player.getMainHandStack();
                this.currentBreakingProgress = 0.0F;
                this.blockBreakingSoundCooldown = 0.0F;
                this.client.world.setBlockBreakingInfo(this.client.player.getId(), this.currentBreakingPos, this.getBlockBreakingProgress());
            }

            autoMine.onStart(this.position);
        }
    }

    @Redirect(
            method = "attackBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V",
                    ordinal = 0
            )
    )
    private void onAbort(ClientPlayNetworkHandler instance, Packet<?> packet) {
        AutoMine autoMine = AutoMine.getInstance();
        if (!autoMine.enabled) {
            instance.sendPacket(packet);
        } else {
            autoMine.onAbort(this.position);
        }
    }

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"))
    private void onUpdateProgress(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        this.position = pos;
    }

    @Redirect(
            method = "updateBlockBreakingProgress",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/BlockState;calcBlockBreakingDelta(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)F"
            )
    )
    private float calcDelta2(BlockState instance, PlayerEntity playerEntity, BlockView blockView, BlockPos pos) {
        HandMine handMine = HandMine.getInstance();
        float vanilla = instance.calcBlockBreakingDelta(playerEntity, blockView, pos);
        return handMine.enabled ? handMine.getDelta(pos, vanilla) : vanilla;
    }

    @Redirect(
            method = "updateBlockBreakingProgress",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;sendSequencedPacket(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/client/network/SequencedPacketCreator;)V",
                    ordinal = 1
            )
    )
    private void onStop(ClientPlayerInteractionManager instance, ClientWorld world, SequencedPacketCreator packetCreator) {
        AutoMine autoMine = AutoMine.getInstance();
        if (autoMine.enabled) {
            autoMine.onStop(this.position);
        } else {
            HandMine handMine = HandMine.getInstance();
            if (handMine.enabled) {
                handMine.onEnd(this.position, () -> this.sendSequencedPacket(world, packetCreator));
            } else {
                this.sendSequencedPacket(world, packetCreator);
            }
        }
    }

    @Redirect(
            method = "cancelBlockBreaking",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V")
    )
    private void cancel(ClientPlayNetworkHandler instance, Packet<?> packet) {
        AutoMine autoMine = AutoMine.getInstance();
        if (!autoMine.enabled) {
            instance.sendPacket(packet);
        } else {
            autoMine.onAbort(this.currentBreakingPos);
        }
    }

    @Redirect(
            method = "interactItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;sendSequencedPacket(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/client/network/SequencedPacketCreator;)V")
    )
    private void onRotationSync(ClientPlayerInteractionManager instance, ClientWorld world, SequencedPacketCreator creator) {
        if (!AntiRotationSync.getInstance().enabled) {
            instance.sendSequencedPacket(world, creator);
            return;
        }

        instance.sendSequencedPacket(world, (sequence) -> {
            creator.predict(sequence);

            return new PlayerMoveC2SPacket.Full(
                    BlackOut.mc.player.getX(),
                    BlackOut.mc.player.getY(),
                    BlackOut.mc.player.getZ(),
                    Managers.ROTATION.prevYaw,
                    Managers.ROTATION.prevPitch,
                    Managers.PACKET.isOnGround()
            );
        });
    }

    @Redirect(method = "attackEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;attack(Lnet/minecraft/entity/Entity;)V"))
    private void onAttack(PlayerEntity instance, Entity target) {
        if (!(target instanceof FakePlayerEntity)) {
            instance.attack(target);
        }
    }
}
