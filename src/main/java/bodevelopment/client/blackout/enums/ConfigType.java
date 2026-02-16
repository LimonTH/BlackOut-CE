package bodevelopment.client.blackout.enums;

import bodevelopment.client.blackout.module.Module;
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

    public final Predicate<Module> predicate;

    ConfigType(Predicate<Module> predicate) {
        this.predicate = predicate;
    }
}
