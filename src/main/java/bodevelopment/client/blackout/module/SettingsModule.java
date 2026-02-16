package bodevelopment.client.blackout.module;

public class SettingsModule extends Module {
    public SettingsModule(String name, boolean client, boolean subscribe) {
        super(name, "Global " + name.toLowerCase() + " settings for all BlackOut modules.", client ? SubCategory.CLIENT : SubCategory.SETTINGS, subscribe);
    }

    @Override
    public boolean toggleable() {
        return false;
    }

    @Override
    public void enable(String msg) {
    }

    @Override
    public void disable(String message) {
    }

    @Override
    public boolean shouldSkipListeners() {
        return false;
    }
}
