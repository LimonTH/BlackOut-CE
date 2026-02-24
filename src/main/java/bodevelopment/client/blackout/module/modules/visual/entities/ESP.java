package bodevelopment.client.blackout.module.modules.visual.entities;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.misc.AntiBot;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.RotationUtils;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import bodevelopment.client.blackout.util.render.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class ESP extends Module {
    private static ESP INSTANCE;
    private final SettingGroup sgGeneral = this.addGroup("General");
    public final Setting<Boolean> renderName = this.sgGeneral.booleanSetting("Render Name", false, ".");
    private final Setting<NameMode> nameMode = this.sgGeneral.enumSetting("Name Mode", NameMode.EntityName, "", this.renderName::get);
    private final Setting<List<EntityType<?>>> entityTypes = this.sgGeneral.entityListSetting("Entities", ".", EntityType.PLAYER);
    private final Setting<Boolean> hp = this.sgGeneral.booleanSetting("Show HP", true, ".");
    private final Setting<BlackOutColor> mxhp = this.sgGeneral.colorSetting("Max HP Color", new BlackOutColor(115, 115, 255, 200), ".", this.hp::get);
    private final Setting<BlackOutColor> mnhp = this.sgGeneral.colorSetting("Min HP Color", new BlackOutColor(255, 30, 30, 200), ".", this.hp::get);
    private final Setting<Boolean> box = this.sgGeneral.booleanSetting("Box", true, "Draws a box around the entity");
    private final Setting<Boolean> fill = this.sgGeneral.booleanSetting("Box Fill", false, "Fills the box");
    private final Setting<Boolean> fadeFill = this.sgGeneral.booleanSetting("Fade Fill", false, ".", this.fill::get);
    private final Setting<BlackOutColor> fillColor = this.sgGeneral.colorSetting("Fill Color", new BlackOutColor(255, 255, 255, 50), ".", this.fill::get);
    private final Setting<Boolean> renderItem = this.sgGeneral.booleanSetting("Render Item", false, ".");
    private final Setting<BlackOutColor> txt = this.sgGeneral.colorSetting("Text Color", new BlackOutColor(255, 255, 255, 255), ".");
    private final Setting<BlackOutColor> lineColor = this.sgGeneral.colorSetting("Line Color", new BlackOutColor(255, 255, 255, 200), ".");
    private final Setting<BlackOutColor> fadeColor = this.sgGeneral.colorSetting("Line Fade Color", new BlackOutColor(16, 16, 16, 200), ".");
    private final MatrixStack stack = new MatrixStack();
    private final List<Entity> entities = new ArrayList<>();
    private float progress = 0.0F;

    public ESP() {
        super("ESP", "Extra Sensory Perception", SubCategory.ENTITIES, true);
        INSTANCE = this;
    }

    public static ESP getInstance() {
        return INSTANCE;
    }

    @Event
    public void onTickPost(TickEvent.Post event) {
        if (BlackOut.mc.world != null && BlackOut.mc.player != null) {
            this.entities.clear();
            BlackOut.mc.world.entityList.forEach(entity -> {
                if (this.shouldRender(entity)) {
                    this.entities.add(entity);
                }
            });
            this.entities.sort(Comparator.comparingDouble(entity -> -BlackOut.mc.player.distanceTo(entity)));
        }
    }

    public boolean shouldRender(Entity entity) {
        AntiBot antiBot = AntiBot.getInstance();
        return (!antiBot.enabled || antiBot.mode.get() != AntiBot.HandlingMode.Ignore || !(entity instanceof AbstractClientPlayerEntity player) || !antiBot.getBots().contains(player)) && entity != BlackOut.mc.player && this.entityTypes.get().contains(entity.getType());
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.world != null && BlackOut.mc.player != null) {
            Vec3d cameraPos = BlackOut.mc.gameRenderer.getCamera().getPos();

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);

            this.entities.forEach(entity -> this.render2D(event.tickDelta, cameraPos, entity));

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        }
    }

    public void render2D(double tickDelta, Vec3d cameraPos, Entity entity) {
        double x = MathHelper.lerp(tickDelta, entity.prevX, entity.getX()) - cameraPos.x;
        double y = MathHelper.lerp(tickDelta, entity.prevY, entity.getY()) - cameraPos.y + entity.getHeight() / 2.0F;
        double z = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ()) - cameraPos.z;
        float s = 1.25F;
        double cameraPitch = Math.abs(BlackOut.mc.gameRenderer.getCamera().getPitch() / 90.0F);
        double anglePitch = Math.abs(RotationUtils.getPitch(new Vec3d(x, y, z), Vec3d.ZERO) / 90.0);
        double yaw1 = 90.0
                - Math.abs(
                90.0
                        - Math.abs(
                        RotationUtils.yawAngle(
                                BlackOut.mc.gameRenderer.getCamera().getYaw() + 180.0F, RotationUtils.getYaw(new Vec3d(x, y, z), Vec3d.ZERO, 0.0)
                        )
                )
        );
        double yaw = yaw1 / 90.0;
        float width = this.getWidth(entity.getBoundingBox(), cameraPitch * (1.0 - anglePitch) * yaw);
        float height = this.getHeight(entity.getBoundingBox(), anglePitch);
        this.stack.push();
        Render3DUtils.setRotation(this.stack);
        this.stack.translate(x, y, z);
        this.stack.scale(s, -s, s);
        this.stack.multiply(RotationAxis.POSITIVE_Y.rotation((float) Math.toRadians(-BlackOut.mc.gameRenderer.getCamera().getYaw() + 180.0F)));
        this.stack.multiply(RotationAxis.POSITIVE_X.rotation((float) Math.toRadians(BlackOut.mc.gameRenderer.getCamera().getPitch())));
        String name = this.nameMode.get().getName(entity);
        float textScale = 0.01F;
        if (this.renderName.get()) {
            BlackOut.FONT
                    .text(
                            this.stack,
                            name,
                            textScale,
                            -width / 2.0F + BlackOut.FONT.getWidth(name) * textScale / 2.0F,
                            -height / 2.0F - BlackOut.FONT.getHeight() * 1.2F * (textScale * 1.1F),
                            this.txt.get().getColor(),
                            true,
                            false
                    );
        }

        if (this.renderItem.get() && entity instanceof AbstractClientPlayerEntity && ((AbstractClientPlayerEntity) entity).getMainHandStack() != null) {
            String stackName = ((AbstractClientPlayerEntity) entity).getMainHandStack().getName().getString();
            BlackOut.FONT
                    .text(
                            this.stack,
                            stackName,
                            textScale,
                            -width / 2.0F + BlackOut.FONT.getWidth(stackName) * textScale / 2.0F,
                            height / 2.0F + BlackOut.FONT.getHeight() * (textScale * 1.1F),
                            this.txt.get().getColor(),
                            true,
                            false
                    );
        }

        if (this.box.get()) {
            RenderUtils.line(this.stack, -width / 2.0F, -height / 2.0F, -width / 2.0F, height / 2.0F, this.lineColor.get().getRGB(), this.fadeColor.get().getRGB());
            RenderUtils.line(this.stack, width / 2.0F, -height / 2.0F, width / 2.0F, height / 2.0F, this.lineColor.get().getRGB(), this.fadeColor.get().getRGB());
            RenderUtils.line(this.stack, -width / 2.0F, -height / 2.0F, width / 2.0F, -height / 2.0F, this.lineColor.get().getRGB());
            RenderUtils.line(this.stack, -width / 2.0F, height / 2.0F, width / 2.0F, height / 2.0F, this.fadeColor.get().getRGB());
        }

        if (this.fill.get() && !this.fadeFill.get()) {
            RenderUtils.quad(this.stack, -width / 2.0F, -height / 2.0F, width, height, this.fillColor.get().getRGB());
        }

        if (this.fill.get() && this.fadeFill.get()) {
            RenderUtils.topFade(this.stack, -width / 2.0F, -height / 2.0F, width, height, this.fillColor.get().getRGB());
        }

        if (entity instanceof LivingEntity livingEntity) {
            float frameTime = BlackOut.mc.getRenderTickCounter().getLastFrameDuration() / 20.0F * 4.0F;
            float targetProgress = Math.min((livingEntity.getHealth() + livingEntity.getAbsorptionAmount()) / livingEntity.getMaxHealth(), 1.0F);
            float progressDelta = frameTime + frameTime * Math.abs(targetProgress - this.progress);
            if (targetProgress > this.progress) {
                this.progress = Math.min(this.progress + progressDelta, targetProgress);
            } else {
                this.progress = Math.max(this.progress - progressDelta, targetProgress);
            }

            if (this.hp.get()) {
                RenderUtils.quad(this.stack, -width / 2.0F - 0.05F, height / 2.0F, 0.03F, height * -this.progress, this.getColor(this.progress).getRGB());
            }
        }

        this.stack.pop();
    }

    private float getWidth(Box box, double pitch) {
        return (float) MathHelper.lerp(Math.sin(pitch * Math.PI / 2.0), box.getLengthX(), box.getLengthY());
    }

    private float getHeight(Box box, double pitch) {
        return (float) MathHelper.lerp(Math.sin(pitch * Math.PI / 2.0), box.getLengthY(), box.getLengthX());
    }

    private Color getColor(float health) {
        return ColorUtils.lerpColor(Math.min(health, 1.0F), this.mnhp.get().getColor(), this.mxhp.get().getColor());
    }

    public enum NameMode {
        Display(entity -> entity.getDisplayName().getString()),
        EntityName(entity -> entity.getName().getString());

        private final Function<Entity, String> function;

        NameMode(Function<Entity, String> function) {
            this.function = function;
        }

        private String getName(Entity entity) {
            return this.function.apply(entity);
        }
    }
}
