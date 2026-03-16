package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.events.InteractBlockEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.interfaces.mixin.IMinecraft;
import bodevelopment.client.blackout.interfaces.mixin.ITimer;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.combat.misc.FastEat;
import bodevelopment.client.blackout.module.modules.combat.misc.MultiTask;
import bodevelopment.client.blackout.module.modules.combat.misc.Quiver;
import bodevelopment.client.blackout.module.modules.legit.HitCrystal;
import bodevelopment.client.blackout.module.modules.misc.FastUse;
import bodevelopment.client.blackout.module.modules.misc.NoInteract;
import bodevelopment.client.blackout.module.modules.misc.Timer;
import bodevelopment.client.blackout.module.modules.movement.FastRiptide;
import bodevelopment.client.blackout.module.modules.visual.misc.CameraModifier;
import bodevelopment.client.blackout.module.modules.visual.misc.CustomChat;
import bodevelopment.client.blackout.randomstuff.CustomChatScreen;
import bodevelopment.client.blackout.randomstuff.timers.TickTimerList;
import bodevelopment.client.blackout.util.SettingUtils;
import bodevelopment.client.blackout.util.SharedFeatures;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

@Mixin(value = Minecraft.class, priority = 500)
public abstract class MixinMinecraft implements IMinecraft {
    @Shadow
    @Nullable
    public LocalPlayer player;
    @Shadow
    @Final
    public Options options;
    @Shadow
    @Final
    @Mutable
    private User user;
    @Shadow
    @Final
    private DeltaTracker.Timer deltaTracker;

    @Shadow
    protected abstract void runTick(boolean tick);

    @Shadow
    public abstract void updateTitle();

    @Shadow
    protected abstract void startUseItem();

    @Redirect(
            method = "handleKeybinds",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;cycle()Lnet/minecraft/client/CameraType;")
    )
    private CameraType setPerspective(CameraType instance) {
        CameraModifier modifier = CameraModifier.getInstance();
        if (modifier != null && modifier.enabled && modifier.noInverse.get()) {
            if (instance == CameraType.FIRST_PERSON) {
                return CameraType.THIRD_PERSON_BACK;
            } else {
                return CameraType.FIRST_PERSON;
            }
        } else {
            return instance.cycle();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void preTick(CallbackInfo ci) {
        TickTimerList.updating.forEach(TickTimerList::update);
        BlackOut.EVENT_BUS.post(TickEvent.Pre.get());
        if (!SettingUtils.grimPackets()) {
            HitCrystal.getInstance().onTick();
        }
    }

    @WrapOperation(
            method = "openChatScreen",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V")
    )
    private void wrapChatOpening(Minecraft instance, Screen screen, Operation<Void> original) {
        Screen targetScreen = CustomChat.getInstance().enabled ? new CustomChatScreen() : screen;

        original.call(instance, targetScreen);
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;", ordinal = 6, opcode = Opcodes.GETFIELD)
    )
    private Screen wrapCurrentScreen(Minecraft instance, Operation<Screen> original) {
        Screen realScreen = original.call(instance);
        return SharedFeatures.shouldSilentScreen() ? null : realScreen;
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;overlay:Lnet/minecraft/client/gui/screens/Overlay;", opcode = Opcodes.GETFIELD)
    )
    private Overlay wrapOverlay(Minecraft instance, Operation<Overlay> original) {
        Overlay realOverlay = original.call(instance);
        return SharedFeatures.shouldSilentScreen() ? null : realOverlay;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void postTick(CallbackInfo ci) {
        BlackOut.EVENT_BUS.post(TickEvent.Post.get());
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void onRun(CallbackInfo ci) {
        this.updateTitle();
        Timer timer = Timer.getInstance();
        ((ITimer) this.deltaTracker).blackout_Client$set(timer.getTickTime());
        if (BlackOut.mc.level != null) {
            BlackOut.mc.level.tickRateManager().setTickRate(timer.getTPS());
        }
    }

    @Redirect(method = "continueAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean isUsing(LocalPlayer instance) {
        return !MultiTask.getInstance().enabled && instance.isUsingItem();
    }

    @Override
    public void blackout_Client$setSession(
            String username, UUID uuid, String accessToken, String xuid, String clientId, User.Type accountType
    ) {
        this.user = new User(username, uuid, accessToken, Optional.ofNullable(xuid), Optional.ofNullable(clientId), accountType);
    }

    @Override
    public void blackout_Client$setSession(User session) {
        this.user = session;
    }

    @Override
    public void blackout_Client$useItem() {
        this.startUseItem();
    }

    @Redirect(
            method = "startUseItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"
            )
    )
    private InteractionResult onInteractBlock(MultiPlayerGameMode instance, LocalPlayer player, InteractionHand hand, BlockHitResult hitResult) {
        if (!BlackOut.EVENT_BUS.post(InteractBlockEvent.get(hitResult, hand)).isCancelled()) {
            NoInteract noInteract = NoInteract.getInstance();
            return noInteract.enabled
                    ? noInteract.handleBlock(hand, hitResult.getBlockPos(), () -> instance.useItemOn(player, hand, hitResult))
                    : instance.useItemOn(player, hand, hitResult);
        } else {
            return InteractionResult.FAIL;
        }
    }

    @Redirect(
            method = "startUseItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;interactAt(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/EntityHitResult;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"
            )
    )
    private InteractionResult onEntityInteractAt(MultiPlayerGameMode instance, Player player, Entity entity, EntityHitResult hitResult, InteractionHand hand) {
        NoInteract noInteract = NoInteract.getInstance();
        return noInteract.enabled
                ? noInteract.handleEntity(hand, entity, () -> instance.interactAt(player, entity, hitResult, hand))
                : instance.interactAt(player, entity, hitResult, hand);
    }

    @Redirect(
            method = "startUseItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"
            )
    )
    private InteractionResult onEntityInteract(MultiPlayerGameMode instance, Player player, Entity entity, InteractionHand hand) {
        NoInteract noInteract = NoInteract.getInstance();
        return noInteract.enabled
                ? noInteract.handleEntity(hand, entity, () -> instance.interact(player, entity, hand))
                : instance.interact(player, entity, hand);
    }

    @Redirect(
            method = "startUseItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"
            )
    )
    private InteractionResult onItemInteract(MultiPlayerGameMode instance, Player player, InteractionHand hand) {
        NoInteract noInteract = NoInteract.getInstance();
        return noInteract.enabled ? noInteract.handleUse(hand, () -> instance.useItem(player, hand)) : instance.useItem(player, hand);
    }

