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
    private final Setting<List<EntityType<?>>> entities = this.sgGeneral.entityListSetting("Entities", ".", EntityType.PLAYER);
    private final Setting<RenderShape> renderShape = this.sgGeneral.enumSetting("Render Shape", RenderShape.Full, "Which parts of boxes should be rendered.");
    private final Setting<BlackOutColor> fill = this.sgGeneral.colorSetting("Fill Color", new BlackOutColor(255, 255, 255, 80), "Fill Color");
    private final Setting<BlackOutColor> line = this.sgGeneral.colorSetting("Line Color", new BlackOutColor(255, 255, 255, 120), "Line Color");

    public FeetESP() {
        super("FeetESP", "Shows the feet hitbox does not show feet pictures", SubCategory.ENTITIES, true);
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