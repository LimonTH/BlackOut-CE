package bodevelopment.client.blackout.module.modules.visual.entities;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.misc.AntiBot;
import bodevelopment.client.blackout.module.modules.visual.misc.FreeCam;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.render.RenderUtils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Tracers extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<List<EntityType<?>>> entityTypes = this.sgGeneral.entityListSetting("Target Filters", "Specifies which entity types will have tracking lines drawn toward them.", EntityType.PLAYER);
    private final Setting<BlackOutColor> line = this.sgGeneral.colorSetting("Default Tracer Color", new BlackOutColor(255, 255, 255, 100), "The color of the tracer lines for standard entities.");
    private final Setting<BlackOutColor> friendLine = this.sgGeneral.colorSetting("Friend Tracer Color", new BlackOutColor(150, 150, 255, 100), "The color of the tracer lines for entities on your friend list.");

    private final MatrixStack stack = new MatrixStack();
    private final List<Entity> entities = new ArrayList<>();

    public Tracers() {
        super("Tracers", "Draws directional lines from the center of the screen to entities to provide spatial awareness of their locations.", SubCategory.ENTITIES, true);
    }

    @Event
    public void onTick(TickEvent.Post event) {
        if (BlackOut.mc.world != null && BlackOut.mc.player != null) {
            this.entities.clear();
            BlackOut.mc.world.entityList.forEach(entity -> {
                if (this.shouldRender(entity)) {
                    this.entities.add(entity);
                }
            });
            this.entities.sort(Comparator.comparingDouble(entity -> -BlackOut.mc.gameRenderer.getCamera().getPos().distanceTo(entity.getPos())));
        }
    }

    @Event
    public void onRender(RenderEvent.Hud.Post event) {
        if (BlackOut.mc.world != null && BlackOut.mc.player != null) {
            this.stack.push();
            RenderUtils.unGuiScale(this.stack);
            this.entities.forEach(entity -> this.renderTracer(event.tickDelta, entity));
            this.stack.pop();
        }
    }

    public void renderTracer(double tickDelta, Entity entity) {
        double x = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
        double y = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
        double z = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());
        this.stack.push();
        Color color;
        if (entity instanceof PlayerEntity && Managers.FRIENDS.isFriend((PlayerEntity) entity)) {
            color = this.friendLine.get().getColor();
        } else {
            color = this.line.get().getColor();
        }

        Vec2f f = RenderUtils.getCoords(x, y + entity.getBoundingBox().getLengthY() / 2.0, z, false);
        if (f == null) {
            this.stack.pop();
        } else {
            RenderUtils.line(
                    this.stack,
                    BlackOut.mc.getWindow().getWidth() / 2.0F,
                    BlackOut.mc.getWindow().getHeight() / 2.0F,
                    f.x,
                    f.y,
                    color.getRGB()
            );
            this.stack.pop();
        }
    }

    public boolean shouldRender(Entity entity) {
        AntiBot antiBot = AntiBot.getInstance();
        if (antiBot.enabled && antiBot.mode.get() == AntiBot.HandlingMode.Ignore && entity instanceof AbstractClientPlayerEntity player && antiBot.getBots().contains(player)) {
            return false;
        } else if (!this.entityTypes.get().contains(entity.getType())) {
            return false;
        } else {
            return entity != BlackOut.mc.player || FreeCam.getInstance().enabled;
        }
    }
}
