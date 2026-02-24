package bodevelopment.client.blackout.module.modules.visual.entities;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.interfaces.mixin.IRaycastContext;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.DamageUtils;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import bodevelopment.client.blackout.util.RotationUtils;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class Sight extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<Double> lineWidth = this.sgGeneral.doubleSetting("Line Width", 1.5, 0.5, 5.0, 0.05, ".");
    private final Setting<BlackOutColor> lineColor = this.sgGeneral.colorSetting("Line Color", new BlackOutColor(255, 0, 0, 255), "");
    private final Setting<Double> fadeIn = this.sgGeneral.doubleSetting("Fade In", 1.0, 0.0, 50.0, 0.5, "");
    private final Setting<Double> length = this.sgGeneral.doubleSetting("Length", 5.0, 0.0, 50.0, 0.5, "");
    private final MatrixStack stack = new MatrixStack();

    public Sight() {
        super("Sight", "Shows where people are looking at.", SubCategory.ENTITIES, true);
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        this.stack.push();
        Render3DUtils.setRotation(this.stack);
        Render3DUtils.start();
        RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
        RenderSystem.lineWidth(this.lineWidth.get().floatValue());

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        RenderSystem.applyModelViewMatrix();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);

        MatrixStack.Entry entry = this.stack.peek();
        Matrix4f matrix4f = entry.getPositionMatrix();
        Vec3d camPos = BlackOut.mc.gameRenderer.getCamera().getPos();
        BlackOut.mc
                .world
                .getPlayers()
                .forEach(
                        player -> {
                            if (player != BlackOut.mc.player) {
                                Vec3d eyePos = OLEPOSSUtils.getLerpedPos(player, event.tickDelta).add(0.0, player.getEyeHeight(player.getPose()), 0.0);
                                Vec3d lookPos = RotationUtils.rotationVec(
                                        MathHelper.lerp(BlackOut.mc.getRenderTickCounter().getTickDelta(true), player.prevYaw, player.getYaw()),
                                        MathHelper.lerp(BlackOut.mc.getRenderTickCounter().getTickDelta(true), player.prevPitch, player.getPitch()),
                                        eyePos,
                                        this.fadeIn.get() + this.length.get()
                                );
                                ((IRaycastContext) DamageUtils.raycastContext).blackout_Client$set(eyePos, lookPos);
                                BlockHitResult hitResult = DamageUtils.raycast(DamageUtils.raycastContext, false);
                                Vec3d hitPos;
                                if (hitResult.getType() == HitResult.Type.MISS) {
                                    hitPos = lookPos;
                                } else {
                                    hitPos = hitResult.getPos();
                                }

                                this.render(bufferBuilder, matrix4f, entry, eyePos.subtract(camPos), hitPos.subtract(camPos));
                            }
                        }
                );
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();

        Render3DUtils.end();
        this.stack.pop();
    }

    private void render(BufferBuilder bufferBuilder, Matrix4f matrix4f, MatrixStack.Entry entry, Vec3d start, Vec3d end) {
        double l = start.distanceTo(end);
        if (l != 0.0) {
            double lerpDelta = this.fadeIn.get() / l;
            Vec3d lerpedPos = start.lerp(end, Math.min(lerpDelta, 1.0));
            Vec3d normal = lerpedPos.subtract(start).normalize();
            bufferBuilder.vertex(matrix4f, (float) start.x, (float) start.y, (float) start.z)
                    .color(ColorUtils.withAlpha(this.lineColor.get().getRGB(), 0))
                    .normal(entry, (float) normal.x, (float) normal.y, (float) normal.z)
                    ;
            bufferBuilder.vertex(matrix4f, (float) lerpedPos.x, (float) lerpedPos.y, (float) lerpedPos.z)
                    .color(ColorUtils.withAlpha(this.lineColor.get().getRGB(), Math.min((int) (1.0 / lerpDelta * 255.0), 255)))
                    .normal(entry, (float) normal.x, (float) normal.y, (float) normal.z)
                    ;
            if (!(lerpDelta >= 1.0)) {
                Vec3d normal2 = end.subtract(lerpedPos).normalize();
                bufferBuilder.vertex(matrix4f, (float) lerpedPos.x, (float) lerpedPos.y, (float) lerpedPos.z)
                        .color(this.lineColor.get().getRGB())
                        .normal(entry, (float) normal2.x, (float) normal2.y, (float) normal2.z)
                        ;
                bufferBuilder.vertex(matrix4f, (float) end.x, (float) end.y, (float) end.z)
                        .color(this.lineColor.get().getRGB())
                        .normal(entry, (float) normal2.x, (float) normal2.y, (float) normal2.z)
                        ;
            }
        }
    }
}
