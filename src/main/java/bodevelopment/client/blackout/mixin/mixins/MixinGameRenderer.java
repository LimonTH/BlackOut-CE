package bodevelopment.client.blackout.mixin.mixins;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.modules.combat.misc.NoTrace;
import bodevelopment.client.blackout.module.modules.misc.Reach;
import bodevelopment.client.blackout.module.modules.visual.misc.*;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import bodevelopment.client.blackout.randomstuff.timers.TimerMap;
import bodevelopment.client.blackout.rendering.shader.Shaders;
import bodevelopment.client.blackout.rendering.texture.BOTextures;
import bodevelopment.client.blackout.util.SharedFeatures;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Shadow
    public abstract void resetData();

    @Shadow
    public abstract void render(DeltaTracker tickCounter, boolean tick);

    @Shadow
    public abstract boolean isPanoramicMode();

    @Shadow
    protected abstract void renderItemInHand(Camera camera, float tickDelta, Matrix4f matrix4f);

    @Redirect(
            method = "render",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;", opcode = Opcodes.GETFIELD)
    )
    private Screen redirectCurrentScreen(Minecraft instance) {
        return instance.screen instanceof ContainerScreen && SharedFeatures.shouldSilentScreen() ? null : instance.screen;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void postRender(DeltaTracker tickCounter, boolean tick, CallbackInfo ci) {
        Managers.PING.update();
    }

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"
            )
    )
    private void onRenderWorldPre(DeltaTracker tickCounter, CallbackInfo ci) {
        PoseStack matrices = new PoseStack();
        float tickDelta = tickCounter.getGameTimeDeltaPartialTick(true);

        TimerList.updating.forEach(TimerList::update);
        TimerMap.updating.forEach(TimerMap::update);

        BlackOut.EVENT_BUS.post(RenderEvent.World.Pre.get(matrices, tickDelta));
    }

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onRenderWorldPost(DeltaTracker tickCounter, CallbackInfo ci) {
        PoseStack matrices = new PoseStack();
        BlackOut.EVENT_BUS.post(RenderEvent.World.Post.get(matrices, tickCounter.getGameTimeDeltaPartialTick(true)));
    }

    @Redirect(
            method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/Camera;FLorg/joml/Matrix4f;)V")
    )
    private void renderHeldItems(GameRenderer instance, Camera camera, float tickDelta, Matrix4f matrix4f) {
        HandESP.getInstance().draw(() -> this.renderItemInHand(camera, tickDelta, matrix4f));
    }

    @Inject(method = "preloadUiShader", at = @At("TAIL"))
    private void onShaderLoad(ResourceProvider factory, CallbackInfo ci) {
        Shaders.loadPrograms();
        BlackOut.FONT.loadFont();
        BlackOut.BOLD_FONT.loadFont();
        BOTextures.init();
    }

    @Redirect(
            method = "pick(F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;blockInteractionRange()D"
            )
    )
    private double getBlockReach(LocalPlayer instance) {
        Reach reach = Reach.getInstance();
        return reach.enabled ? reach.blockReach.get() : instance.blockInteractionRange();
    }

    @Redirect(
            method = "pick(F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;entityInteractionRange()D"
            )
    )
    private double getEntityReach(LocalPlayer instance) {
        Reach reach = Reach.getInstance();
        return reach.enabled ? reach.entityReach.get() : instance.entityInteractionRange();
    }

    @Redirect(
            method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;")
    )
    private HitResult raycast(Entity instance, double maxDistance, float tickDelta, boolean includeFluids) {
        FreeCam freecam = FreeCam.getInstance();
        if (!freecam.enabled) {
            return instance.pick(maxDistance, tickDelta, includeFluids);
        } else {
            Vec3 start = freecam.pos;
            Vec3 rotation = instance.getViewVector(tickDelta);
            Vec3 end = start.add(rotation.x * maxDistance, rotation.y * maxDistance, rotation.z * maxDistance);
            return instance.level().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, includeFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, instance));
        }
    }

    @Redirect(
            method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;")
    )
    private Vec3 cameraPos(Entity instance, float tickDelta) {
        FreeCam freecam = FreeCam.getInstance();
        return freecam.enabled ? freecam.pos : instance.getEyePosition(tickDelta);
    }

    @Redirect(
            method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"
            )
    )
    private EntityHitResult raycastEntities(Entity entity, Vec3 min, Vec3 max, AABB box, Predicate<Entity> predicate, double d) {
        return NoTrace.getInstance().enabled ? null : ProjectileUtil.getEntityHitResult(entity, min, max, box, predicate, d);
    }

    @Inject(method = "getFov", at = @At("HEAD"), cancellable = true)
    private void getFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        FovModifier modifier = FovModifier.getInstance();
        if (modifier.enabled) {
            cir.setReturnValue((float) this.getFOV(changingFov, modifier));
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void onBobView(PoseStack matrices, float tickDelta, CallbackInfo ci) {
        if (NoRender.getInstance().enabled && NoRender.getInstance().noBobbing.get()) {
            ci.cancel();
        }
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void onHurtCameraEffect(PoseStack matrices, float tickDelta, CallbackInfo ci) {
        if (NoRender.getInstance().enabled && NoRender.getInstance().noHurtCam.get()) {
            ci.cancel();
        }
    }

    @Unique
    private double getFOV(boolean changing, FovModifier fovModifier) {
        if (this.isPanoramicMode()) {
            return 90.0;
        } else if (!changing) {
            ViewModel handView = ViewModel.getInstance();
            return handView.enabled ? handView.fov.get() : Minecraft.getInstance().options.fov().get().doubleValue();
        } else {
            return fovModifier.getFOV();
        }
    }

    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void outlineRender(CallbackInfoReturnable<Boolean> cir) {
        if (Highlight.getInstance().enabled) {
            cir.setReturnValue(false);
        }
    }
}