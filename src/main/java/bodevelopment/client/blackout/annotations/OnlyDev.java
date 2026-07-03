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

package bodevelopment.client.blackout.annotations;

import bodevelopment.client.blackout.annotations.PublicAPI;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a module or addon component that should only be loaded in
 * {@link bodevelopment.client.blackout.BlackOut.Type#Dev} builds.
 *
 * <h3>Behaviour</h3>
 * <ul>
 *   <li><b>Dev builds:</b> annotated classes are loaded normally.</li>
 *   <li><b>Beta / Release builds:</b> annotated classes are silently skipped
 *       during module scanning (both built-in and addon).</li>
 * </ul>
 *
 * <h3>Scope</h3>
 * <p>Works on any class discovered by package scanning:
 * <ul>
 *   <li>Built-in modules — checked in {@code ModuleManager.addModuleObjects()}</li>
 *   <li>Addon modules — checked in {@code AddonLoader.scan()}</li>
 *   <li>Addon commands / HUD elements / GUI screens — checked in {@code AddonLoader.scan()}</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * @OnlyDev
 * public class DebugModule extends Module {
 *     public DebugModule() {
 *         super("Debug", "Dev-only debug module", SubCategory.MISC, true);
 *     }
 * }
 * }</pre>
 *
 * <p>Addon authors can use this to ship experimental or debug modules
 * that automatically hide themselves when the client is not a dev build.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@PublicAPI
public @interface OnlyDev {
}
