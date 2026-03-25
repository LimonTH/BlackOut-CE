package bodevelopment.client.blackout.module.modules.visual.entities;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.interfaces.mixin.IClipContext;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.ColorUtils;
import bodevelopment.client.blackout.util.DamageUtils;
import bodevelopment.client.blackout.util.EntityUtils;
import bodevelopment.client.blackout.util.RotationUtils;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class Sight extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<Double> lineWidth = this.sgGeneral.doubleSetting("Stroke Weight", 1.5, 0.5, 5.0, 0.05, "The thickness of the rendered view vector lines.");
    private final Setting<BlackOutColor> lineColor = this.sgGeneral.colorSetting("Line Color", new BlackOutColor(255, 0, 0, 255), "The primary color for the gaze indicators.");
    private final Setting<Double> fadeIn = this.sgGeneral.doubleSetting("Proximal Fade", 1.0, 0.0, 50.0, 0.5, "The distance from the player's eyes where the line starts to transition from transparent to opaque.");
    private final Setting<Double> length = this.sgGeneral.doubleSetting("Vector Magnitude", 5.0, 0.0, 50.0, 0.5, "The maximum distance the sight line extends before termination.");

    private final PoseStack stack = new PoseStack();

    public Sight() {
        super("Sight", "Traces the visual trajectory of other players by rendering gaze vectors that reflect their current pitch and yaw.", SubCategory.ENTITIES, true);
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        this.stack.pushPose();
        Render3DUtils.setRotation(this.stack);
        Render3DUtils.start();

        RenderSystem.setShader(CoreShaders.RENDERTYPE_LINES);
        RenderSystem.lineWidth(this.lineWidth.get().floatValue());

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

        PoseStack.Pose entry = this.stack.last();
        Matrix4f matrix4f = entry.pose();
        Vec3 camPos = BlackOut.mc.gameRenderer.getMainCamera().getPosition();

        BlackOut.mc.level.players().forEach(player -> {
            if (player != BlackOut.mc.player) {
                float tickDelta = BlackOut.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

                Vec3 eyePos = EntityUtils.getLerpedPos(player, tickDelta).add(0.0, player.getEyeHeight(player.getPose()), 0.0);

                Vec3 lookPos = RotationUtils.rotationVec(
                        Mth.lerp(tickDelta, player.yRotO, player.getYRot()),
                        Mth.lerp(tickDelta, player.xRotO, player.getXRot()),
                        eyePos,
                        this.fadeIn.get() + this.length.get()
                );

                ((IClipContext) DamageUtils.raycastContext).blackout_Client$set(eyePos, lookPos);
                BlockHitResult hitResult = DamageUtils.raycast(DamageUtils.raycastContext, false);

                Vec3 hitPos = (hitResult.getType() == HitResult.Type.MISS) ? lookPos : hitResult.getLocation();

                this.render(bufferBuilder, matrix4f, entry, eyePos.subtract(camPos), hitPos.subtract(camPos));
            }
        });

        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

        Render3DUtils.end();
        this.stack.popPose();
    }

    private void render(BufferBuilder bufferBuilder, Matrix4f matrix4f, PoseStack.Pose entry, Vec3 start, Vec3 end) {
        double l = start.distanceTo(end);
        if (l != 0.0) {
            double lerpDelta = this.fadeIn.get() / l;
            Vec3 lerpedPos = start.lerp(end, Math.min(lerpDelta, 1.0));
            Vec3 normal = lerpedPos.subtract(start).normalize();
            bufferBuilder.addVertex(matrix4f, (float) start.x, (float) start.y, (float) start.z)
                    .setColor(ColorUtils.withAlpha(this.lineColor.get().getRGB(), 0))
                    .setNormal(entry, (float) normal.x, (float) normal.y, (float) normal.z)
                    ;
            bufferBuilder.addVertex(matrix4f, (float) lerpedPos.x, (float) lerpedPos.y, (float) lerpedPos.z)
                    .setColor(ColorUtils.withAlpha(this.lineColor.get().getRGB(), Math.min((int) (1.0 / lerpDelta * 255.0), 255)))
                    .setNormal(entry, (float) normal.x, (float) normal.y, (float) normal.z)
                    ;
            if (!(lerpDelta >= 1.0)) {
                Vec3 normal2 = end.subtract(lerpedPos).normalize();
                bufferBuilder.addVertex(matrix4f, (float) lerpedPos.x, (float) lerpedPos.y, (float) lerpedPos.z)
                        .setColor(this.lineColor.get().getRGB())
                        .setNormal(entry, (float) normal2.x, (float) normal2.y, (float) normal2.z)
                        ;
                bufferBuilder.addVertex(matrix4f, (float) end.x, (float) end.y, (float) end.z)
                        .setColor(this.lineColor.get().getRGB())
                        .setNormal(entry, (float) normal2.x, (float) normal2.y, (float) normal2.z)
                        ;
            }
        }
    }
}
