package bodevelopment.client.blackout.module;

import bodevelopment.client.blackout.BlackOut;

/**
 * Non-toggleable module that serves as a global settings panel.
 * Unlike {@link Module}, it has no keybind, no enabled/disabled state,
 * and its event listeners are always active.
 */
public class SettingsModule extends AbstractModule {
    public SettingsModule(String name, boolean client, boolean subscribe) {
        super(name, "Global " + name.toLowerCase() + " settings for all BlackOut modules.", client ? SubCategory.CLIENT : SubCategory.SETTINGS);

        if (subscribe) {
            BlackOut.EVENT_BUS.subscribe(this, this::shouldSkipListeners);
        }
    }

    @Override
    public boolean shouldSkipListeners() {
        return false;
    }
}
