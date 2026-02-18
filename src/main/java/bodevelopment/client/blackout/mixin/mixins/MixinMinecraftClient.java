package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.events.InteractBlockEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.interfaces.mixin.IMinecraftClient;
import bodevelopment.client.blackout.interfaces.mixin.IRenderTickCounter;
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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.session.Session;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient implements IMinecraftClient {
    @Shadow
    static MinecraftClient instance;
    @Shadow
    @Nullable
    public ClientPlayerEntity player;
    @Shadow
    @Final
    public GameOptions options;
    @Shadow
    @Final
    @Mutable
    private Session session;
    @Shadow
    @Final
    private RenderTickCounter.Dynamic renderTickCounter;


    @Shadow
    protected abstract void render(boolean tick);

    @Shadow
    public abstract void updateWindowTitle();

    @Shadow
    protected abstract void doItemUse();

    @Redirect(
            method = "openChatScreen",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V")
    )
    private void redirectChat(MinecraftClient instance, Screen screen) {
        instance.setScreen(CustomChat.getInstance().enabled ? new CustomChatScreen() : screen);
    }

    @Redirect(
            method = "handleInputEvents",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/Perspective;next()Lnet/minecraft/client/option/Perspective;")
    )
    private Perspective setPerspective(Perspective instance) {
        CameraModifier modifier = CameraModifier.getInstance();
        if (modifier != null && modifier.enabled && modifier.noInverse.get()) {
            return instance == Perspective.FIRST_PERSON ? Perspective.THIRD_PERSON_BACK : Perspective.FIRST_PERSON;
        } else {
            return instance;
        }
    }

    @Redirect(
            method = "tick",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;", ordinal = 6, opcode = Opcodes.GETFIELD)
    )
    private Screen redirectCurrentScreen(MinecraftClient instance) {
        return SharedFeatures.shouldSilentScreen() ? null : instance.currentScreen;
    }

    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;overlay:Lnet/minecraft/client/gui/screen/Overlay;", opcode = Opcodes.GETFIELD))
    private Overlay redirectOverlay(MinecraftClient instance) {
        return SharedFeatures.shouldSilentScreen() ? null : instance.getOverlay();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void preTick(CallbackInfo ci) {
        TickTimerList.updating.forEach(TickTimerList::update);
        BlackOut.EVENT_BUS.post(TickEvent.Pre.get());
        if (!SettingUtils.grimPackets()) {
            HitCrystal.getInstance().onTick();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void postTick(CallbackInfo ci) {
        BlackOut.EVENT_BUS.post(TickEvent.Post.get());
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRun(CallbackInfo ci) {
        this.updateWindowTitle();
        Timer timer = Timer.getInstance();
        ((IRenderTickCounter) this.renderTickCounter).blackout_Client$set(timer.getTickTime());
        if (BlackOut.mc.world != null) {
            BlackOut.mc.world.getTickManager().setTickRate(timer.getTPS());
        }
    }

    @Redirect(method = "handleBlockBreaking", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean isUsing(ClientPlayerEntity instance) {
        return !MultiTask.getInstance().enabled && instance.isUsingItem();
    }

    @Override
    public void blackout_Client$setSession(
            String username, UUID uuid, String accessToken, String xuid, String clientId, Session.AccountType accountType
    ) {
        this.session = new Session(username, uuid, accessToken, Optional.ofNullable(xuid), Optional.ofNullable(clientId), accountType);
    }

    @Override
    public void blackout_Client$setSession(Session session) {
        this.session = session;
    }

    @Override
    public void blackout_Client$useItem() {
        this.doItemUse();
    }

    @Redirect(
            method = "doItemUse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;interactBlock(Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;"
            )
    )
    private ActionResult onInteractBlock(ClientPlayerInteractionManager instance, ClientPlayerEntity player, Hand hand, BlockHitResult hitResult) {
        if (!BlackOut.EVENT_BUS.post(InteractBlockEvent.get(hitResult, hand)).isCancelled()) {
            NoInteract noInteract = NoInteract.getInstance();
            return noInteract.enabled
                    ? noInteract.handleBlock(hand, hitResult.getBlockPos(), () -> instance.interactBlock(player, hand, hitResult))
                    : instance.interactBlock(player, hand, hitResult);
        } else {
            return ActionResult.FAIL;
        }
    }

    @Redirect(
            method = "doItemUse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;interactEntityAtLocation(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/hit/EntityHitResult;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"
            )
    )
    private ActionResult onEntityInteractAt(ClientPlayerInteractionManager instance, PlayerEntity player, Entity entity, EntityHitResult hitResult, Hand hand) {
        NoInteract noInteract = NoInteract.getInstance();
        return noInteract.enabled
                ? noInteract.handleEntity(hand, entity, () -> instance.interactEntityAtLocation(player, entity, hitResult, hand))
                : instance.interactEntityAtLocation(player, entity, hitResult, hand);
    }

    @Redirect(
            method = "doItemUse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;interactEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"
            )
    )
    private ActionResult onEntityInteract(ClientPlayerInteractionManager instance, PlayerEntity player, Entity entity, Hand hand) {
        NoInteract noInteract = NoInteract.getInstance();
        return noInteract.enabled
                ? noInteract.handleEntity(hand, entity, () -> instance.interactEntity(player, entity, hand))
                : instance.interactEntity(player, entity, hand);
    }

    @Redirect(
            method = "doItemUse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;interactItem(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"
            )
    )
    private ActionResult onItemInteract(ClientPlayerInteractionManager instance, PlayerEntity player, Hand hand) {
        NoInteract noInteract = NoInteract.getInstance();
        return noInteract.enabled ? noInteract.handleUse(hand, () -> instance.interactItem(player, hand)) : instance.interactItem(player, hand);
    }

    @Redirect(
            method = "handleInputEvents",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;stopUsingItem(Lnet/minecraft/entity/player/PlayerEntity;)V"
            )
    )
    private void onReleaseUsing(ClientPlayerInteractionManager instance, PlayerEntity player) {
        if (!Quiver.charging && !FastEat.eating()) {
            instance.stopUsingItem(player);
        }
    }

    @Redirect(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"))
    private boolean shouldKeepUsing(KeyBinding instance) {
        FastRiptide fastRiptide = FastRiptide.getInstance();
        ItemStack activeItem = BlackOut.mc.player != null ? BlackOut.mc.player.getActiveItem() : null;
        return fastRiptide.enabled && activeItem != null && activeItem.getItem() instanceof TridentItem
                ? System.currentTimeMillis() - fastRiptide.prevRiptide < fastRiptide.cooldown.get() * 1000.0
                : instance.isPressed();
    }

    @Inject(method = "onResolutionChanged", at = @At("TAIL"))
    private void onResize(CallbackInfo ci) {
        Managers.FRAME_BUFFER.onResize();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getWindowTitle()Ljava/lang/String;"))
    private String windowTitle(MinecraftClient instance) {
        return this.getBOTitle();
    }

    @Redirect(method = "updateWindowTitle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getWindowTitle()Ljava/lang/String;"))
    private String updateTitle(MinecraftClient instance) {
        return this.getBOTitle();
    }

    @ModifyConstant(method = "doItemUse", constant = @Constant(intValue = 4))
    private int itemUseCooldown(int constant) {
        FastUse fastUse = FastUse.getInstance();
        if (fastUse.enabled && fastUse.timing.get() == FastUse.Timing.Tick) {
            ItemStack stack = fastUse.getStack();
            return fastUse.isValid(stack) ? fastUse.delayTicks.get() : 4;
        } else {
            return 4;
        }
    }

    @Redirect(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;isBreakingBlock()Z"))
    private boolean multiTaskThingy(ClientPlayerInteractionManager instance) {
        MultiTask multiTask = MultiTask.getInstance();
        FastUse fastUse = FastUse.getInstance();
        if (fastUse.enabled) {
            ItemStack stack = fastUse.getStack();
            if (fastUse.isValid(stack) && fastUse.rotateIfNeeded(stack)) {
                return false;
            }
        }

        return !multiTask.enabled && instance.isBreakingBlock();
    }

    @Unique
    private String getBOTitle() {
        return BlackOut.NAME + " Client " + BlackOut.VERSION;
    }
}
