package bodevelopment.client.blackout.module.modules.visual.entities;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.modules.combat.misc.AntiBot;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.multisettings.BoxMultiSetting;
import bodevelopment.client.blackout.module.setting.settings.ListSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.EntityUtils;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BoxESP extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<List<EntityType<?>>> entityTypes = this.sgGeneral.entityListSetting("Targeted Entities", "Specifies which entity categories will be highlighted by the ESP.", EntityType.PLAYER);
    private final BoxMultiSetting rendering = BoxMultiSetting.of(this.sgGeneral);

    private final List<Entity> entities = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public BoxESP() {
        super("Box ESP", "Renders three-dimensional axis-aligned bounding boxes around entities to grant visual awareness through terrain and obstructions.", SubCategory.ENTITIES, true);
        ((ListSetting<EntityType<?>>) this.entityTypes).withItemColors(
                () -> this.rendering.lineColor.get().getColor(),
                () -> this.rendering.sideColor.get().getColor(),
                () -> this.rendering.shape.get()
        ).snapshotDefaults();
    }

    @Event
    public void onTickPost(TickEvent.Post event) {
        if (BlackOut.mc.level != null && BlackOut.mc.player != null) {
            this.entities.clear();
            BlackOut.mc.level.tickingEntities.forEach(entity -> {
                if (this.shouldRender(entity)) {
                    this.entities.add(entity);
                }
            });
            this.entities.sort(Comparator.comparingDouble(entity -> -BlackOut.mc.player.distanceTo(entity)));
        }
    }

    public boolean shouldRender(Entity entity) {
        AntiBot antiBot = AntiBot.getInstance();
        return (!antiBot.enabled || antiBot.mode.get() != AntiBot.HandlingMode.Ignore || !(entity instanceof AbstractClientPlayer player) || !antiBot.getBots().contains(player)) && entity != BlackOut.mc.player && this.entityTypes.get().contains(entity.getType());
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.level != null && BlackOut.mc.player != null) {
            this.entities.forEach(entity -> this.renderBox(entity, event.tickDelta));
        }
    }

    private void renderBox(Entity entity, double tickDelta) {
        Vec3 pos = EntityUtils.getLerpedPos(entity, tickDelta);
        ListSetting<EntityType<?>> list = (ListSetting<EntityType<?>>) this.entityTypes;
        Color customLine = list.getItemData(entity.getType(), "lineColor");
        Color customSide = list.getItemData(entity.getType(), "sideColor");

        BlackOutColor lineOverride = customLine != null
                ? new BlackOutColor(customLine.getRed(), customLine.getGreen(), customLine.getBlue(), customLine.getAlpha())
                : null;
        BlackOutColor sideOverride = customSide != null
                ? new BlackOutColor(customSide.getRed(), customSide.getGreen(), customSide.getBlue(), customSide.getAlpha())
                : null;

        AABB entityBox = Managers.POSITION.getBox(entity);
        AABB box = Managers.POSITION.aabb().get(
                pos.x() - entityBox.getXsize() / 2.0,
                pos.y(),
                pos.z() - entityBox.getZsize() / 2.0,
                pos.x() + entityBox.getXsize() / 2.0,
                pos.y() + entityBox.getYsize(),
                pos.z() + entityBox.getZsize() / 2.0
        );

        if (lineOverride != null || sideOverride != null) {
            this.rendering.render(box, lineOverride, sideOverride);
        } else {
            this.rendering.render(box);
        }
    }
}