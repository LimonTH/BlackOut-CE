package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.combat.offensive.AutoMine;
import bodevelopment.client.blackout.module.modules.misc.AntiRotationSync;
import bodevelopment.client.blackout.module.modules.misc.HandMine;
import bodevelopment.client.blackout.randomstuff.FakePlayerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode {
    @Shadow
    public BlockPos destroyBlockPos;
    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    private float destroyProgress;
    @Shadow
    private ItemStack destroyingItem;
    @Shadow
    private boolean isDestroying;
    @Shadow
    private float destroyTicks;
    @Unique
    private BlockPos position = null;
    @Unique
    private Direction dir = null;

    @Shadow
    public abstract InteractionResult useItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult);

    @Shadow
    protected abstract void startPrediction(ClientLevel world, PredictiveAction packetCreator);

    @Shadow
    public abstract boolean destroyBlock(BlockPos pos);

    @Shadow
    public abstract int getDestroyStage();

    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void onAttack(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        this.position = pos;
        this.dir = direction;
    }

    @Redirect(
            method = "startDestroyBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startPrediction(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/multiplayer/prediction/PredictiveAction;)V",
                    ordinal = 1
            )
    )
    private void onStart(MultiPlayerGameMode instance, ClientLevel world, PredictiveAction packetCreator) {
        AutoMine autoMine = AutoMine.getInstance();
        if (!autoMine.enabled) {
            HandMine handMine = HandMine.getInstance();
            if (!handMine.enabled) {
                this.startPrediction(world, packetCreator);
            } else {
                BlockState blockState = world.getBlockState(this.position);
                boolean bl = !blockState.isAir();
                boolean canInstant = bl
                        && handMine.getDelta(
                        this.position, blockState.getDestroyProgress(this.minecraft.player, this.minecraft.player.getCommandSenderWorld(), this.position)
                )
                        >= 1.0F;
                Runnable runnable = () -> this.startPrediction(world, sequence -> {
                    if (bl && this.destroyProgress == 0.0F) {
                        blockState.attack(this.minecraft.level, this.position, this.minecraft.player);
                    }

                    if (bl && canInstant) {
                        handMine.onInstant(this.position, () -> this.destroyBlock(this.position));
                    } else {
                        this.isDestroying = true;
                        this.destroyBlockPos = this.position;
                        this.destroyingItem = this.minecraft.player.getMainHandItem();
                        this.destroyProgress = 0.0F;
                        this.destroyTicks = 0.0F;
                        this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, this.getDestroyStage());
                    }

                    return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, this.position, this.dir, sequence);
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
            if (bl && this.destroyProgress == 0.0F) {
                blockState.attack(this.minecraft.level, this.position, this.minecraft.player);
            }

            if (bl && blockState.getDestroyProgress(this.minecraft.player, this.minecraft.player.getCommandSenderWorld(), this.position) >= 1.0F) {
                this.destroyBlock(this.position);
            } else {
                this.isDestroying = true;
                this.destroyBlockPos = this.position;
                this.destroyingItem = this.minecraft.player.getMainHandItem();
                this.destroyProgress = 0.0F;
                this.destroyTicks = 0.0F;
                this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, this.getDestroyStage());
            }

            autoMine.onStart(this.position);
        }
    }

    @Redirect(
            method = "startDestroyBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V",
                    ordinal = 0
            )
    )
    private void onAbort(ClientPacketListener instance, Packet<?> packet) {
        AutoMine autoMine = AutoMine.getInstance();

        if (!autoMine.enabled) {
            instance.send(packet);
        } else {
            autoMine.onAbort(this.destroyBlockPos);
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void onUpdateProgress(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        this.position = pos;
    }

    @Redirect(
            method = "continueDestroyBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroyProgress(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"
            )
    )
    private float calcDelta2(BlockState instance, Player player, BlockGetter level, BlockPos pos) {
        HandMine handMine = HandMine.getInstance();
        float vanilla = instance.getDestroyProgress(player, level, pos);

        return handMine.enabled ? handMine.getDelta(pos, vanilla) : vanilla;
    }

    @Redirect(
            method = "continueDestroyBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startPrediction(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/multiplayer/prediction/PredictiveAction;)V",
                    ordinal = 1
            )
    )
    private void onStop(MultiPlayerGameMode instance, ClientLevel world, PredictiveAction packetCreator) {
        AutoMine autoMine = AutoMine.getInstance();
        if (autoMine.enabled) {
            autoMine.onStop(this.position);
        } else {
            HandMine handMine = HandMine.getInstance();
            if (handMine.enabled) {
                handMine.onEnd(this.position, () -> this.startPrediction(world, packetCreator));
            } else {
                this.startPrediction(world, packetCreator);
            }
        }
    }

    @Redirect(
            method = "stopDestroyBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"
            )
    )
    private void cancel(ClientPacketListener instance, Packet<?> packet) {
        AutoMine autoMine = AutoMine.getInstance();
        if (!autoMine.enabled) {
            instance.send(packet);
        } else {
            autoMine.onAbort(this.destroyBlockPos);
        }
    }

    @Redirect(
            method = "useItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startPrediction(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/multiplayer/prediction/PredictiveAction;)V")
    )
    private void onRotationSync(MultiPlayerGameMode instance, ClientLevel world, PredictiveAction creator) {
        if (!AntiRotationSync.getInstance().enabled) {
            instance.startPrediction(world, creator);
            return;
        }

        instance.startPrediction(world, (sequence) -> {
            Packet<?> originalPacket = creator.predict(sequence);
            InteractionHand hand = InteractionHand.MAIN_HAND;

            if (originalPacket instanceof ServerboundUseItemPacket interactPacket) {
                hand = interactPacket.getHand();
            }

            return new ServerboundUseItemPacket(
                hand, 
                sequence, 
                Managers.ROTATION.prevYaw, 
                Managers.ROTATION.prevPitch
            );
        });
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;attack(Lnet/minecraft/world/entity/Entity;)V"))
    private void onAttack(Player instance, Entity target) {
        if (!(target instanceof FakePlayerEntity)) {
            instance.attack(target);
        }
    }
}
