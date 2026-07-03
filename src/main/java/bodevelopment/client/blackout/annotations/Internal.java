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

import java.lang.annotation.*;

/**
 * Marks a type, method, or field as internal API.
 * <p>
 * Internal APIs are implementation details of BlackOut Client that may change
 * at any time without warning. <b>Addons must not depend on internal APIs</b> —
 * doing so will likely cause breakage on client updates.
 * <p>
 * Use the public API exposed through {@code BlackoutAddon}, {@code Managers},
 * and the stable module/setting/hud abstractions instead.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
@PublicAPI
public @interface Internal {
}
