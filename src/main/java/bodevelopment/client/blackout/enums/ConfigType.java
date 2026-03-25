package bodevelopment.client.blackout.enums;

import bodevelopment.client.blackout.module.AbstractModule;
import bodevelopment.client.blackout.module.ParentCategory;

import java.util.function.Predicate;

public enum ConfigType {
    Combat(module -> module.category.parent() == ParentCategory.COMBAT),
    Movement(module -> module.category.parent() == ParentCategory.MOVEMENT),
    Visual(module -> module.category.parent() == ParentCategory.VISUAL),
    Misc(module -> module.category.parent() == ParentCategory.MISC),
    Legit(module -> module.category.parent() == ParentCategory.LEGIT),
    Client(module -> module.category.parent() == ParentCategory.CLIENT),
    HUD(null),
    Binds(null);

    public final Predicate<AbstractModule> predicate;

    ConfigType(Predicate<AbstractModule> predicate) {
        this.predicate = predicate;
    }
}
