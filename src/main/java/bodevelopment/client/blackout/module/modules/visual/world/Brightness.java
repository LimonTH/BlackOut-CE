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

package bodevelopment.client.blackout.module.modules.visual.world;

import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.Experimental;
import bodevelopment.client.blackout.event.Event;
import bodevelopment.client.blackout.event.events.TickEvent;
import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;
import bodevelopment.client.blackout.module.setting.Setting;
import bodevelopment.client.blackout.module.setting.SettingGroup;
import bodevelopment.client.blackout.util.PlayerUtils;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

@Experimental
public class Brightness extends Module {
    private static Brightness INSTANCE;

    private final SettingGroup sgGeneral = this.addGroup("General");

    public final Setting<Mode> mode = this.sgGeneral.enumSetting("Illumination Mode", Mode.Gamma, "The technical approach used to enhance visibility.");
    public final Setting<Integer> luminanceLevel = this.sgGeneral.intSetting("Luminance Level", 8, 0, 15, 1, "Brightness level for blocks in Luminance mode.", () -> this.mode.get() == Mode.Luminance);

    public static int luminanceValue = 0;

    public Brightness() {
        super("Brightness", "Provides artificial illumination via Gamma override, Status Effects, or Lightmap Luminance.", SubCategory.WORLD, true);
        INSTANCE = this;
    }

    public static Brightness getInstance() {
        return INSTANCE;
    }

    @Override
    public void onDisable() {
        if (BlackOut.mc.player != null) {
            if (BlackOut.mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
                BlackOut.mc.player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
    }

    @Override
    public String getInfo() {
        return this.mode.get().name();
    }

    @Event
    public void onTick(TickEvent.Pre event) {
        if (!PlayerUtils.isInGame()) return;

        int level = this.luminanceLevel.get();
        luminanceValue = (level << 4) | (level << 20);
        if (this.mode.get() == Mode.Effect) {
            BlackOut.mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, MobEffectInstance.MAX_AMPLIFIER, false, false));
        } else {
            if (BlackOut.mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
                BlackOut.mc.player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
    }

    public enum Mode {
        Effect,
        Gamma,
        Luminance
    }
}