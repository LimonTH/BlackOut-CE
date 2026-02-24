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
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Wireframe extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");
    private final Setting<RenderShape> renderShape = this.sgGeneral.enumSetting("Render Shape", RenderShape.Full, ".");
    private final Setting<BlackOutColor> lineColor = this.sgGeneral.colorSetting("Line Color", new BlackOutColor(255, 0, 0, 255), ".");
    private final Setting<BlackOutColor> sideColor = this.sgGeneral.colorSetting("Side Color", new BlackOutColor(255, 0, 0, 50), ".");
    private final List<AbstractClientPlayerEntity> player = new ArrayList<>();

    public Wireframe() {
        super("Wireframe", "Draws a wireframe of players", SubCategory.ENTITIES, true);
    }

    @Event
    public void onTickPost(TickEvent.Post event) {
        if (BlackOut.mc.world != null && BlackOut.mc.player != null) {
            this.player.clear();
            BlackOut.mc.world.entityList.forEach(entity -> {
                if (entity instanceof AbstractClientPlayerEntity && this.shouldRender(entity)) {
                    this.player.add((AbstractClientPlayerEntity) entity);
                }
            });
            this.player.sort(Comparator.comparingDouble(entity -> -BlackOut.mc.player.distanceTo(entity)));
        }
    }

    public boolean shouldRender(Entity entity) {
        AntiBot antiBot = AntiBot.getInstance();
        return (!antiBot.enabled || antiBot.mode.get() != AntiBot.HandlingMode.Ignore || !antiBot.getBots().contains(entity)) && entity != BlackOut.mc.player;
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.world == null || BlackOut.mc.player == null) return;

        Camera camera = BlackOut.mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        float tickDelta = event.tickDelta;

        this.player.forEach(entity -> {
            WireframeRenderer.ModelData data = new WireframeRenderer.ModelData(entity, tickDelta);

            event.stack.push();

            event.stack.loadIdentity();
            event.stack.multiply(new Quaternionf(camera.getRotation()).conjugate());

            double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX()) - camPos.x;
            double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()) - camPos.y;
            double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ()) - camPos.z;

            event.stack.translate((float) x, (float) y, (float) z);

            WireframeRenderer.renderModel(
                    event.stack,
                    entity,
                    data,
                    this.lineColor.get(),
                    this.sideColor.get(),
                    this.renderShape.get()
            );

            event.stack.pop();
        });
    }
}
