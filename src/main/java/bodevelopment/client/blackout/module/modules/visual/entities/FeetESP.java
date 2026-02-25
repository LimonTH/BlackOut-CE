package bodevelopment.client.blackout.module.modules.visual.entities;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class FeetESP extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<List<EntityType<?>>> entities = this.sgGeneral.entityListSetting("Target Filters", "Specifies which entity categories will have their base coordinates highlighted.", EntityType.PLAYER);
    private final Setting<RenderShape> renderShape = this.sgGeneral.enumSetting("Mesh Mode", RenderShape.Full, "Defines which geometric components (faces, lines, or both) are rendered for the foot-level box.");
    private final Setting<BlackOutColor> fill = this.sgGeneral.colorSetting("Interior Color", new BlackOutColor(255, 255, 255, 80), "The color and transparency of the polygon faces at the entity's feet.");
    private final Setting<BlackOutColor> line = this.sgGeneral.colorSetting("Outline Color", new BlackOutColor(255, 255, 255, 120), "The color and transparency of the wireframe edges at the entity's feet.");

    public FeetESP() {
        super("Feet ESP", "Renders a discrete bounding box at the base of entities to highlight their exact ground position and collision footprint.", SubCategory.ENTITIES, true);
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.world == null || BlackOut.mc.player == null) return;

        Render3DUtils.start();

        BlackOut.mc.world.getEntities().forEach(entity -> {
            if (this.entities.get().contains(entity.getType())) {
                Vec3d pos = new Vec3d(entity.prevX, entity.prevY, entity.prevZ)
                        .lerp(entity.getPos(), BlackOut.mc.getRenderTickCounter().getTickDelta(true));

                double halfWidth = entity.getBoundingBox().getLengthX() / 2.0;
                double halfDepth = entity.getBoundingBox().getLengthZ() / 2.0;

                Box feetBox = new Box(
                        pos.x - halfWidth, pos.y, pos.z - halfDepth,
                        pos.x + halfWidth, pos.y + 0.01, pos.z + halfDepth
                );

                Render3DUtils.box(feetBox, fill.get(), line.get(), renderShape.get());
            }
        });

        Render3DUtils.end();
    }
}