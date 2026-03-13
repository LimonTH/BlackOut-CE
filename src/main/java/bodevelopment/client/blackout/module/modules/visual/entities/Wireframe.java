package bodevelopment.client.blackout.module.modules.visual.entities;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.misc.AntiBot;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.render.WireframeRenderer;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class Wireframe extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<RenderShape> renderShape = this.sgGeneral.enumSetting("Mesh Mode", RenderShape.Full, "Defines which geometric components of the player model are rendered (outlines, faces, or both).");
    private final Setting<BlackOutColor> lineColor = this.sgGeneral.colorSetting("Wireframe Color", new BlackOutColor(255, 0, 0, 255), "The color and opacity of the polygonal edges.");
    private final Setting<BlackOutColor> sideColor = this.sgGeneral.colorSetting("Surface Color", new BlackOutColor(255, 0, 0, 50), "The color and opacity of the model's polygon faces.");

    private final List<AbstractClientPlayer> player = new ArrayList<>();

    public Wireframe() {
        super("Wireframe", "Renders a structural shell over player entities to visualize their model geometry and joint rotations.", SubCategory.ENTITIES, true);
    }

    @Event
    public void onTickPost(TickEvent.Post event) {
        if (BlackOut.mc.level != null && BlackOut.mc.player != null) {
            this.player.clear();
            BlackOut.mc.level.tickingEntities.forEach(entity -> {
                if (entity instanceof AbstractClientPlayer && this.shouldRender(entity)) {
                    this.player.add((AbstractClientPlayer) entity);
                }
            });
            this.player.sort(Comparator.comparingDouble(entity -> -BlackOut.mc.player.distanceTo(entity)));
        }
    }

    public boolean shouldRender(Entity entity) {
        AntiBot antiBot = AntiBot.getInstance();

        if (antiBot.enabled && antiBot.mode.get() == AntiBot.HandlingMode.Ignore) {
            if (entity instanceof AbstractClientPlayer playerx && antiBot.getBots().contains(playerx)) {
                return false;
            }
        }

        return entity != BlackOut.mc.player;
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.level == null || BlackOut.mc.player == null) return;

        Camera camera = BlackOut.mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        float tickDelta = event.tickDelta;

        this.player.forEach(entity -> {
            WireframeRenderer.ModelData data = new WireframeRenderer.ModelData(entity, tickDelta);

            event.stack.pushPose();

            event.stack.setIdentity();
            event.stack.mulPose(new Quaternionf(camera.rotation()).conjugate());

            double x = Mth.lerp(tickDelta, entity.xOld, entity.getX()) - camPos.x;
            double y = Mth.lerp(tickDelta, entity.yOld, entity.getY()) - camPos.y;
            double z = Mth.lerp(tickDelta, entity.zOld, entity.getZ()) - camPos.z;

            event.stack.translate((float) x, (float) y, (float) z);

            WireframeRenderer.renderModel(
                    event.stack,
                    entity,
                    data,
                    this.lineColor.get(),
                    this.sideColor.get(),
                    this.renderShape.get()
            );

            event.stack.popPose();
        });
    }
}