    @Redirect(
            method = "handleKeybinds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;releaseUsingItem(Lnet/minecraft/world/entity/player/Player;)V"
            )
    )
    private void onReleaseUsing(MultiPlayerGameMode instance, Player player) {
        if (!Quiver.charging && !FastEat.eating()) {
            instance.releaseUsingItem(player);
        }
    }

    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private boolean shouldKeepUsing(KeyMapping instance) {
        FastRiptide fastRiptide = FastRiptide.getInstance();
        ItemStack activeItem = BlackOut.mc.player != null ? BlackOut.mc.player.getUseItem() : null;
        return fastRiptide.enabled && activeItem != null && activeItem.getItem() instanceof TridentItem
                ? System.currentTimeMillis() - fastRiptide.prevRiptide < fastRiptide.cooldown.get() * 1000.0
                : instance.isDown();
    }

    @Inject(method = "resizeDisplay", at = @At("TAIL"))
    private void onResize(CallbackInfo ci) {
        Managers.FRAME_BUFFER.onResize();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;createTitle()Ljava/lang/String;"))
    private String windowTitle(Minecraft instance) {
        return this.getBOTitle();
    }

    @Redirect(method = "updateTitle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;createTitle()Ljava/lang/String;"))
    private String updateTitle(Minecraft instance) {
        return this.getBOTitle();
    }

    @ModifyConstant(method = "startUseItem", constant = @Constant(intValue = 4))
    private int itemUseCooldown(int constant) {
        FastUse fastUse = FastUse.getInstance();
        if (fastUse.enabled && fastUse.timing.get() == FastUse.Timing.Tick) {
            ItemStack stack = fastUse.getStack();
            return fastUse.isValid(stack) ? fastUse.delayTicks.get() : 4;
        } else {
            return 4;
        }
    }

    @Redirect(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;isDestroying()Z"))
    private boolean multiTaskThingy(MultiPlayerGameMode instance) {
        MultiTask multiTask = MultiTask.getInstance();
        FastUse fastUse = FastUse.getInstance();
        if (fastUse.enabled) {
            ItemStack stack = fastUse.getStack();
            if (fastUse.isValid(stack) && fastUse.rotateIfNeeded(stack)) {
                return false;
            }
        }

        return !multiTask.enabled && instance.isDestroying();
    }

    @Unique
    private String getBOTitle() {
        return BlackOut.NAME + " Client " + BlackOut.VERSION;
    }
}
