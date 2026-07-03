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
 * Marks a method or class whose hot-path execution is designed to avoid
 * heap allocations (object instantiation, boxing, collection resizing, etc.).
 * <p>
 * This is a documentation-only annotation. Code reviewers and addon authors
 * should treat {@code @NoAlloc} as a contract: the annotated code must not
 * allocate new objects on the heap during normal execution. Typical patterns
 * include object pooling, primitive collections (FastUtil), stack-allocated
 * arrays, and pre-computed constants.
 * <p>
 * Violating the zero-allocation contract in hot paths (onTick, onRender)
 * can cause GC pressure and micro-stutter, especially on low-end hardware.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE})
@PublicAPI
public @interface NoAlloc {
}
