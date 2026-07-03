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
 * Indicates that a class, method, or field is designed for thread-safe access.
 * <p>
 * This annotation documents the thread-safety contract. It does not enforce
 * thread safety at compile time or runtime. Code marked with this annotation
 * should be safe to call from any thread (main thread, network thread,
 * render thread, async workers) without external synchronization.
 * <p>
 * Addon authors can use this to quickly identify APIs that are safe to use
 * in packet handlers, async tasks, or other off-main-thread contexts.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@PublicAPI
public @interface ThreadSafe {
}
