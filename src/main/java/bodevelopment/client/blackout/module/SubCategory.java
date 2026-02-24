package bodevelopment.client.blackout.module;

import java.util.ArrayList;
import java.util.List;

public record SubCategory(String name, ParentCategory parent) implements Category {
    public static List<SubCategory> categories = new ArrayList<>();

    public static SubCategory DEFENSIVE = new SubCategory("Defensive", ParentCategory.COMBAT);
    public static SubCategory OFFENSIVE = new SubCategory("Offensive", ParentCategory.COMBAT);
    public static SubCategory MISC_COMBAT = new SubCategory("Misc", ParentCategory.COMBAT);

    public static SubCategory MOVEMENT = new SubCategory("Movement", ParentCategory.MOVEMENT);

    public static SubCategory ENTITIES = new SubCategory("Entities", ParentCategory.VISUAL);
    public static SubCategory WORLD = new SubCategory("World", ParentCategory.VISUAL);
    public static SubCategory MISC_VISUAL = new SubCategory("Misc", ParentCategory.VISUAL);

    public static SubCategory MISC = new SubCategory("Misc", ParentCategory.MISC);
    public static SubCategory MEMES = new SubCategory("Memes", ParentCategory.MISC);

    public static SubCategory LEGIT = new SubCategory("Legit", ParentCategory.LEGIT);

    public static SubCategory CLIENT = new SubCategory("Client", ParentCategory.CLIENT);
    public static SubCategory SETTINGS = new SubCategory("Settings", ParentCategory.CLIENT);

    public SubCategory {
        categories.add(this);
    }
}
