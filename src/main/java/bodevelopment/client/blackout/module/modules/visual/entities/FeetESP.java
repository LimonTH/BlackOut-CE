/*
 * Blackout Client (CE) - A cutting-edge, feature-rich cheat client for Minecraft.
 * A modernized continuation of the original Blackout project by OLEPOSSU & KassuK.
 * Copyright (C) 2026  LimonTH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

package bodevelopment.client.blackout.module.modules.visual.entities;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.enums.RenderShape;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.RenderEvent;
import bodevelopment.client.blackout.manager.Managers;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.module.setting.settings.ListSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.render.Render3DUtils;
import bodevelopment.client.blackout.util.render.RenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.List;

public class FeetESP extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<List<EntityType<?>>> entities = this.sgGeneral.entityListSetting("Target Filters", "Specifies which entity categories will have their base coordinates highlighted.", EntityType.PLAYER);
    private final Setting<RenderShape> renderShape = this.sgGeneral.enumSetting("Mesh Mode", RenderShape.Full, "Defines which geometric components (faces, lines, or both) are rendered for the foot-level box.");
    private final Setting<BlackOutColor> fill = this.sgGeneral.colorSetting("Interior Color", new BlackOutColor(255, 255, 255, 80), "The color and transparency of the polygon faces at the entity's feet.");
    private final Setting<BlackOutColor> line = this.sgGeneral.colorSetting("Outline Color", new BlackOutColor(255, 255, 255, 120), "The color and transparency of the wireframe edges at the entity's feet.");

    @SuppressWarnings("unchecked")
    public FeetESP() {
        super("Feet ESP", "Renders a discrete bounding box at the base of entities to highlight their exact ground position and collision footprint.", SubCategory.ENTITIES, true);
        ((ListSetting<EntityType<?>>) this.entities).withItemColors(
                () -> this.line.get().getColor(),
                () -> this.fill.get().getColor()
        ).snapshotDefaults();
    }

    @Event
    public void onRender(RenderEvent.World.Post event) {
        if (BlackOut.mc.level == null || BlackOut.mc.player == null) return;

        try (RenderState state = Render3DUtils.begin()) {
            BlackOut.mc.level.entitiesForRendering().forEach(entity -> {
                if (this.entities.get().contains(entity.getType())) {
                    float partial = BlackOut.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
                    Vec3 pos = Managers.POSITION.vec3().get(
                            Mth.lerp(partial, entity.xo, entity.getX()),
                            Mth.lerp(partial, entity.yo, entity.getY()),
                            Mth.lerp(partial, entity.zo, entity.getZ())
                    );

                    AABB entityBox = Managers.POSITION.getBox(entity);
                    double halfWidth = entityBox.getXsize() / 2.0;
                    double halfDepth = entityBox.getZsize() / 2.0;

                    AABB feetBox = Managers.POSITION.aabb().get(
                            pos.x - halfWidth, pos.y, pos.z - halfDepth,
                            pos.x + halfWidth, pos.y + 0.01, pos.z + halfDepth
                    );

                    ListSetting<EntityType<?>> listSetting = (ListSetting<EntityType<?>>) this.entities;
                    Color customLine = listSetting.getItemData(entity.getType(), "lineColor");
                    Color customFill = listSetting.getItemData(entity.getType(), "sideColor");
                    BlackOutColor useLine = customLine != null
                            ? new BlackOutColor(customLine.getRed(), customLine.getGreen(), customLine.getBlue(), customLine.getAlpha())
                            : this.line.get();
                    BlackOutColor useFill = customFill != null
                            ? new BlackOutColor(customFill.getRed(), customFill.getGreen(), customFill.getBlue(), customFill.getAlpha())
                            : this.fill.get();

                    Render3DUtils.box(feetBox, useFill, useLine, renderShape.get());
                }
            });
        }
    }
}