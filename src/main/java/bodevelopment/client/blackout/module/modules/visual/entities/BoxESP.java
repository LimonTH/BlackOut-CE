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
import bodevelopment.client.blackout.module.setting.multisettings.BoxMultiSetting;
import bodevelopment.client.blackout.util.OLEPOSSUtils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BoxESP extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<List<EntityType<?>>> entityTypes = this.sgGeneral.entityListSetting("Targeted Entities", "Specifies which entity categories will be highlighted by the ESP.", EntityType.PLAYER);
    private final BoxMultiSetting rendering = BoxMultiSetting.of(this.sgGeneral);

    private final List<Entity> entities = new ArrayList<>();

    public BoxESP() {
        super("Box ESP", "Renders three-dimensional axis-aligned bounding boxes around entities to grant visual awareness through terrain and obstructions.", SubCategory.ENTITIES, true);
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
            this.entities.forEach(entity -> this.renderBox(entity, event.tickDelta));
        }
    }

    private void renderBox(Entity entity, double tickDelta) {
        Vec3d pos = OLEPOSSUtils.getLerpedPos(entity, tickDelta);
        this.rendering
                .render(
                        new Box(
                                pos.getX() - entity.getBoundingBox().getLengthX() / 2.0,
                                pos.getY(),
                                pos.getZ() - entity.getBoundingBox().getLengthZ() / 2.0,
                                pos.getX() + entity.getBoundingBox().getLengthX() / 2.0,
                                pos.getY() + entity.getBoundingBox().getLengthY(),
                                pos.getZ() + entity.getBoundingBox().getLengthZ() / 2.0
                        )
                );
    }
}