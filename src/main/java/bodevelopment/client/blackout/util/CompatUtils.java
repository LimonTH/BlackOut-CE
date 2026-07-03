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

package bodevelopment.client.blackout.util;

import baritone.api.BaritoneAPI;
import bodevelopment.client.blackout.BlackOut;
import bodevelopment.client.blackout.annotations.Internal;
import bodevelopment.client.blackout.module.modules.movement.ElytraFly;
import bodevelopment.client.blackout.util.render.CapeRenderContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

/**
 * Centralized compatibility checks for third-party mods.
 * Each mod gets its own inner class for clean access patterns.
 * <p>
 * <b>Mod compatibility via Mixins (all in {@code blackout.mixins.json}):</b>
 * <ul>
 *   <li><b>Sodium</b> —
 *   {@code mixins.sodium.MixinSodiumBlockOcclusionCache},
 *   {@code mixins.sodium.MixinSodiumBlockRenderer},
 *   {@code mixins.sodium.MixinSodiumDefaultFluidRenderer},
 *   {@code mixins.sodium.MixinSodiumOcclusionCuller},
 *   {@code mixins.sodium.MixinSodiumVisibilityEncoding}
 *   </li>
 *   <li><b>Fabric API</b> —
 *   {@code mixins.fabricapi.MixinFabricBakedModel}
 *   </li>
 *   <li><b>ModMenu</b> — detected via {@link CompatUtils.ModMenu#isLoaded()},
 *   "Mods" button added in {@code gui.menu.MainMenu}</li>
 *   <li><b>Baritone</b> — detected via {@link CompatUtils.Baritone#isLoaded()},
 *   rotation bypass in {@code mixins.MixinLocalPlayer}, {@code mixins.MixinLivingEntity},
 *   {@code mixins.MixinEntity}, {@code mixins.MixinClientPacketListener},
 *   {@code mixins.MixinKeyboardInput}, {@code manager.managers.RotationManager}</li>
 *   <li><b>FirstPersonModel</b> — detected via {@link CompatUtils.FirstPersonModel#isLoaded()},
 *   HandESP compat in {@code mixins.MixinEntityRenderDispatcher#onRender}</li>
 *   <li><b>WaveyCapes</b> —
 *   UV scaling via {@code mixins.MixinVanillaCapeRenderer} +
 *   {@link CapeVertexConsumer} + {@link CapeRenderContext},
 *   global cape texture intercept via {@code mixins.MixinPlayerSkin}
 *   (works for any cape mod)</li>
 * </ul>
 */
@Internal
public class CompatUtils {
    public static class Baritone {
        private static final boolean PRESENT;

        static {
            boolean present;
            try {
                Class.forName("baritone.api.BaritoneAPI", false, Baritone.class.getClassLoader());
                present = true;
            } catch (ClassNotFoundException e) {
                present = false;
            }
            PRESENT = present;
        }

        public static boolean isLoaded() {
            return PRESENT;
        }

        public static boolean isPathing() {
            return PRESENT && LazyLoader.isPathing();
        }

        public static boolean shouldBypassRotations() {
            return isPathing() && (BlackOut.mc.player == null || !BlackOut.mc.player.isFallFlying()
                    || !ElytraFly.getInstance().enabled);
        }

        private static class LazyLoader {
            private static final baritone.api.IBaritone BARITONE_INSTANCE;

            static {
                baritone.api.IBaritone inst = null;
                try {
                    inst = BaritoneAPI.getProvider().getPrimaryBaritone();
                } catch (Throwable ignored) {
                }
                BARITONE_INSTANCE = inst;
            }

            private static boolean isPathing() {
                if (BARITONE_INSTANCE == null) return false;
                try {
                    return BARITONE_INSTANCE.getPathingBehavior().isPathing()
                            || BARITONE_INSTANCE.getPathingControlManager().mostRecentInControl().isPresent();
                } catch (Throwable ignored) {
                    return false;
                }
            }
        }
    }

    public static class ModMenu {
        private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("modmenu");

        public static boolean isLoaded() {
            return LOADED;
        }

        public static Screen createScreen(Screen parent) {
            if (!LOADED) return null;
            try {
                Class<?> modsScreenClass = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
                return (Screen) modsScreenClass.getConstructor(Screen.class).newInstance(parent);
            } catch (Exception e) {
                BOLogger.error("Failed to open Mod Menu screen", e);
                return null;
            }
        }
    }

    public static class FirstPersonModel {
        private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("firstperson");

        public static boolean isLoaded() {
            return LOADED;
        }
    }
}
