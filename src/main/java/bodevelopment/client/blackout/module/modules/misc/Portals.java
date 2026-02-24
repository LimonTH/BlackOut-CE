package bodevelopment.client.blackout.module.modules.misc;


import bodevelopment.client.blackout.module.Module;
import bodevelopment.client.blackout.module.SubCategory;

public class Portals extends Module {
    private static Portals INSTANCE;

    public Portals() {
        super("Portals", "Allows interacting with GUIs and prevents the automatic closing of screens while standing inside a portal.", SubCategory.MISC, true);
        INSTANCE = this;
    }

    public static Portals getInstance() {
        return INSTANCE;
    }
}