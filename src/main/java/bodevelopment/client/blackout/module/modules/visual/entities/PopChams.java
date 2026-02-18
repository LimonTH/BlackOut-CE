package bodevelopment.client.blackout.module.modules.visual.entities;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.PopEvent;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.randomstuff.timers.TimerList;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import bodevelopment.client.blackout.util.render.WireframeRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class PopChams extends Module {
    private static PopChams INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<Double> time = this.sgGeneral.d("Time", 1.0, 0.0, 5.0, 0.05, ".");
    private final Setting<Double> y = this.sgGeneral.d("Y", 0.0, -5.0, 5.0, 0.1, ".");
    private final Setting<Double> scale = this.sgGeneral.d("Scale", 1.0, 0.0, 5.0, 0.1, ".");
    private final Setting<Boolean> enemy = this.sgGeneral.b("Enemy", true, ".");
    private final Setting<Boolean> friends = this.sgGeneral.b("Friends", true, ".");
    private final Setting<Boolean> self = this.sgGeneral.b("Self", false, ".");
    private final Setting<RenderShape> renderShape = this.sgGeneral.e("Render Shape", RenderShape.Full, "Which parts of boxes should be rendered.");
    private final Setting<BlackOutColor> lineColor = this.sgGeneral.c("Line Color", new BlackOutColor(255, 255, 255, 255), "Fill Color");
    private final Setting<BlackOutColor> sideColor = this.sgGeneral.c("Side Color", new BlackOutColor(255, 255, 255, 50), "Side Color");
    private final TimerList<Pop> pops = new TimerList<>(true);
    private final MatrixStack matrixStack = new MatrixStack();

    public PopChams() {
        super("Pop Chams", ".", SubCategory.ENTITIES, true);
        INSTANCE = this;
    }

    public static PopChams getInstance() {
        return INSTANCE;
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.player != null && BlackOut.mc.world != null) {
            this.matrixStack.push();
            Render3DUtils.setRotation(this.matrixStack);
            this.pops.forEach(timer -> this.renderPop(timer.value, MathHelper.getLerpProgress(System.currentTimeMillis(), timer.startTime, timer.endTime)));
            this.matrixStack.pop();
        }
    }

    @Event
    public void onPop(PopEvent event) {
        if (this.shouldRender(event.player)) {
            this.pops.add(new Pop(event.player), this.time.get());
        }
    }

    private boolean shouldRender(AbstractClientPlayerEntity player) {
        if (player == BlackOut.mc.player) {
            return this.self.get();
        } else {
            return Managers.FRIENDS.isFriend(player) ? this.friends.get() : this.enemy.get();
        }
    }

    private void renderPop(Pop pop, float progress) {
        Render3DUtils.start();
        WireframeRenderer.provider.consumer.start();
        this.drawPlayer(pop.player, pop, this.y.get() * progress, MathHelper.lerp(progress, 0.9375F, this.scale.get().floatValue() * 0.9375F));
        List<Vec3d[]> positions = WireframeRenderer.provider.consumer.positions;
        BlackOutColor sides = this.sideColor.get().alphaMulti(1.0F - progress);
        BlackOutColor lines = this.lineColor.get().alphaMulti(1.0F - progress);
        if (this.renderShape.get().sides) {
            WireframeRenderer.drawQuads(positions, sides.red / 255.0F, sides.green / 255.0F, sides.blue / 255.0F, sides.alpha / 255.0F);
        }

        if (this.renderShape.get().outlines) {
            WireframeRenderer.drawLines(positions, lines.red / 255.0F, lines.green / 255.0F, lines.blue / 255.0F, lines.alpha / 255.0F);
        }

        Render3DUtils.end();
    }

    private void drawPlayer(AbstractClientPlayerEntity player, Pop pop, double extraY, float scale) {
        EntityRenderer<? super AbstractClientPlayerEntity> entityRenderer = BlackOut.mc.getEntityRenderDispatcher().getRenderer(player);

        if (!(entityRenderer instanceof LivingEntityRenderer)) return;

        Vec3d cameraPos = BlackOut.mc.gameRenderer.getCamera().getPos();
        double x = pop.x - cameraPos.x;
        double y = pop.y - cameraPos.y + extraY;
        double z = pop.z - cameraPos.z;

        this.matrixStack.push();
        this.matrixStack.translate(x, y, z);
        WireframeRenderer.hidden = true;

        if (entityRenderer instanceof LivingEntityRenderer<?, ?> livingRenderer) {
            @SuppressWarnings("unchecked")
            var castedRenderer = (LivingEntityRenderer<AbstractClientPlayerEntity, ?>) livingRenderer;
            this.renderModel(castedRenderer, pop, scale);
        }

        WireframeRenderer.hidden = false;
        this.matrixStack.pop();
    }

    @SuppressWarnings("unchecked")
    private void renderModel(LivingEntityRenderer<AbstractClientPlayerEntity, ?> renderer, Pop pop, float scale) {
        EntityModel<?> rawModel = renderer.getModel();
        if (!(rawModel instanceof BipedEntityModel)) return;
        BipedEntityModel<AbstractClientPlayerEntity> model = (BipedEntityModel<AbstractClientPlayerEntity>) rawModel;
        this.matrixStack.push();
        model.handSwingProgress = pop.swingProgress;
        model.riding = pop.riding;
        model.child = false;

        float h = pop.bodyYaw;
        float j = pop.headYaw;
        float k = j - h;

        if (pop.hasVehicle) {
            h = pop.vehicleYaw;
            k = j - h;
            float l = MathHelper.clamp(MathHelper.wrapDegrees(k), -85.0F, 85.0F);
            h = j - l;
            if (l * l > 2500.0F) {
                h += l * 0.2F;
            }
            k = j - h;
        }

        float m = pop.pitch;
        if (pop.flip) {
            m *= -1.0F;
            k *= -1.0F;
        }

        if (pop.sleeping) {
            Direction direction = pop.sleepDir;
            if (direction != null) {
                float height = pop.eyeHeight;
                this.matrixStack.translate(-direction.getOffsetX() * height, 0.0F, -direction.getOffsetZ() * height);
            }
        }

        float l = pop.animationProgress;
        this.matrixStack.scale(scale, -scale, -scale);
        this.matrixStack.translate(0.0F, -1.501F, 0.0F);

        float o = !pop.hasVehicle ? pop.limbPos : 0.0F;
        float n = !pop.hasVehicle ? Math.min(pop.limbSpeed, 1.0F) : 0.0F;

        this.matrixStack.multiply(RotationAxis.POSITIVE_Y.rotation((float) Math.toRadians(h)));

        model.leaningPitch = pop.leaningPitch;

        model.setAngles(pop.player, o, n, l, k, m);
        model.render(this.matrixStack, WireframeRenderer.provider.getBuffer(null), 69420, 0, ColorUtils.intColor(1, 1, 1, 1));

        this.matrixStack.pop();
    }

    private static class Pop {
        private final AbstractClientPlayerEntity player;
        private final boolean riding;
        private final double x;
        private final double y;
        private final double z;
        private final boolean flip;
        private final float pitch;
        private final float bodyYaw;
        private final float headYaw;
        private final float swingProgress;
        private final boolean hasVehicle;
        private final float vehicleYaw;
        private final float eyeHeight;
        private final float animationProgress;
        private final float leaningPitch;
        private final float limbSpeed;
        private final float limbPos;
        private final boolean sleeping;
        private final Direction sleepDir;

        public Pop(AbstractClientPlayerEntity player) {
            this.player = player;
            float tickDelta = BlackOut.mc.getRenderTickCounter().getTickDelta(true);
            this.riding = player.hasVehicle();
            this.bodyYaw = MathHelper.lerp(tickDelta, player.prevBodyYaw, player.bodyYaw);
            this.headYaw = MathHelper.lerp(tickDelta, player.prevHeadYaw, player.headYaw);
            if (player.getVehicle() instanceof LivingEntity livingEntity) {
                this.hasVehicle = true;
                this.vehicleYaw = MathHelper.lerpAngleDegrees(tickDelta, livingEntity.prevBodyYaw, livingEntity.bodyYaw);
            } else {
                this.hasVehicle = false;
                this.vehicleYaw = 0.0F;
            }

            this.flip = LivingEntityRenderer.shouldFlipUpsideDown(player);
            this.pitch = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());
            this.eyeHeight = player.getEyeHeight(EntityPose.STANDING) - 0.1F;
            this.animationProgress = player.age + tickDelta;
            this.leaningPitch = player.getLeaningPitch(tickDelta);
            this.limbSpeed = player.limbAnimator.getSpeed(tickDelta);
            this.limbPos = player.limbAnimator.getPos(tickDelta);
            this.swingProgress = player.getHandSwingProgress(tickDelta);
            this.x = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX());
            this.y = MathHelper.lerp(tickDelta, player.lastRenderY, player.getY());
            this.z = MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ());
            this.sleeping = player.isInPose(EntityPose.SLEEPING);
            this.sleepDir = player.getSleepingDirection();
        }
    }
}
