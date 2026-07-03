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
 * Indicates that the annotated element is a public API entry point intended
 * for use by addons and external consumers.
 * <p>
 * Methods, fields, and types annotated with {@code @PublicAPI} follow
 * semantic versioning: they will not be removed or have their signatures
 * changed in a backward-incompatible way within the same major version
 * of BlackOut Client.
 * <p>
 * This is the opposite of {@link Internal}. Addon authors should prefer
 * depending on {@code @PublicAPI} elements.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
public @interface PublicAPI {
}
