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

package bodevelopment.client.blackout.enums;

import java.util.Objects;

public enum RotationType {
    Interact(null, false),
    InstantInteract(Interact, true),
    BlockPlace(null, false),
    InstantBlockPlace(BlockPlace, true),
    Attacking(null, false),
    InstantAttacking(Attacking, true),
    Mining(null, false),
    InstantMining(Mining, true),
    Use(null, false),
    InstantUse(Use, true),
    Other(null, false),
    InstantOther(Other, true);

    public final RotationType checkType;
    public final boolean instant;

    RotationType(RotationType checkType, boolean instant) {
        this.checkType = Objects.requireNonNullElse(checkType, this);
        this.instant = instant;
    }

    public RotationType asInstant() {
        return switch (this) {
            case Interact -> InstantInteract;
            case BlockPlace -> InstantBlockPlace;
            case Attacking -> InstantAttacking;
            case Mining -> InstantMining;
            case Use -> InstantUse;
            case Other -> InstantOther;
            default -> this;
        };
    }

    public RotationType asNonInstant() {
        return switch (this) {
            case InstantInteract -> Interact;
            case InstantBlockPlace -> BlockPlace;
            case InstantAttacking -> Attacking;
            case InstantMining -> Mining;
            case InstantUse -> Use;
            case InstantOther -> Other;
            default -> this;
        };
    }

    public RotationType withInstant(boolean instant) {
        if (this.instant == instant) {
            return this;
        } else {
            return instant ? this.asInstant() : this.asNonInstant();
        }
    }
}
