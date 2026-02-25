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
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Shadow
    public abstract void reset();

    @Shadow
    public abstract void render(RenderTickCounter tickCounter, boolean tick);

    @Shadow
    public abstract boolean isRenderingPanorama();

    @Shadow
    protected abstract void renderHand(Camera camera, float tickDelta, Matrix4f matrix4f);

    @Redirect(
            method = "render",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;", opcode = Opcodes.GETFIELD)
    )
    private Screen redirectCurrentScreen(MinecraftClient instance) {
        return instance.currentScreen instanceof GenericContainerScreen && SharedFeatures.shouldSilentScreen() ? null : instance.currentScreen;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void postRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        Managers.PING.update();
    }

    @Inject(
            method = "renderWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lnet/minecraft/client/render/LightmapTextureManager;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"
            )
    )
    private void onRenderWorldPre(RenderTickCounter tickCounter, CallbackInfo ci, @Local MatrixStack matrices) {
        float tickDelta = tickCounter.getTickDelta(true);

        TimerList.updating.forEach(TimerList::update);
        TimerMap.updating.forEach(TimerMap::update);

        BlackOut.EVENT_BUS.post(RenderEvent.World.Pre.get(matrices, tickDelta));
    }

    @Inject(
            method = "renderWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lnet/minecraft/client/render/LightmapTextureManager;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onRenderWorldPost(RenderTickCounter tickCounter, CallbackInfo ci, @Local MatrixStack matrices) {
        BlackOut.EVENT_BUS.post(RenderEvent.World.Post.get(matrices, tickCounter.getTickDelta(true)));
    }

    @Redirect(
            method = "renderWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/GameRenderer;renderHand(Lnet/minecraft/client/render/Camera;FLorg/joml/Matrix4f;)V"
            )
    )
    private void renderHeldItems(GameRenderer instance, Camera camera, float tickDelta, org.joml.Matrix4f matrix4f) {
        HandESP.getInstance().draw(() -> this.renderHand(camera, tickDelta, matrix4f));
    }

    @Inject(method = "preloadPrograms", at = @At("TAIL"))
    private void onShaderLoad(ResourceFactory factory, CallbackInfo ci) {
        Shaders.loadPrograms();
        BlackOut.FONT.loadFont();
        BlackOut.BOLD_FONT.loadFont();
        BOTextures.init();
    }

    @Redirect(
            method = "updateCrosshairTarget",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getBlockInteractionRange()D")
    )
    private double getBlockReach(ClientPlayerEntity instance) {
        Reach reach = Reach.getInstance();
        return reach.enabled ? reach.blockReach.get() : instance.getBlockInteractionRange();
    }

    @Redirect(
            method = "updateCrosshairTarget",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getEntityInteractionRange()D")
    )
    private double getEntityReach(ClientPlayerEntity instance) {
        Reach reach = Reach.getInstance();
        return reach.enabled ? reach.entityReach.get() : instance.getEntityInteractionRange();
    }

    @Redirect(
            method = "findCrosshairTarget",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;raycast(DFZ)Lnet/minecraft/util/hit/HitResult;")
    )
    private HitResult raycast(Entity instance, double maxDistance, float tickDelta, boolean includeFluids) {
        FreeCam freecam = FreeCam.getInstance();
        if (!freecam.enabled) {
            return instance.raycast(maxDistance, tickDelta, includeFluids);
        } else {
            Vec3d start = freecam.pos;
            Vec3d rotation = instance.getRotationVec(tickDelta);
            Vec3d end = start.add(rotation.x * maxDistance, rotation.y * maxDistance, rotation.z * maxDistance);
            return instance.getWorld().raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, includeFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE, instance));
        }
    }

    @Redirect(
            method = "findCrosshairTarget",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getCameraPosVec(F)Lnet/minecraft/util/math/Vec3d;")
    )
    private Vec3d cameraPos(Entity instance, float tickDelta) {
        FreeCam freecam = FreeCam.getInstance();
        return freecam.enabled ? freecam.pos : instance.getCameraPosVec(tickDelta);
    }

    @Redirect(
            method = "findCrosshairTarget",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/projectile/ProjectileUtil;raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"
            )
    )
    private EntityHitResult raycastEntities(Entity entity, Vec3d min, Vec3d max, Box box, Predicate<Entity> predicate, double d) {
        return NoTrace.getInstance().enabled ? null : ProjectileUtil.raycast(entity, min, max, box, predicate, d);
    }

    @Inject(method = "getFov", at = @At("HEAD"), cancellable = true)
    private void getFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        FovModifier modifier = FovModifier.getInstance();
        if (modifier.enabled) {
            cir.setReturnValue(this.getFOV(changingFov, modifier));
            cir.cancel();
        }
    }

    @Unique
    private double getFOV(boolean changing, FovModifier fovModifier) {
        if (this.isRenderingPanorama()) {
            return 90.0;
        } else if (!changing) {
            ViewModel handView = ViewModel.getInstance();
            return handView.enabled ? handView.fov.get() : MinecraftClient.getInstance().options.getFov().getValue().doubleValue();
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