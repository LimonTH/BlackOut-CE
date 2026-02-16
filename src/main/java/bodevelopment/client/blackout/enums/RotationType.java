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
