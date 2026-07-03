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
import bodevelopment.client.blackout.module.setting.settings.ListSetting;
import bodevelopment.client.blackout.randomstuff.BlackOutColor;
import bodevelopment.client.blackout.util.ScreenUtils;
import bodevelopment.client.blackout.util.render.Render2DUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Tracers extends Module {
    private final SettingGroup sgGeneral = this.addGroup("General");

    private final Setting<List<EntityType<?>>> entityTypes = this.sgGeneral.entityListSetting("Target Filters", "Specifies which entity types will have tracking lines drawn toward them.", EntityType.PLAYER);
    private final Setting<BlackOutColor> line = this.sgGeneral.colorSetting("Default Tracer Color", new BlackOutColor(255, 255, 255, 100), "The color of the tracer lines for standard entities.");
    private final Setting<BlackOutColor> friendLine = this.sgGeneral.colorSetting("Friend Tracer Color", new BlackOutColor(150, 150, 255, 100), "The color of the tracer lines for entities on your friend list.");

    private final PoseStack stack = new PoseStack();
    private final List<Entity> entities = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public Tracers() {
        super("Tracers", "Draws directional lines from the center of the screen to entities to provide spatial awareness of their locations.", SubCategory.ENTITIES, true);
        ((ListSetting<EntityType<?>>) this.entityTypes).withItemColors(
                () -> this.line.get().getColor(),
                null
        ).snapshotDefaults();
    }

    @Event
    public void onTick(TickEvent.Post event) {
        if (BlackOut.mc.level != null && BlackOut.mc.player != null) {
            this.entities.clear();
            BlackOut.mc.level.tickingEntities.forEach(entity -> {
                if (this.shouldRender(entity)) {
                    this.entities.add(entity);
                }
            });
            this.entities.sort(Comparator.comparingDouble(entity -> -BlackOut.mc.gameRenderer.getMainCamera().getPosition().distanceTo(entity.position())));
        }
    }

    @Event
    public void onRender(RenderEvent.Hud.Post event) {
        if (BlackOut.mc.level != null && BlackOut.mc.player != null) {
            ScreenUtils.beginPixelSpace(this.stack);
            this.entities.forEach(entity -> this.renderTracer(event.tickDelta, entity));
            ScreenUtils.endPixelSpace(this.stack);
        }
    }

    public void renderTracer(double tickDelta, Entity entity) {
        double x = Mth.lerp(tickDelta, entity.xo, entity.getX());
        double y = Mth.lerp(tickDelta, entity.yo, entity.getY());
        double z = Mth.lerp(tickDelta, entity.zo, entity.getZ());
        this.stack.pushPose();
        Color color;
        if (entity instanceof Player && Managers.FRIENDS.isFriend((Player) entity)) {
            color = this.friendLine.get().getColor();
        } else {
            Color customColor = ((ListSetting<EntityType<?>>) this.entityTypes).getItemData(entity.getType(), "lineColor");
            color = customColor != null ? customColor : this.line.get().getColor();
        }

        Vec2 f = Render2DUtils.getCoords(x, y + Managers.POSITION.getBox(entity).getYsize() / 2.0, z, false);
        if (f == null) {
            this.stack.popPose();
        } else {
            Render2DUtils.line(
                    this.stack,
                    ScreenUtils.screenWidth() / 2.0F,
                    ScreenUtils.screenHeight() / 2.0F,
                    f.x,
                    f.y,
                    color.getRGB()
            );
            this.stack.popPose();
        }
    }

    public boolean shouldRender(Entity entity) {
        AntiBot antiBot = AntiBot.getInstance();
        if (antiBot.enabled && antiBot.mode.get() == AntiBot.HandlingMode.Ignore && entity instanceof AbstractClientPlayer player && antiBot.getBots().contains(player)) {
            return false;
        } else if (!this.entityTypes.get().contains(entity.getType())) {
            return false;
        } else {
            return entity != BlackOut.mc.player || FreeCam.getInstance().enabled;
        }
    }
}
